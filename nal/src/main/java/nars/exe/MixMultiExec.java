package nars.exe;

import com.conversantmedia.util.concurrent.DisruptorBlockingQueue;
import jcog.Service;
import jcog.TODO;
import jcog.exe.valve.AbstractWork;
import jcog.exe.valve.InstrumentedWork;
import jcog.exe.valve.Sharing;
import jcog.exe.valve.TimeSlicing;
import jcog.math.random.SplitMix64Random;
import nars.$;
import nars.NAR;
import nars.task.ITask;
import nars.time.clock.RealTime;
import org.eclipse.collections.api.set.primitive.LongSet;
import org.eclipse.collections.impl.factory.primitive.LongSets;
import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.LongPredicate;
import java.util.stream.Stream;

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

    public static Exec get(int capacity, int concurrency) {
        if (concurrency > 1)
            return new WorkerMultiExec(capacity, concurrency);
        else
            return new PoolMultiExec(capacity, concurrency);
    }


    @Override
    protected void update(NAR nar) {
        cpu.cycleTimeNS.set( Math.round(((RealTime)nar.time).durSeconds() * 1.0E9) );
        super.update(nar);
        revaluator.update(nar);
        sharing.commit();
    }

    public void execute(Runnable async) {
        cpu.queue(async);
    }

    public static class PoolMultiExec extends MixMultiExec {

        public PoolMultiExec(int conceptsCapacity, int threads) {
            super(conceptsCapacity, threads, ForkJoinPool.commonPool());
        }

    }

    public static class WorkerMultiExec extends MixMultiExec {

        static LongPredicate isActiveThreadId = (x) -> false;

        static final ThreadFactory activeThreads = new ThreadFactory() {
            final List<Thread> activeThreads = $.newArrayList();
            LongSet activeThreadIds = new LongHashSet();

            @Override
            public Thread newThread(@NotNull Runnable runnable) {
                Thread t = new Thread(()->{
                    try {
                        runnable.run();
                    } finally {
                        synchronized (activeThreads) {
                            activeThreads.forEach(Thread::interrupt);
                            Thread tt = Thread.currentThread();
                            activeThreads.remove(tt);
                            activeThreadIds = activeThreadIds.reject(x ->x==tt.getId()).toImmutable();
                            rebuild();
                        }

                    }
                });
                synchronized (activeThreads) {
                    activeThreads.add(t);
                    activeThreadIds = LongSets.mutable.ofAll(activeThreadIds).with(t.getId()).toImmutable();
                    rebuild();
                }
                return t;
            }

            void rebuild() {
                long max = activeThreadIds.max();
                long min = activeThreadIds.min();
                if (max - min == activeThreadIds.size() - 1) {
                    isActiveThreadId = (x) -> x >= min && x <= max;
                } else {
                    isActiveThreadId = activeThreadIds::contains;
                }
            }
        };



        public WorkerMultiExec(int conceptsCapacity, int threads) {
            super(conceptsCapacity, threads,

                new ThreadPoolExecutor(threads, threads, 0L,
                        TimeUnit.MILLISECONDS, new DisruptorBlockingQueue(threads),
                        activeThreads)
                //Executors.newFixedThreadPool(threads)
            );
        }


        @Override
        public void execute(Object t) {

            if (t instanceof Runnable)
                if (!isWorker()) {
                    super.execute((Runnable) t);
                    return;
                }

            executeNow(t);
        }

        public void execute(/*@NotNull*/ Iterator<? extends ITask> input) {
            input.forEachRemaining(this::executeNow);
        }

        public void execute(/*@NotNull*/ Stream<? extends ITask> input) {
            input.forEach(this::executeNow);
        }



        private boolean isWorker() {
            return isWorker(Thread.currentThread());
        }

        private boolean isWorker(Thread t) {
            return isActiveThreadId.test(t.getId());
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
            
            n.services().filter(x -> x instanceof Causable).forEach(x -> add((Causable) x));

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
