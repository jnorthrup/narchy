package nars.exe;

import jcog.Util;
import jcog.data.bit.AtomicMetalBitSet;
import jcog.data.list.MetalConcurrentQueue;
import jcog.data.map.ConcurrentFastIteratingHashMap;
import jcog.event.Offs;
import jcog.exe.valve.AbstractWork;
import jcog.exe.valve.InstrumentedWork;
import jcog.exe.valve.Sharing;
import jcog.exe.valve.TimeSlicing;
import jcog.math.FloatRange;
import jcog.service.Service;
import nars.NAR;
import org.eclipse.collections.api.tuple.primitive.ObjectBooleanPair;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * single thread executor used for testing
 * TODO expand the focus abilities instead of naively executed all Can's a specific # of times per cycle
 */
public class UniExec extends AbstractExec {


    //new Focus.AERevaluator(new SplitMix64Random(1));
    //new Focus.DefaultRevaluator();

    final AtomicMetalBitSet sleeping = new AtomicMetalBitSet();

    public final ConcurrentFastIteratingHashMap<Causable, InstrumentedCausable> can = new ConcurrentFastIteratingHashMap<>(new InstrumentedCausable[0]);

    protected static final int inputQueueCapacityPerThread = 1024;

    final MetalConcurrentQueue in;

    static float timeSliceMomentum = 0.5f;


    final Sharing sharing = new Sharing();
    TimeSlicing cpu;
    protected Offs ons = null;

    public UniExec() {
        this(1, 1);
    }

    protected UniExec(int concurrency, int concurrencyMax) {
        super(concurrency, concurrencyMax);
        in = new MetalConcurrentQueue(inputQueueCapacityPerThread * concurrencyMax());
    }

    public int queueSize() {
        return in.size();
    }
    public int queueCapacity() {
        return in.capacity();
    }

    /**
     * increasing the rate closer to 1 reduces the dynamic range of the temporal allocation
     */
    public final FloatRange explorationRate = FloatRange.unit(0.1f);

    public final class InstrumentedCausable extends InstrumentedWork {

        public final Causable c;

        public InstrumentedCausable(Causable c) {
            super(new MyWork(c));
            this.c = c;
        }


    }

    private final class MyWork extends AbstractWork {

        private final Causable c;

        public MyWork(Causable c) {
            super(sharing.start(c), "CPU", 0.5f);
            this.c = c;
        }

        @Override
        public boolean next() {
            c.next(nar, () -> false);
            return false;
        }

        @Override
        public final int next(int n) {
            int[] done = new int[]{0};
            try {
                c.next(nar, () -> ++done[0] < n);
            } catch (Throwable t) {
                logger.error("{} {}", c, t);
                return 0;
            }
            return done[0];
        }

        @Override
        public int next(BooleanSupplier kontinue) {

            try {
                return c.nextCounted(nar, kontinue);
            } catch (Throwable t) {
                logger.error("{} {}", c, t);
                return 0;
            }

        }
    }

