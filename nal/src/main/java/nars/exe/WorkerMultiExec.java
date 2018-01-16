package nars.exe;

import com.conversantmedia.util.concurrent.DisruptorBlockingQueue;
import com.conversantmedia.util.concurrent.MultithreadConcurrentQueue;
import jcog.Util;
import jcog.exe.AffinityExecutor;
import jcog.math.random.XoRoShiRo128PlusRandom;
import nars.$;
import nars.NAR;
import nars.Task;
import nars.task.ITask;
import org.eclipse.collections.api.set.primitive.LongSet;
import org.eclipse.collections.impl.factory.primitive.LongSets;
import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import java.util.function.LongPredicate;
import java.util.stream.Stream;

/**
 * multithreaded executor, via a set of deducated worker threads running in special process loops
 */
public class WorkerMultiExec extends AbstractExec {

    /**
     * period to sleep while NAR is not looping, but not stopped
     */
    int idleSleepPeriodMS = 50;

    /**
     * duty cycle proportion , fraction of the main NAR cycle
     */
    final float subCycle;

    static final Logger logger = LoggerFactory.getLogger(WorkerMultiExec.class);

    protected Executor exe;

    protected int threads;
    final MultithreadConcurrentQueue q;

    final List<Thread> activeThreads = $.newArrayList();
    LongSet activeThreadIds = new LongHashSet();
    LongPredicate isActiveThreadId = (x) -> false;
    public Focus focus;
    private final AtomicLong lagTime = new AtomicLong(0);

    private final static Function<NAR, Revaluator> Revaluator_Default = (nar) ->
            new Focus.AERevaluator(nar.random());
    //new DefaultRevaluator();
    //new RBMRevaluator(nar.random());


    public WorkerMultiExec(int concepts, int threads, int qSize) {
        this(concepts, threads, qSize, true);
    }

    public WorkerMultiExec(int concepts, int threads, int qSize, boolean affinity) {
        super(concepts);

        assert (qSize > 0);

        this.q = new DisruptorBlockingQueue<>(qSize);
        this.threads = threads;


        this.subCycle = (1f - 1f / (threads + 1)) * 0.5f /* base double freq */;

        if (affinity) {
            exe = new AffinityExecutor() {
                @Override
                protected void add(AffinityExecutor.AffinityThread at) {
                    super.add(at);
                    register(at);
                }
            };
        } else {
            exe = Executors.newFixedThreadPool(threads, runnable -> {
                Thread t = new Thread(runnable);
                register(t);
                return t;
            });
        }

    }


    /**
     * stages (3): SLEEP, WORK, PLAY
     */
    protected void runner() {

        Random rng =
                //nar.random();
                new XoRoShiRo128PlusRandom(System.nanoTime());

        final long[] runUntil = new long[1];
        DoubleSupplier next = () -> {

            if (!running)
                return -1;

            if (!nar.loop.isRunning()) {
                Util.sleep(idleSleepPeriodMS);
                return 0;
            }


            int cycleTimeMS = nar.loop.periodMS.intValue();
            double cycleTime; //in seconds

            if (cycleTimeMS <= 0) {
                //HACK
                logger.warn("unknown or infinite cycleTime");
                cycleTime = 0.1f; //10hz alpha
            } else {
                cycleTime = Math.max(0,
                        subCycle * cycleTimeMS / 1000f - lagTime.getAndSet(0) / (1.0E9 * Math.max(1, (threads - 1))));
            }

            long cycleNanosRemain = Math.round(cycleTime * 1E9);

            float throttle = nar.loop.throttle.floatValue();
            if (throttle < 1f) {
                long sleepTime = Math.round(((1.0 - throttle) * (cycleTime * 1E9)) / 1.0E6f);
                if (sleepTime > 0) {
                    Util.sleep(sleepTime - 1);
                    cycleNanosRemain -= sleepTime;
                }
            }


            long cycleStart = System.nanoTime();

            int worked = 0;
            Object i;
            while ((i = q.poll()) != null) {
                executeInline(i);
                worked++;
            }

            if (worked > 0) {
                long postWork = System.nanoTime();
                cycleNanosRemain = cycleNanosRemain - (postWork - cycleStart);
            }

            runUntil[0] = cycleStart + cycleNanosRemain;
            return cycleNanosRemain / 1.0E9 * nar.loop.jiffy.floatValue();
        };

        focus.runDeadline(
                next,
                () -> (System.nanoTime() <= runUntil[0]),
                rng, nar);

    }


