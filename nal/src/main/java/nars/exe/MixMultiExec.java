package nars.exe;

import jcog.Service;
import jcog.TODO;
import jcog.exe.valve.AbstractWork;
import jcog.exe.valve.InstrumentedWork;
import jcog.exe.valve.Sharing;
import jcog.exe.valve.TimeSlicing;
import jcog.math.random.SplitMix64Random;
import nars.NAR;
import nars.time.clock.RealTime;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
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
abstract public class MixMultiExec extends AbstractExec {

    /** sharing context - to be integrated with the NAR's Services, this
     *  exec registers with it for it to manage compute resources
     */
    final Sharing sharing = new Sharing();
    private final TimeSlicing cpu;

    Revaluator revaluator;

    public MixMultiExec(int conceptsCapacity, int threads, Executor exe) {
        super(conceptsCapacity);

        cpu = new TimeSlicing<>("CPU", threads, exe) {
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
                        //value = Math.max(value, 0);
                        
                        double meanTimeNS = Math.max(1, s.iterTimeNS.getMean());
                        if (!Double.isFinite(meanTimeNS))
                            meanTimeNS = POSITIVE_INFINITY;
                        //double valuePerNano = (value / Math.log(meanTimeNS));
                        double valuePerSecond = (value / (1.0E-9 * meanTimeNS));

                        s.need(  (float) (valuePerSecond));
                    }
                });

                super.commit();

                

                return this;
            }
        };
        sharing.can(cpu);
    }

    @Override
    protected void update(NAR nar) {
        cpu.cycleTimeNS.set( Math.round(((RealTime)nar.time).durSeconds() * 1.0E9) );
        super.update(nar);
        revaluator.update(nar);
        sharing.commit();
    }

    public final void execute(Runnable async) {
        cpu.queue(async);
    }

    public static class PoolMultiExec extends MixMultiExec {

        public PoolMultiExec(int conceptsCapacity, int threads) {
            super(conceptsCapacity, threads, ForkJoinPool.commonPool());
        }

    }
    public static class WorkerMultiExec extends MixMultiExec {

        public WorkerMultiExec(int conceptsCapacity, int threads) {
            super(conceptsCapacity, threads, Executors.newFixedThreadPool(threads));
        }
    }



    @Override
    public void start(NAR n) {
        synchronized (this) {
            super.start(n);

            revaluator =
                    
                    new Focus.AERevaluator(new SplitMix64Random(1));

            
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
                return done >= 0;
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
