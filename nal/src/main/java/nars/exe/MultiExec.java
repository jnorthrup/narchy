package nars.exe;

import com.conversantmedia.util.concurrent.DisruptorBlockingQueue;
import com.conversantmedia.util.concurrent.MultithreadConcurrentQueue;
import jcog.Util;
import jcog.decide.Roulette;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.LongPredicate;
import java.util.stream.Stream;

public class MultiExec extends AbstractExec {

    /**
     * period to sleep while NAR is not looping, but not stopped
     */
    int idleSleepPeriodMS = 50;

    /** duty cycle proportion , fraction of the main NAR cycle */
    float subCycle = 0.75f;

    static final Logger logger = LoggerFactory.getLogger(MultiExec.class);

    protected Executor exe;

    protected int threads;
    final MultithreadConcurrentQueue q;

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




    /** stages (3): SLEEP, WORK, PLAY */
    protected void runner() {


        Random rng =
                //nar.random();
                new XoRoShiRo128PlusRandom(System.nanoTime());

        while (running) {

            if (!nar.loop.isRunning()) {
                Util.sleep(idleSleepPeriodMS);
                continue;
            }

            long cycleStart = System.nanoTime();



            double cycleTime = subCycle * nar.loop.periodMS.intValue() / 1000f;

            if (!nar.loop.isRunning()) {
                continue;
            }

            if (cycleTime != cycleTime)
                cycleTime = 0.1f; //10hz alpha

            long cycleNanosRemain = Math.round(cycleTime * 1E9);

            float throttle = nar.loop.throttle.floatValue();
            if (throttle < 1f) {
                long sleepTime = Math.round(((1.0 - throttle) * (cycleTime * 1E9)) / 1.0E6f);
                if (sleepTime > 0) {
                    Util.sleep(sleepTime - 1);
                    cycleNanosRemain -= sleepTime;
                }
            }



            int qq = q.size();
            if (qq > 0) {

                long minPlay =
                    0;
                    //cycleNanosRemain/2;

                int WORK_SHARE =
                        (int) Math.ceil(((float) qq) / Math.max(1, (threads - 1)));
                do {

                    Object i = q.poll();
                    if (i != null)
                        executeInline(i);
                    else
                        break;

                } while (--WORK_SHARE > 0);

                long postWork = System.nanoTime();
                cycleNanosRemain = Math.max(minPlay, cycleNanosRemain - (postWork - cycleStart));
            }

            long runUntil = cycleStart + cycleNanosRemain;
            /** jiffy temporal granularity time constant */
            float jiffy = (float) (nar.loop.jiffy.floatValue() * cycleTime);

            Focus.Schedule s = focus.schedule.read();

            float[] cw = s.weight;
            if (cw.length == 0) {
                continue;
            }

            float[] iterPerSecond = s.iterPerSecond;
            Causable[] can = s.active;

            do {
                try {
                    int x = Roulette.decideRoulette(cw, rng);
                    Causable cx = can[x];
                    AtomicBoolean cb = cx.busy;

                    int completed;
                    if (cb == null) {
                        completed = run(cx, iterPerSecond[x], jiffy);
                    } else {
                        if (cb.compareAndSet(false, true)) {
                            float weightSaved = cw[x];
                            cw[x] = 0; //hide from being selected by other threads
                            try {
                                completed = run(cx, iterPerSecond[x], jiffy);
                            } finally {
                                cb.set(false);
                                cw[x] = weightSaved;
                            }
                        } else {
                            continue;
                        }
                    }

                    if (completed < 0) {
                        cw[x] = 0;
                    }
                } catch (Throwable t) {
                    t.printStackTrace();
                }

            } while (System.nanoTime() <= runUntil);

        }
    }

    private int run(Causable cx, float iterPerSecond, float time) {
        int iters = Math.max(1, Math.round(iterPerSecond * time));
        //System.out.println(cx + " x " + iters);
        return cx.run(nar, iters);
    }

    boolean running;

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
        if ((t instanceof Task)) {
            execute((ITask) t);
        } else {
            if (isWorker(Thread.currentThread())) {
                executeInline(t);
            } else {
                queue(t);
            }
        }
    }

    void executeInline(Object t) {
        try {
            if (t instanceof Runnable) {
                ((Runnable) t).run();
            } else {
                super.execute(t);
            }
        } catch (Throwable e) {
            logger.error("{} {}", t, e);
        }
    }

    protected void queue(Object t) {
        while (!q.offer(t)) {
            if (!running) {
                throw new RuntimeException("work queue exceeded capacity while not running");
            }
            Object next = q.poll();
            if (next != null)
                super.execute(next);
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
