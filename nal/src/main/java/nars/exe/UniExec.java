package nars.exe;

import jcog.data.list.MetalConcurrentQueue;
import jcog.data.map.ConcurrentFastIteratingHashMap;
import jcog.event.Ons;
import jcog.exe.valve.AbstractWork;
import jcog.exe.valve.InstrumentedWork;
import jcog.exe.valve.Sharing;
import jcog.exe.valve.TimeSlicing;
import jcog.math.FloatRange;
import jcog.service.Service;
import nars.NAR;
import nars.control.DurService;
import org.eclipse.collections.api.tuple.primitive.ObjectBooleanPair;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * single thread executor used for testing
 * TODO expand the focus abilities instead of naively executed all Can's a specific # of times per cycle
 */
public class UniExec extends AbstractExec {


    public final Revaluator revaluator;

    //new Focus.AERevaluator(new SplitMix64Random(1));
    //new Focus.DefaultRevaluator();

    public final ConcurrentFastIteratingHashMap<Causable, InstrumentedCausable> can = new ConcurrentFastIteratingHashMap<>(new InstrumentedCausable[0]);

    protected static final int IN_CAPACITY = 8 * 1024;
    final MetalConcurrentQueue in =
            //new ArrayBlockingQueue(8192);
            new MetalConcurrentQueue(IN_CAPACITY);
    //new DisruptorBlockingQueue(32*1024);

    final Sharing sharing = new Sharing();
    TimeSlicing cpu;
    private Ons ons = null;

    public final FloatRange explorationRate = FloatRange.unit(0.1f);



    public UniExec() {
        this(Revaluator.NullRevaluator.the);
    }

    public UniExec(Revaluator revaluator) {
        this.revaluator = revaluator;
    }

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
            int[] done = new int[]{0};
            try {
                c.next(nar, kontinue);
            } catch (Throwable t) {
                logger.error("{} {}", c, t);
                return 0;
            }
            return done[0];
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
                    double[] valMin = {Double.POSITIVE_INFINITY}, valMax = {Double.NEGATIVE_INFINITY};

                    int n = size();

                    this.forEach((InstrumentedWork s) -> {
                        double v = ((Causable) s.who).value();
                        s.valueNormalized = v;
                        if (v > valMax[0]) valMax[0] = v;
                        if (v < valMin[0]) valMin[0] = v;
                    });

                    double valRange = valMax[0] - valMin[0];


                    if (Math.abs(valRange) > Double.MIN_NORMAL) {

                        final double[] valRateMin = {Double.POSITIVE_INFINITY};
                        final double[] valRateMax = {Double.NEGATIVE_INFINITY};
                        this.forEach((InstrumentedWork s) -> {
//                            Causable x = (Causable) s.who;
                            //if (x instanceof Causable) {

                            s.valueNormalized = (s.valueNormalized - valMin[0]) / valRange;

                            double value = s.valueNormalized;

                            double valuePerSecond;
                            long accumTime = s.accumulatedTime(true);
                            if (!Double.isFinite(value) || accumTime < 1)
                                valuePerSecond = 0;
                            else {
                                //value = Math.max(value, 0);
                                //double valuePerNano = (value / Math.log(meanTimeNS));
                                valuePerSecond = (value / accumTime);
                            }

                            s.valuePerSecond.addValue(valuePerSecond);
                            double valuePerSecondMean = s.valuePerSecond.getMean();
                            if (valuePerSecond > valRateMax[0]) valRateMax[0] = valuePerSecondMean;
                            if (valuePerSecond < valRateMin[0]) valRateMin[0] = valuePerSecondMean;

                        });
                        double valRateRange = valRateMax[0] - valRateMin[0];
                        if (valRateRange > Double.MIN_NORMAL * n) {
                            double valueRateSum[] = { 0 };
                            forEach((InstrumentedWork s) -> {
                                double svsn = s.valuePerSecondNormalized = (s.valuePerSecond.getMean() - valRateMin[0]) / valRateRange;
                                valueRateSum[0] += svsn;
                            });

                            float explorationRate = UniExec.this.explorationRate.floatValue()/n;
                            forEach((InstrumentedWork s) -> {
                                s.pri(
                                        Math.max(explorationRate,
                                                (float) (s.valuePerSecondNormalized/valueRateSum[0])) );
                            });
                            //print();
                            return this;
                        }

                    }

                    /** flat */
                    float flatDemand = n > 1 ? (1f / n) : 1f;
                    forEach((InstrumentedWork s) -> s.pri(flatDemand));



                    return this;
                }
            };
            sharing.can(cpu);

            ons = new Ons();
            ons.add(n.onCycle(this::onCycle));
            ons.add(DurService.on(n, this::onDur));
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


    protected void onDur() {
        revaluator.update(nar);


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

    @Override
    public boolean concurrent() {
        return false;
    }
}
