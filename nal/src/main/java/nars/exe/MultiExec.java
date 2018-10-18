package nars.exe;

import jcog.TODO;
import jcog.Texts;
import jcog.Util;
import jcog.data.list.FasterList;
import jcog.event.Off;
import jcog.exe.AffinityExecutor;
import jcog.exe.Exe;
import jcog.exe.realtime.FixedRateTimedFuture;
import jcog.random.SplitMix64Random;
import nars.NAR;
import nars.control.DurService;
import nars.task.AbstractTask;
import nars.task.ITask;
import nars.task.NALTask;
import nars.task.TaskProxy;
import nars.time.clock.RealTime;

import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

abstract public class MultiExec extends UniExec {


    public static final float inputQueueSizeSafetyThreshold = 0.9f;
    private final Revaluator revaluator;

    protected volatile long idleTimePerCycle;

    /** global sleep nap period */
    private static final long NapTime = 2 * 1000 * 1000; //on the order of ~1ms

    float queueLatencyMeasurementProbability = 0.05f;

    public MultiExec(Revaluator revaluator, int concurrency  /* TODO adjustable dynamically */) {
        super(concurrency, concurrency);
        this.revaluator = revaluator;
    }

    @Override
    public final void execute(Object x) {
        if (x instanceof NALTask || x instanceof TaskProxy)
            executeNow((ITask) x);
        else
            executeLater(x);
    }

    public void executeLater(/*@NotNull */Object x) {

        if (!in.offer(x)) {
            logger.warn("{} blocked queue on: {}", this, x);
            //in.add(x);
            executeNow(x);
        }
    }

    @Override
    public final void execute(Runnable r) {
        if (r instanceof FixedRateTimedFuture && ((((FixedRateTimedFuture) r).run instanceof NARLoop))) {
            nextCycle(r);
        } else {
            executeLater(r);
        }
    }


    /**
     * receives the NARLoop cycle update. should execute immediately, preferably in a worker thread (not synchronously)
     */
    protected abstract void nextCycle(Runnable r);


    @Override
    public final void execute(Consumer<NAR> r) {
        executeLater(r);
    }

    protected void onDur() {
        revaluator.update(nar);

        if (nar.time instanceof RealTime) {
            double cycleNS = nar.loop.periodNS();
            if(cycleNS < 0) {
                //paused
                idleTimePerCycle = NapTime;
                cpu.cycleTimeNS.set(0);
            } else {
                double throttle = nar.loop.throttle.floatValue();

                //TODO better idle calculation in each thread / worker
                idleTimePerCycle = Math.round(Util.clamp(cycleNS * (1 - throttle), 0, cycleNS));

                cpu.cycleTimeNS.set(Math.max(1, Math.round(cycleNS * throttle)));
            }


            if (nar.random().nextFloat() < queueLatencyMeasurementProbability) {
                execute(new QueueLatencyMeasurement(System.nanoTime()));
            }

        } else
            throw new TODO();

        sharing.commit();

    }

    /** measure queue latency "ping" */
    private void queueLatency(long start, long end) {
        long latencyNS = end - start;
        double cycles = latencyNS / ((double)nar.loop.cycleTimeNS);
        if (cycles > 1) {
            logger.info("queue latency {} ({} cycles)", Texts.timeStr(latencyNS), Texts.n4(cycles));
        }
    }

    @Override
    public void start(NAR n) {
        super.start(n);
        ons.add(DurService.on(n, this::onDur));
    }

    protected void onCycle(NAR nar) {

        nar.time.schedule(this::executeLater);


    }

//
//    protected void work(FasterList b, int concurrency) {
//        //in.drainTo(b, (int) Math.ceil(in.size() * (1f / Math.max(1, (concurrency - 1)))));
//
//
//
//        int batchSize = (int) Math.ceil( (((float)in.capacity()) / Math.max(concurrency, (totalConcurrency - 1))));
//        //int batchSize = 8;
//        //int remaining = Math.min(incoming, batchSize);
//
//
//        int drained = in.remove(b, batchSize);
//
//
////            if (drained > 0) {
////
////                exe += drained;
////                //in.clear(b::add, batchSize);
//////            remaining -= batchSize;
////
////                return exe;
////
////            }
//
//
//
//        //System.out.println(Thread.currentThread() + " " + incoming + " " + batchSize + " " + exe);
//
//    }

