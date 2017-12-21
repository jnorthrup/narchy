package nars.exe;

import com.conversantmedia.util.concurrent.DisruptorBlockingQueue;
import com.conversantmedia.util.concurrent.MultithreadConcurrentQueue;
import jcog.Util;
import jcog.exe.AffinityExecutor;
import nars.$;
import nars.NAR;
import nars.Task;
import nars.task.ITask;
import nars.task.NativeTask;
import org.eclipse.collections.api.set.primitive.LongSet;
import org.eclipse.collections.impl.factory.primitive.LongSets;
import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.LongPredicate;
import java.util.stream.Stream;

import static nars.time.Tense.ETERNAL;

public class MultiExec extends AbstractExec {


    static final Logger logger = LoggerFactory.getLogger(MultiExec.class);

    protected Executor exe;

    protected int threads;
    final MultithreadConcurrentQueue<ITask> q;

    final List<Thread> activeThreads = $.newArrayList();
    LongSet activeThreadIds = new LongHashSet();
    LongPredicate isActiveThreadId = (x) -> false;
    public Focus focus;


    public MultiExec(int concepts, int threads, int qSize) {
        this(concepts, threads, qSize, true);
    }

    public MultiExec(int concepts, int threads, int qSize, boolean affinity) {
        super(concepts);

        assert (qSize > 0);

        this.q = new DisruptorBlockingQueue<>(qSize);
        this.threads = threads;


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
     * period to sleep while NAR is not looping, but not stopped
     */
    int idlePeriodMS = 50;

    protected void runner() {


        while (running) {

            if (!nar.loop.isRunning()) {
                Util.sleep(idlePeriodMS);
            } else {

                focus.run(() -> {
                    long cycleStart = System.nanoTime();

                    @Deprecated float throttle = nar.loop.throttle.floatValue();

                    float subCycle = 0.5f;
                    double cycleTime = subCycle * nar.loop.periodMS.intValue() / 1000f;

                    if (!nar.loop.isRunning()) {
                        return ETERNAL; //break
                    }

                    if (cycleTime != cycleTime)
                        cycleTime = 0.1f; //10hz alpha

                    if (throttle < 1f) {
                        long sleepTime = Math.round(((1.0 - throttle) * (cycleTime * 1E9)) / 1.0E6f);
                        if (sleepTime > 0)
                            Util.sleep(sleepTime);
                    }

                    long cycleNanosRemain = Math.round(cycleTime * throttle * 1E9);
                    //long minPlay =
                    //0;
                    //cycleNanosRemain/2;


                    int qq = q.size();
                    if (qq > 0) {
//                    int WORK_SHARE =
//                            (int) Math.ceil(((float) qq) / Math.max(1, (threads - 1)));
                        do {

                            ITask i = q.poll();
                            if (i != null)
                                execute(i);
                            else
                                break;

                        } while (true);//--WORK_SHARE > 0);

                        //long postWork = System.nanoTime();
                        //cycleNanosRemain = Math.max(minPlay, cycleNanosRemain - (postWork - cycleStart));
                    }

                    return cycleStart + cycleNanosRemain;
                });
            }

        }
    }

    boolean running = false;

    @Override
    public void start(NAR nar) {
        synchronized (exe) {
            this.focus = new Focus(nar);
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
        add(new NativeTask.RunTask(r));
    }
    @Override
    public void execute(Consumer<NAR> r) {
        add(new NativeTask.NARTask(r));
    }

    @Override
    protected synchronized void clear() {
        super.clear();
        activeThreads.forEach(Thread::interrupt);
        activeThreads.clear();
        activeThreadIds = new LongHashSet();
        isActiveThreadId = (x) -> false;
    }


    final Consumer<ITask> immediate = this::execute;

    final Consumer<ITask> deferred = x -> {
            if (x instanceof Task)
                execute(x);
            else
                queue(x);
        };

    /**
     * the input procedure according to the current thread
     */
    protected Consumer<ITask> add() {
        return isWorker(Thread.currentThread()) ? immediate : deferred;
    }

    @Override
    public void add(Stream<? extends ITask> input) {
        input.forEach(add());
    }

    @Override
    public void add(Iterator<? extends ITask> input) {
        input.forEachRemaining(add());
    }

    @Override
    public void add(ITask t) {
        if ((t instanceof Task) || (isWorker(Thread.currentThread()))) {
            execute(t);
        } else {
            queue(t);
        }
    }

    protected void queue(ITask t) {
        while (!q.offer(t)) {
            if (!running) {
                throw new RuntimeException("work queue exceeded capacity while not running");
            }
            ITask next = q.poll();
            if (next != null)
                execute(next);
        }
    }

//
//    protected int which(ITask t) {
//        return Math.abs(t.hashCode() % sub.length);
//    }

    @Override
    public int concurrency() {
        return threads;
    }


//    private class SharedCan extends Can {
//
//        final AtomicInteger workDone = new AtomicInteger(0);
//        final AtomicInteger workRemain = new AtomicInteger(0);
//        final AtomicDouble time = new AtomicDouble(0);
//
//        final AtomicLong valueCachedAt = new AtomicLong(ETERNAL);
//        float valueCached = 0;
//
//        @Override
//        public void commit() {
//            workRemain.set(iterations());
//        }
//
//        public int share(float prop) {
//            return (int) Math.ceil(workRemain.get() * prop);
//        }
//
//        @Override
//        public float value() {
//            long now = nar.time();
//            if (valueCachedAt.getAndSet(now) != now) {
//
//                //HACK
//                float valueSum = 0;
//                for (Cause c : nar.causes) {
//                    if (c instanceof Conclude.RuleCause) {
//                        valueSum += c.value();
//                    }
//                }
//
//                this.valueCached = valueSum;
//
//                int w = workDone.getAndSet(0);
//                double t = time.getAndSet(0);
//
//                this.update(w, valueCached, t);
//            }
//
//            return valueCached;
//        }
//
//        public void update(int work, double timeSec) {
//            this.workDone.addAndGet(work);
//            this.time.addAndGet(timeSec);
//        }
//
//    }
}