    @Override
    public void start(NAR n) {
        synchronized (this) {
            super.start(n);

            Consumer<ObjectBooleanPair<Service<NAR>>> serviceChange = (xb) -> {
                Service<NAR> s = xb.getOne();
                if (s instanceof Causable) {
                    if (xb.getTwo())
                        UniExec.this.add((Causable) s);
                    else
                        UniExec.this.remove((Causable) s);
                }
            };
            refreshServices();
            n.services.change.on(serviceChange);
            refreshServices(); //to be sure


            cpu = new TimeSlicing<>("CPU", 1, nar.exe) {


                @Deprecated
                @Override
                protected void trySpawn() {

                }

                @Override
                @Deprecated
                protected boolean work() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public synchronized TimeSlicing commit() {

                    int n = size();
                    if (n == 0)
                        return this;

                    double[] valMin = {Double.POSITIVE_INFINITY}, valMax = {Double.NEGATIVE_INFINITY};

                    long now = nar.time();

                    this.forEach((InstrumentedWork s) -> {
                        Causable c = (Causable) s.who;

                        boolean sleeping = c.sleeping(now);
                        UniExec.this.sleeping.set(c.scheduledID, sleeping);
                        if (sleeping) {
                            return;
                        }

                        double v = c.value();
                        if (v == v) {
                            s.valueNext = v;
                            if (v > valMax[0]) valMax[0] = v;
                            if (v < valMin[0]) valMin[0] = v;
                        } else {
                            s.valueNext = Double.NaN;
                        }
                    });

                    double valRange = valMax[0] - valMin[0];


                    if (Double.isFinite(valRange) && Math.abs(valRange) > Double.MIN_NORMAL) {

                        final double[] valRateMin = {Double.POSITIVE_INFINITY}, valRateMax = {Double.NEGATIVE_INFINITY};
                        this.forEach((InstrumentedWork s) -> {
                            Causable c = (Causable) s.who;
                            if (sleeping.get(c.scheduledID))
                                return;

//                            Causable x = (Causable) s.who;
                            //if (x instanceof Causable) {



                            double value = s.valueNext;
                            if (value != value)
                                value = valMin[0];


                            long accumTime = Math.max(1, s.accumulatedTime(true));
                            double valuePerSecond = (value / accumTime);
                            s.valuePerSecond = valuePerSecond;
                            if (valuePerSecond > valRateMax[0]) valRateMax[0] = valuePerSecond;
                            if (valuePerSecond < valRateMin[0]) valRateMin[0] = valuePerSecond;
                        });
                        double valRateRange = valRateMax[0] - valRateMin[0];
                        if (Double.isFinite(valRateRange) && valRateRange > Double.MIN_NORMAL * n) {

                            float explorationRate = UniExec.this.explorationRate.floatValue();

                            double valueRateSum[] = {0};

                            forEach((InstrumentedWork s) -> {
                                Causable c = (Causable) s.who;
                                if (sleeping.get(c.scheduledID)) {
                                    s.pri(0, 1-timeSliceMomentum);
                                    return;
                                }

                                double v = s.valuePerSecond, vv;
                                if (v == v) {
                                    vv = (v - valRateMin[0]) / valRateRange;
                                } else {
                                    vv = 0;
                                }
                                s.valuePerSecondNormalized = Util.lerp(explorationRate, vv, 1);
                                valueRateSum[0] += vv;
                            });

                            if (valueRateSum[0] > Double.MIN_NORMAL) {
                                forEach((InstrumentedWork s) -> {
                                    Causable c = (Causable) s.who;
                                    if (sleeping.get(c.scheduledID))
                                        return;

                                    double v = s.valuePerSecondNormalized;
//                                    if (v == v) {
                                        s.pri(
                                                //((float)v)
                                                (float) (v / valueRateSum[0]),
                                                1-timeSliceMomentum
                                        );
//                                    } else {
//                                        s.pri(explorationRate);
//                                    }
                                });

                                return this;
                            }
                        }

                    }

                    /** flat */
                    n -= sleeping.cardinality();
                    float flatDemand = n > 1 ? (1f / n) : 1f;
                    forEach((InstrumentedWork s) -> {
                        Causable c = (Causable) s.who;
                        if (sleeping.get(c.scheduledID)) {
                            s.pri(0);
                            return;
                        }

                        s.pri(flatDemand);
                    });


                    return this;
                }
            };
            sharing.can(cpu);

            ons = new Offs();
            ons.add(n.onCycle(this::onCycle));

        }
    }

    private void refreshServices() {
        nar.services().filter(x -> x instanceof Causable).forEach(x -> add((Causable) x));
    }


    @Override
    public void stop() {
        synchronized (this) {
            if (ons != null) {
                ons.off();
                ons = null;
            }
            if (sharing != null) {
                sharing.off(cpu.what, cpu);
            }
            super.stop();
        }
    }


    protected void onCycle(NAR nar) {

        sync();
        nar.time.schedule(this::executeNow);

        can.forEachValue(c ->
                c.c.next(nar, () -> false /* 1 work unit */)
        );
    }

    protected void sync() {
        Object next;
        while ((next = in.poll()) != null) executeNow(next);
    }


    public boolean remove(Causable s) {
        return can.remove(s) != null;
    }

    public void add(Causable s) {
        InstrumentedCausable r = can.computeIfAbsent(s, InstrumentedCausable::new);
    }


}