    protected boolean execute(FasterList b, int concurrency) {
        //TODO sort, distribute etc
        int bn = b.size();
        if (bn == 0)
            return false;

        if (bn > 2) {
            b.sortThisByInt(x -> x.getClass().hashCode()); //sloppy sort by type
        }

        if (concurrency <= 1) {
            b.forEach(this::executeNow);
        } else {

            float granularity = 2;
            int chunkSize = Math.max(1, (int) Math.min(concurrency, b.size() / (concurrency * granularity)));

            (((FasterList<?>) b).chunkView(chunkSize))
                    .parallelStream().forEach(x -> x.forEach(this::executeNow));
        }

        b.clear();
        return true;
    }

//    protected void play() {
//
////        long dutyTimeStart = System.nanoTime();
////        long dutyTimeEnd = System.nanoTime();
//        double timeSliceNS =
//                Math.max(1,
//                    cpu.cycleTimeNS.longValue()// - Math.max(0, (dutyTimeEnd - dutyTimeStart))
//                        * nar.loop.jiffy.doubleValue()
//                );
//
//        can.forEachValue(c -> {
//            if (c.c.instance.availablePermits() == 0)
//                return;
//
//
//            double iterTimeMean = c.iterTimeNS.getMean();
//            double iterationsMean = c.iterations.getMean();
//            int work;
//            if (iterTimeMean == iterTimeMean && iterationsMean==iterationsMean) {
//
//                double growth = 1;
//                double maxIters = growth * Math.max(1, (c.pri() * timeSliceNS / (iterTimeMean / iterationsMean)));
//                work = (maxIters == maxIters) ?
//                        Util.clamp((int)Math.round(maxIters), 1, CAN_ITER_MAX) : 1;
//            } else {
//                work = 1;
//            }
//            //System.out.println(c + " " + work);
//
//            //int workRequested = c.;
//            //b.add((Runnable) (() -> { //new NLink<Runnable>(()->{
//
//            play(c, work);
//
//
//            //}));
//
//            //c.c.run(nar, WORK_PER_CYCLE, x -> b.add(x.get()));
//        });
//    }
//
//    private void play(MyAbstractWork c, int work) {
//
//        tryCycle();
//
//        if (c.start()) {
//            try {
//                c.next(work);
//            } finally {
//                c.stop();
//            }
//        }
//    }

//    public static class UniBufferedExec extends BufferedExec {
//        final Flip<List> buffer = new Flip<>(FasterList::new);
//
//        final AtomicBoolean cycleBusy = new AtomicBoolean();
//
//        @Override
//        protected void onCycle() {
//            super.onCycle();
//
//            if (!cycleBusy.compareAndSet(false, true))
//                return; //busy
//            try {
//                onCycle(buffer.write(), concurrent());
//            } finally {
//                cycleBusy.set(false);
//            }
//        }
//    }


    public static class WorkerExec extends MultiExec {

        public final int threads;
        final boolean affinity;

        final AffinityExecutor exe = new AffinityExecutor();
        private List<Worker> workers;

        public WorkerExec(Revaluator r, int threads) {
            this(r, threads, false);
        }

        public WorkerExec(Revaluator revaluator, int threads, boolean affinity) {
            super(revaluator, threads);
            this.threads = threads;
            this.affinity = affinity;
        }

        final AtomicReference<Runnable> narCycle = new AtomicReference(null);


        /**
         * a lucky worker gets to execute the next NAR cycle when ready
         */
        protected final boolean tryCycle() {
            Runnable r = narCycle.getAndSet(null);
            if (r != null) {
                nar.run();
                return true;
            }
            return false;
        }

        @Override
        protected void nextCycle(Runnable r) {
            //it will be the same instance each time.  so only concerned if it wasnt already cleared by now
            if (this.narCycle.getAndSet(r) != null) {

                //TODO measure
                //if this happens excessively then probably need to reduce the framerate, ie. backpressure

                //logger.warn("lag trying to execute NAR cycle");

            }
        }

        @Override
        public void start(NAR n) {

            int procs = Runtime.getRuntime().availableProcessors();

            synchronized (this) {

                super.start(n);

                workers = exe.execute(Worker::new, threads, affinity);

                if (concurrency() > procs / 2) {
                    /** absorb system-wide tasks rather than using the default ForkJoin commonPool */
                    Exe.setExecutor(this);
                }
            }

        }


        @Override
        public synchronized void stop() {
            Exe.setExecutor(ForkJoinPool.commonPool()); //TODO use the actual executor replaced by the start() call instead of assuming FJP

            workers.forEach(Worker::off);
            workers.clear();

            exe.shutdownNow();

            sync();

            super.stop();
        }


        private final class Worker implements Runnable, Off {

            private final FasterList schedule = new FasterList(inputQueueCapacityPerThread);

            private boolean alive = true;

            final SplitMix64Random rng;

            public Worker() {
                 rng = new SplitMix64Random((31L * System.identityHashCode(this)) + System.nanoTime());
            }

