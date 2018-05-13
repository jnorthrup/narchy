package nars.exe;

import jcog.Service;
import jcog.TODO;
import jcog.Util;
import jcog.exe.valve.AbstractWork;
import jcog.exe.valve.InstrumentedWork;
import jcog.exe.valve.Sharing;
import jcog.exe.valve.TimeSlicing;
import jcog.math.random.SplitMix64Random;
import nars.NAR;
import nars.time.clock.RealTime;

import java.util.concurrent.ForkJoinPool;

import static java.lang.Double.POSITIVE_INFINITY;

/**
 *     inline tasks
 *         invoked on current thread, on current stack
 *
 *     active tasks
 *         priority ~ runtime allocated
 *
 *     realtime tasks
 *         have a specific clock deadline (ie. hash wheel timer). no guarantee but more likely to be executed on time than lazy tasks.
 *
 *     lazy tasks
 *         have a specified preferred adjustable periodicity but no guarantees
 *             maintenance/metrics/resizing of concepts etc
 */
public class MixMultiExec extends AbstractExec {

    /** sharing context - to be integrated with the NAR's Services, this
     *  exec registers with it for it to manage compute resources
     */
    final Sharing sharing = new Sharing();
    private final TimeSlicing cpu;

//    private final InstrumentedWork async;
//    private final MultithreadConcurrentQueue asyncQueue = new MultithreadConcurrentQueue(2048);


    Revaluator revaluator;

    public MixMultiExec(int conceptsCapacity) {
        super(conceptsCapacity);

        cpu = new TimeSlicing<>("CPU", Util.concurrencyDefault(2)) {
            @Override
            public TimeSlicing commit() {
                this.forEach((InstrumentedWork s) -> {
                    Object x = s.who;
                    if (x instanceof Causable) {
                        Causable c = (Causable) x;
                        c.can.commit((l,i)->{ /* unused */ });

                        double value = c.value();
                        if (!Double.isFinite(value))
                            value = 0;
                        value = Math.max(value, 0);
                        //(Util.tanhFast(((Causable) x).value()) + 1f)/2f;
                        double meanTimeNS = Math.max(1, s.iterTimeNS.getMean());
                        if (!Double.isFinite(meanTimeNS))
                            meanTimeNS = POSITIVE_INFINITY;
                        double valuePerNano = (value / Math.log(meanTimeNS));

                        s.need( 0.05f + (float) (valuePerNano));
                    }
                });

                super.commit();

                System.out.println(toString());

                return this;
            }
        };
        sharing.can(cpu);


//        async = new InstrumentedWork(new AbstractWork(sharing.start("Async_Exec"), "CPU", 0.5f) {
//
//            @Override
//            public boolean next() {
//                Object next = asyncQueue.poll();
//                if (next == null)
//                    return false;
//
//                execute(next);
//
//                return true;
//            }
//        });

    }

    @Override
    protected void update() {
        cpu.cycleTimeNS.set( Math.round(((RealTime)nar.time).durSeconds() * 1.0E9) );
        super.update();
        revaluator.update(nar);
        sharing.commit();
    }

    @Override
    public void execute(Runnable async) {
        ForkJoinPool.commonPool().execute(async);
//        if (!asyncQueue.offer(async)) {
//            throw new TODO("queue overflow");
//        }
    }


    @Override
    public void start(NAR n) {
        synchronized (this) {
            super.start(n);

            revaluator =
                    //new Focus.DefaultRevaluator();
                    new Focus.AERevaluator(new SplitMix64Random(1));

            //TODO move this into a method that accepts a 2 method interface
            n.services.change.on((xa) -> {
                Service<NAR> x = xa.getOne();
                if (x instanceof Causable) {
                    Causable c = (Causable) x;
                    if (xa.getTwo())
                        add(c);
                    else
                        remove(c);
                }
            });
            //add existing
            n.services().filter(x -> x instanceof Causable).forEach(x -> {
                add((Causable) x);
            });

        }
    }

    protected void add(Causable c) {
        new InstrumentedWork<>(new AbstractWork<>(sharing.start(c), "CPU", 0.25f) {

            @Override
            public boolean next() {
                int done = c.next(nar, 1);
                if (done < 0)
                    return false;
                else
                    return true;
            }
        });
    }
    protected void remove(Causable c) {
        throw new TODO();
    }

    @Override
    public boolean concurrent() {
        return true;
    }

}
