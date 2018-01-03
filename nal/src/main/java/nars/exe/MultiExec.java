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
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.LongPredicate;
import java.util.stream.Stream;

public class MultiExec extends AbstractExec {

    /**
     * period to sleep while NAR is not looping, but not stopped
     */
    int idleSleepPeriodMS = 50;

    /** duty cycle proportion , fraction of the main NAR cycle */
    final float subCycle;

    static final Logger logger = LoggerFactory.getLogger(MultiExec.class);

    protected Executor exe;

    protected int threads;
    final MultithreadConcurrentQueue q;

    final List<Thread> activeThreads = $.newArrayList();
    LongSet activeThreadIds = new LongHashSet();
    LongPredicate isActiveThreadId = (x) -> false;
    public Focus focus;
    private final AtomicLong lagTime = new AtomicLong(0);

    private final static Function<NAR,Revaluator> Revaluator_Default = (nar)->
        new Focus.AERevaluator(nar.random());
        //new DefaultRevaluator();
        //new RBMRevaluator(nar.random());



    public MultiExec(int concepts, int threads, int qSize) {
        this(concepts, threads, qSize, true);
    }

    public MultiExec(int concepts, int threads, int qSize, boolean affinity) {
        super(concepts);

        assert (qSize > 0);

        this.q = new DisruptorBlockingQueue<>(qSize);
        this.threads = threads;


        this.subCycle = (1f - 1f/(threads+1)) * 0.5f /* base double freq */;

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



            int cycleTimeMS = nar.loop.periodMS.intValue();
            double cycleTime; //in seconds

            if (cycleTimeMS <= 0)
                cycleTime = 0.1f; //10hz alpha
            else {
                cycleTime = Math.max(0,
                        subCycle * cycleTimeMS/1000f - lagTime.getAndSet(0)/(1.0E9*Math.max(1,(threads-1))));
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



            int qq = q.size();
            if (qq > 0) {

                long minPlay =
                    0;
                    //cycleNanosRemain/2;

                int WORK_SHARE =
                        //(int) Math.ceil(((float) qq) / Math.max(1, (threads - 1)));
                        qq;
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


            Focus.Schedule s = focus.read();

            float[] cw = s.weight;
            if (cw.length == 0) {
                continue;
            }

            float[] iterPerSecond = s.iterPerSecond;
            Causable[] can = s.active;

            float jiffy = nar.loop.jiffy.floatValue();

            do {
                try {
                    int x = cw.length > 1 ? Roulette.decideRoulette(cw, rng) : 0;
                    Causable cx = can[x];
                    AtomicBoolean cb = cx.busy;

                    double t = jiffy * cycleNanosRemain/1.0E9;
                    if (t <= 0)
                        continue;

                    int completed;
                    if (cb == null) {
                        completed = run(cx, iterPerSecond[x], t);
                    } else {
                        if (cb.compareAndSet(false, true)) {
                            float weightSaved = cw[x];
                            cw[x] = 0; //hide from being selected by other threads
                            try {
                                completed = run(cx, iterPerSecond[x], t);
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

    private int run(Causable cx, float iterPerSecond, double time) {
        int iters = (int) Math.min(Integer.MAX_VALUE,
                Math.max(1, Math.round(iterPerSecond * time)));
        //System.out.println(cx + " x " + iters);
        return cx.run(nar, iters);
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
            lagTime.getAndAdd(lagEnd-lagStart);
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
