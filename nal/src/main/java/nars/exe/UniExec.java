package nars.exe;

import jcog.TODO;
import jcog.data.list.MetalConcurrentQueue;
import jcog.data.map.ConcurrentFastIteratingHashMap;
import jcog.event.Ons;
import jcog.exe.valve.AbstractWork;
import jcog.exe.valve.InstrumentedWork;
import jcog.exe.valve.Sharing;
import jcog.exe.valve.TimeSlicing;
import jcog.service.Service;
import nars.NAR;
import nars.control.DurService;
import org.eclipse.collections.api.tuple.primitive.ObjectBooleanPair;

import java.util.function.Consumer;

/**
 * single thread executor used for testing
 * TODO expand the focus abilities instead of naively executed all Can's a specific # of times per cycle
 */
public class UniExec extends AbstractExec {

    int WORK_PER_CYCLE = 1;

    final Focus.DefaultRevaluator revaluator =
            //new Focus.AERevaluator(new SplitMix64Random(1));
            new Focus.DefaultRevaluator();

    public final ConcurrentFastIteratingHashMap<Causable, InstrumentedCausable> can = new ConcurrentFastIteratingHashMap<>(new InstrumentedCausable[0]);

    final MetalConcurrentQueue in =
            //new ArrayBlockingQueue(8192);
            new MetalConcurrentQueue(32*1024);
            //new DisruptorBlockingQueue(32*1024);

    final Sharing sharing = new Sharing();
    TimeSlicing cpu;
    private Ons ons = null;

    public final class InstrumentedCausable extends InstrumentedWork {

        public final Causable c;

        public InstrumentedCausable(Causable c) {
            super(new AbstractWork<>(sharing.start(c), "CPU", 0.5f) {

                @Override
                public boolean next() {
                    return c.next(nar, 1) >= 0;
                }

                @Override
                public final int next(int n) {
                    return c.next(nar, n);
                }
            });
            this.c = c;
        }
//
//        @Override
//        public boolean next() {
//            return super.next();
//        }
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


                @Deprecated @Override
                protected void trySpawn() {

                }

                @Override
                @Deprecated protected boolean work() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public TimeSlicing commit() {
                    double[] valMin = {Double.POSITIVE_INFINITY}, valMax = {Double.NEGATIVE_INFINITY};

                    int n = size();

                    this.forEach((InstrumentedWork s) -> {
                        ((Causable)s.who).can.commit((l, i) -> { /* unused */ });
                        double v = ((Causable)s.who).value();
                        s.valueNormalized = v;
                        if (v > valMax[0]) valMax[0] = v;
                        if (v < valMin[0]) valMin[0] = v;
                    });

                    double valRange = valMax[0] - valMin[0];
                    if (Math.abs(valRange) > Double.MIN_NORMAL) {

                        final double[] valRateMin = {Double.POSITIVE_INFINITY};
                        final double[] valRateMax = {Double.NEGATIVE_INFINITY};
                        this.forEach((InstrumentedWork s) -> {
                            Object x = s.who;
                            if (x instanceof Causable) {

                                s.valueNormalized = (s.valueNormalized - valMin[0]) / valRange;

                                double value = s.valueNormalized;
                                //(1 + Util.tanhFast(c.value()))/2;

                                if (Math.abs(value) > Double.MIN_NORMAL) {
                                    if (!Double.isFinite(value))
                                        s.valuePerSecond = 0;
                                    else {
                                        //value = Math.max(value, 0);
                                        double iters = s.iterations.getMean();
                                        if (iters == iters) {
                                            double meanTimeNS = s.iterTimeNS.getMean() * iters;
                                            if (!Double.isFinite(meanTimeNS))
                                                s.valuePerSecond = 0;
                                            else {
                                                //double valuePerNano = (value / Math.log(meanTimeNS));
                                                s.valuePerSecond = (value / (1.0E-9 * meanTimeNS));
                                            }
                                        } else {
                                            s.valuePerSecond = 0;
                                        }
                                    }
                                } else {
                                    s.valuePerSecond = 0;
                                }
                                double valuePerSecond = s.valuePerSecond;
                                if (valuePerSecond > valRateMax[0]) valRateMax[0] = valuePerSecond;
                                if (valuePerSecond < valRateMin[0]) valRateMin[0] = valuePerSecond;
                            }
                        });
                        double valRateRange = valRateMax[0] - valRateMin[0];
                        if (valRateRange > Double.MIN_NORMAL * can.size()) {
                            forEach((InstrumentedWork s) -> {
                                //s.need((float) s.valueNormalized); //abs

                                double valuePerSecondNormalized = (s.valuePerSecond - valRateMin[0])/valRateRange;
                                if (Double.isFinite(valuePerSecondNormalized)) {
                                    s.pri((float) valuePerSecondNormalized);
                                } else {
                                    //s.pri(0);
                                    throw new TODO();
                                }

                                //System.out.println(s + " " + s.iterations.getMean());

                            });

                            return this;
                        }

                    }

                    /** flat */
                    float flatDemand = n > 1 ?  (1f/n) : 0.5f;
                    forEach((InstrumentedWork s) -> {
                        s.pri(flatDemand);
                    });


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
            if (ons!=null) {
                ons.off();
                ons = null;
            }
            if (sharing!=null) {
                sharing.off(cpu.what, cpu);
            }
            super.stop();
        }
    }


    protected void onDur() {
        revaluator.update(nar);
    }

    protected void onCycle(NAR nar) {
        nar.time.scheduled(this::executeNow);
        sync();

        can.forEachValue(c->
            c.c.next(nar, WORK_PER_CYCLE)
        );
    }

    protected void sync() {
        Object next;
        while ((next = in.poll())!=null) executeNow(next);
    }


    public boolean remove(Causable s) {
        return can.remove(s)!=null;
    }

    public void add(Causable s) {
        InstrumentedCausable r = can.computeIfAbsent(s, InstrumentedCausable::new);
    }

    @Override
    public boolean concurrent() {
        return false;
    }
}
