package nars.exe;

import jcog.Service;
import jcog.TODO;
import jcog.Util;
import jcog.exe.valve.AbstractWork;
import jcog.exe.valve.InstrumentedWork;
import jcog.exe.valve.Sharing;
import jcog.exe.valve.TimeSlicing;
import jcog.list.FasterList;
import jcog.math.random.SplitMix64Random;
import nars.NAR;
import nars.task.ITask;
import nars.time.clock.RealTime;
import org.apache.commons.lang3.mutable.MutableLong;
import org.eclipse.collections.api.set.primitive.LongSet;
import org.eclipse.collections.impl.factory.primitive.LongSets;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.LongPredicate;
import java.util.stream.Stream;

import static java.lang.Double.POSITIVE_INFINITY;

/**
 * inline tasks
 * invoked on current thread, on current stack
 * <p>
 * active tasks
 * priority ~ runtime allocated
 * <p>
 * realtime tasks
 * have a specific clock deadline (ie. hash wheel timer). no guarantee but more likely to be executed on time than lazy tasks.
 * <p>
 * lazy tasks
 * have a specified preferred adjustable periodicity but no guarantees
 * maintenance/metrics/resizing of concepts etc
 */
abstract public class MixMultiExec extends AbstractExec {

    /**
     * sharing context - to be integrated with the NAR's Services, this
     * exec registers with it for it to manage compute resources
     */
    final Sharing sharing = new Sharing();
    private final TimeSlicing cpu;

    Revaluator revaluator;
    private long idleTimePerCycle;

    @Deprecated
    final static ThreadLocal<MutableLong> last = ThreadLocal.withInitial(()->new MutableLong(Long.MIN_VALUE));

    public MixMultiExec(int conceptsCapacity, int threads, Executor exe) {
        super(conceptsCapacity);


        cpu = new TimeSlicing<>("CPU", threads, exe) {


            @Override
            protected boolean work() {
                if (super.work()) {
                    //TODO better calculation
                    long now = TIME;
                    MutableLong ll = last.get();
                    long lastSleepCycle = ll.get();
                    if (lastSleepCycle != now) {
                        Util.sleepNS(idleTimePerCycle);
                        ll.set(now);
                    }
                    return true;
                }
                return false;
            }

            @Override
            public TimeSlicing commit() {
                this.forEach((InstrumentedWork s) -> {
                    Object x = s.who;
                    if (x instanceof Causable) {
                        Causable c = (Causable) x;
                        c.can.commit((l, i) -> { /* unused */ });

                        double value = c.value();
                        if (!Double.isFinite(value))
                            value = 0;
                        //value = Math.max(value, 0);

                        double meanTimeNS = Math.max(1, s.iterTimeNS.getMean());
                        if (!Double.isFinite(meanTimeNS))
                            meanTimeNS = POSITIVE_INFINITY;
                        //double valuePerNano = (value / Math.log(meanTimeNS));
                        double valuePerSecond = (value / (1.0E-9 * meanTimeNS));

                        s.need((float) (valuePerSecond));
                    }
                });

                super.commit();


                return this;
            }
        };
        sharing.can(cpu);
    }

    public static Exec get(int capacity, int concurrency) {
        if (concurrency > 3)
            return new WorkerMultiExec(capacity, concurrency);
        else
            return new PoolMultiExec(capacity, Math.max(2, concurrency));
    }


    private transient long TIME = Long.MIN_VALUE + 1;

    @Override
    protected void update(NAR nar) {
        TIME = nar.time();
        double throttle = nar.loop.throttle.floatValue();
        double cycleNS = ((RealTime) nar.time).durSeconds() * 1.0E9;
        cpu.cycleTimeNS.set(Math.round(cycleNS * nar.loop.jiffy.floatValue()));

        //TODO better idle calculation in each thread / worker
        idleTimePerCycle = Math.round(Util.clamp(nar.loop.periodNS() * (1 - throttle), 0, cycleNS));

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
            final List<Thread> activeThreads = new FasterList();
            LongSet activeThreadIds = LongSets.immutable.empty();

            @Override
            public Thread newThread(Runnable runnable) {
                Thread t = new Thread(() -> {
                    Thread tt = Thread.currentThread();

                    try {

                        runnable.run();

                    } finally {
                        long threadID = Thread.currentThread().getId();
                        synchronized (activeThreads) {
                            boolean removed = activeThreads.remove(tt);
                            assert (removed);
                            activeThreadIds = activeThreadIds.reject(x -> x == threadID).toImmutable();
                            rebuild();
                        }

                    }
                });
                long threadID = t.getId();
                synchronized (activeThreads) {
                    activeThreads.add(t);
                    activeThreadIds = LongSets.mutable.ofAll(activeThreadIds).with(threadID).toImmutable();
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
                            TimeUnit.MILLISECONDS,
                            //new DisruptorBlockingQueue(threads),
                            new ArrayBlockingQueue<>(threads),
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