    boolean running;

    @Override
    public void start(NAR nar) {
        synchronized (exe) {

            Revaluator revaluator =
                    Revaluator_Default.apply(nar);


            this.focus = new Focus(nar, revaluator);
            super.start(nar);
            for (int i = 0; i < threads; i++)
                exe.execute(this::runner);
            running = true;
        }
    }

    @Override
    public void stop() {
        synchronized (exe) {
            if (exe instanceof AffinityExecutor)
                ((AffinityExecutor) exe).stop();
            else
                ((ExecutorService) exe).shutdownNow();

            super.stop();
            running = false;
        }
    }

    protected boolean isWorker(Thread t) {
        return isActiveThreadId.test(t.getId());
    }


    /**
     * to be called in initWorkers() impl for each thread constructed
     */
    protected synchronized void register(Thread t) {
        activeThreads.add(t);
        activeThreadIds = LongSets.mutable.ofAll(activeThreadIds).with(t.getId()).toImmutable();
        long max = activeThreadIds.max();
        long min = activeThreadIds.min();
        if (max - min == activeThreadIds.size() - 1) {
            //contiguous id's, use fast id tester
            isActiveThreadId = (x) -> x >= min && x <= max;
        } else {
            isActiveThreadId = activeThreadIds::contains;
        }
    }

//    public static class CoolNQuiet extends MultiExec {
//
//        private ThreadPoolExecutor texe;
//
//        public CoolNQuiet(int concepts, int threads, int qSize) {
//            super(concepts, threads, qSize);
//        }
//
//        @Override
//        protected WaitStrategy waitStrategy() {
//            //return new SleepingWaitStrategy();
//            return new LiteBlockingWaitStrategy();
//        }
//
//        @Override
//        protected Executor initExe() {
//            //return Executors.newCachedThreadPool();
//            //return texe = ((ThreadPoolExecutor) Executors.newFixedThreadPool(threads));
//
//
//
//        }
//
//        @Override
//        protected void initWorkers() {
//            WorkHandler<ITask[]>[] w = new WorkHandler[threads];
//            for (int i = 0; i < threads; i++)
//                w[i] = wh;
//            disruptor.handleEventsWithWorkerPool(w);
//        }
//    }


    @Override
    public boolean concurrent() {
        return true;
    }

    @Override
    public float load() {
        return ((float) q.size()) / q.capacity();
    }

    @Override
    public void execute(Runnable r) {
        execute((Object) r);
    }

    @Override
    public void execute(Consumer<NAR> r) {
        execute((Object) r);
    }

    @Override
    protected synchronized void clear() {
        super.clear();
        activeThreads.forEach(Thread::interrupt);
        activeThreads.clear();
        activeThreadIds = new LongHashSet();
        isActiveThreadId = (x) -> false;
    }


    final Consumer immediate = this::executeInline;

    final Consumer deferred = x -> {
        if (x instanceof Task)
            execute(x);
        else
            queue(x);
    };

    /**
     * the input procedure according to the current thread
     */
    protected Consumer add() {
        return isWorker(Thread.currentThread()) ? immediate : deferred;
    }

    @Override
    public void execute(Stream<? extends ITask> input) {
        input.forEach(add());
    }

    @Override
    public void execute(Iterator<? extends ITask> input) {
        input.forEachRemaining(add());
    }

    @Override
    public void execute(Object t) {

        if (t instanceof Task || isWorker(Thread.currentThread())) {
            executeInline(t);
        } else {
            queue(t);
        }

    }


//    @Override
//    public void cycle() {
//        super.cycle();
//
////        long lagTime = this.lagTime.get();
////        if (lagTime > 0) {
////            System.err.println("lag: " + Texts.timeStr(lagTime));
////        }
//    }


    protected void queue(Object t) {
        long lagStart = Long.MAX_VALUE;
        while (!q.offer(t)) {
            if (lagStart == Long.MAX_VALUE)
                lagStart = System.nanoTime();

            if (!running) {
                throw new RuntimeException("work queue exceeded capacity while not running");
            }
            Object next = q.poll();
            if (next != null)
                executeInline(next);
        }

        if (lagStart != Long.MAX_VALUE) {
            long lagEnd = System.nanoTime();
            lagTime.getAndAdd(lagEnd - lagStart);
        }
    }

}