            @Override
            public void run() {

                while (alive) {

                    long workTime = work();

                    long playTime = cpu.cycleTimeNS.longValue() - workTime;
                    if (playTime > 0) {
                        play(playTime);
                    }

                    sleep();
                }
            }


            private long work() {
                long workStart = System.nanoTime();

                do {
                    int available = in.size(); //in.capacity();
                    int batchSize =
                            Util.lerp(nar.loop.throttle.floatValue(),
                                    available, /* all of it if low throttle. this allows other threads to remains asleep while one awake thread takes care of it all */
                                    (int) Math.ceil((((float) available) / Math.max(1, (concurrency()-1))))
                            );

                    int drained = in.remove(schedule, batchSize);
                    if (drained > 0) {
                        execute(schedule, 1);
                    }
                } while (!queueSafe());

                tryCycle();

                long workEnd = System.nanoTime();

                return workEnd - workStart;
            }

            private void play(long playTime) {


                long until = System.nanoTime() + playTime;

                //generate planning



                double granularity = Math.max(1, can.size() - sleeping.cardinality());

                //autojiffy
                double jiffies = Math.max(1, granularity/(concurrency())) //((double)granularity) * Math.max(1, (granularity - 1)) / concurrency();
                        ;
                double jiffyTime = playTime / jiffies;
                double minJiffyTime = jiffyTime;
                double maxJiffyTime = playTime;

                while (queueSafe() && (until > System.nanoTime())) {
                    //int ii = i;
                    InstrumentedCausable c = can.getIndex(rng);
                    if (c == null) break; //empty

                    int id = c.c.scheduledID;

                    if (sleeping.get(id))
                        continue;

                    boolean singleton = c.c.singleton();
                    if (!singleton || c.c.instance.tryAcquire()) {
                        try {

                            long runtimeNS =
                                    Math.round(Util.lerp(c.pri(), minJiffyTime, maxJiffyTime));
                            if (runtimeNS > 0)
                                c.runUntil(System.nanoTime(), runtimeNS);

//                            if (c.c.sleeping(nar))
//                                sleeping.set(id, true);

                        } finally {
                            if (singleton)
                                c.c.instance.release();
                        }
                    }

                    tryCycle();
                    //if (ii == 0)
                    //System.out.println(c + " for " + Texts.timeStr(runtimeNS) + " " + c.iterations.getMean() + " iters");

                    //schedule.add((Runnable) () -> c.runFor(runtimeNS));
                    //});
                }
                //}
            }

            public void sleep() {
                long i = WorkerExec.this.idleTimePerCycle;
                if (i > 0) {

                    Util.sleepNSwhile(i, NapTime, WorkerExec.this::queueSafe);
                }
            }

            @Override
            public synchronized void off() {
                alive = false;

                //execute remaining tasks in callee's thread
                schedule.removeIf(x -> {
                    executeNow(x);
                    return true;
                });
            }
        }


        private boolean queueSafe() {
            return in.availablePct(inputQueueCapacityPerThread) >= inputQueueSizeSafetyThreshold;
        }
    }

    private class QueueLatencyMeasurement extends AbstractTask {

        private final long start;

        public QueueLatencyMeasurement(long start) {
            this.start = start;
        }

        @Override
        public ITask next(NAR n) {
            long end = System.nanoTime();
            queueLatency(start, end);
            return null;
        }
    }

//    public static class ForkJoinExec extends BufferedExec {
//
//        public final Semaphore threads;
//
//        public ForkJoinExec(int threads) {
//            this.threads = new Semaphore(threads);
//
//        }
//
////        @Override
////        public void execute(Runnable r) {
////            ForkJoinPool.commonPool().execute(r);
////        }
//
//
//        final class ForkJoinWorker extends RunnableForkJoin {
//
//
//            final List localBuffer = new FasterList(1024);
//
//            @Override
//            public boolean exec() {
//
//                    try {
//                        onCycle(localBuffer, false);
//                        //if (running) {
//                    } finally {
//                        threads.release();
//                        fork();
//                    }
//                    return false;
//
//
////                    return false;
////                } else {
////                    return true;
////                }
//
//            }
//        }
//
//        @Override
//        public void start(NAR n) {
//
//            synchronized (this) {
//
//                super.start(n);
//
//                int i1 = threads.availablePermits(); //TODO
//                for (int i = 0; i < i1; i++)
//                    spawn();
//
//            }
//
//        }
//
//        private void spawn() {
//            ForkJoinPool.commonPool().execute((ForkJoinTask<?>) new ForkJoinWorker());
//        }
//
//
//        @Override
//        public void stop() {
//            synchronized (this) {
//                //exe.shutdownNow();
//                super.stop();
//            }
//        }
//
//        @Override
//        protected void onCycle() {
//            updateTiming();
//        }
//    }
}
