package nars.exe;

import com.conversantmedia.util.concurrent.DisruptorBlockingQueue;
import jcog.Service;
import jcog.WTF;
import jcog.data.map.ConcurrentFastIteratingHashMap;
import jcog.exe.valve.AbstractWork;
import jcog.exe.valve.InstrumentedWork;
import jcog.exe.valve.Sharing;
import jcog.exe.valve.TimeSlicing;
import jcog.pri.Pri;
import nars.NAR;
import nars.control.DurService;
import org.eclipse.collections.api.tuple.primitive.ObjectBooleanPair;

import java.util.concurrent.BlockingQueue;
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

    final ConcurrentFastIteratingHashMap<Causable,MyAbstractWork> can = new ConcurrentFastIteratingHashMap<>(new MyAbstractWork[0]);

    final BlockingQueue in =
            //new ArrayBlockingQueue(8192);
            new DisruptorBlockingQueue(8192);

    final Sharing sharing = new Sharing();
    TimeSlicing cpu;

    public final class MyAbstractWork extends InstrumentedWork {

        protected final Causable c;

        public MyAbstractWork(Causable c) {
            super(new AbstractWork<>(sharing.start(c), "CPU", 0.5f) {
                @Override
                public boolean start() {
                    if (!c.instance.tryAcquire())
                        return false;
                    return super.start();
                }

                @Override
                public void stop() {
                    c.instance.release();
                    super.stop();
                }

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

        @Override
        public boolean next() {
            return super.next();
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
            n.services.change.on(serviceChange);
            n.services().filter(x -> x instanceof Causable).forEach(x -> add((Causable) x));


            cpu = new TimeSlicing<>("CPU", 1, nar.exe) {


                @Override
                protected void trySpawn() {
                    //dont
                }

                @Override
                @Deprecated protected boolean work() {
                    throw new UnsupportedOperationException();
//                    if (super.work()) {
//                        //TODO better calculation
//                        long now = TIME;
//                        MutableLong ll = last.get();
//                        long lastSleepCycle = ll.get();
//                        if (lastSleepCycle != now) {
//                            Util.sleepNS(idleTimePerCycle);
//                            ll.set(now);
//                        }
//                        return true;
//                    }
//                    return false;
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
                    if (Math.abs(valRange) > Pri.EPSILON) {

                        final double[] valRateMin = {Double.POSITIVE_INFINITY};
                        final double[] valRateMax = {Double.NEGATIVE_INFINITY};
                        this.forEach((InstrumentedWork s) -> {
                            Object x = s.who;
                            if (x instanceof Causable) {

                                s.valueNormalized = (s.valueNormalized - valMin[0]) / valRange;



                                double value =
                                        s.valueNormalized;
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
                        if (valRateRange > Pri.EPSILON * can.size()) {
                            forEach((InstrumentedWork s) -> {
                                //s.need((float) s.valueNormalized); //abs

                                double valuePerSecondNormalized = (s.valuePerSecond - valRateMin[0])/valRateRange;
                                if (Double.isFinite(valuePerSecondNormalized)) {


                                    //s.priSet((float) s.valueNormalized);
                                    s.priSet((float) valuePerSecondNormalized);
                                } else {
                                    s.priSet(0);
                                }

                                //System.out.println(s + " " + s.iterations.getMean());

                            });
                            super.commit();
                            return this;
                        }

                    }

                    /** flat */
                    forEach((InstrumentedWork s) -> {
                        //s.need(1f/n);
                        s.priSet(1f/n);
                    });


                    super.commit();
                    return this;
                }
            };
            sharing.can(cpu);

            n.onCycle(this::onCycle);
            DurService.on(n, this::onDur);
        }
    }


    @Override
    public void stop() {
        synchronized (this) {
            sharing.off(cpu.what, cpu);
            super.stop();
        }
    }


    protected void onDur() {
        revaluator.update(nar);
    }

    protected void onCycle() {
        if (nar==null)
            return; //??

        in.removeIf(e -> {
            executeNow(e);
            return true;
        });
        can.forEachValue(c->
            //c.c.run(nar, WORK_PER_CYCLE, x->x.get().run())
            c.c.next(nar, WORK_PER_CYCLE)
        );
    }



    public boolean remove(Causable s) {
        return can.remove(s)!=null;
    }

    public boolean add(Causable s) {
        MyAbstractWork r = can.put(s, new MyAbstractWork(s));

        if (r!=null && r.c!=s) throw new WTF("duplicate: " + r + " replaced by " + s);

        return true;
    }

    @Override
    public boolean concurrent() {
        return false;
    }
}
