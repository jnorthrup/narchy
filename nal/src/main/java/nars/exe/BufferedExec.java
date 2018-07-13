package nars.exe;

import jcog.TODO;
import jcog.Util;
import jcog.event.Off;
import jcog.exe.AffinityExecutor;
import jcog.exe.Exe;
import jcog.exe.realtime.FixedRateTimedFuture;
import jcog.list.FasterList;
import nars.NAR;
import nars.NARLoop;
import nars.task.ITask;
import nars.task.NALTask;
import nars.task.TaskProxy;
import nars.time.clock.RealTime;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

abstract public class BufferedExec extends UniExec {

    private final static int CAN_ITER_MAX = 4096;
    private final int totalConcurrency;

    protected volatile long idleTimePerCycle;

    public BufferedExec(int concurrency) {
        this.totalConcurrency = concurrency;
    }

    @Override
    public void execute(Object x) {
        if (x instanceof NALTask || x instanceof TaskProxy)
            executeNow((ITask) x);
        else
            executeLater(x);
    }

    public void executeLater(@NotNull Object x) {

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

    /** poll for the cycle runnable and runs it in the callee. returns true if it actually executed it. */
    abstract protected boolean tryCycle();

    /**
     * receives the NARLoop cycle update. should execute immediately, preferably in a worker thread (not synchronously)
     */
    protected abstract void nextCycle(Runnable r);


    @Override
    public final void execute(Consumer<NAR> r) {
        executeLater(r);
    }


    @Override
    public boolean concurrent() {
        return true;
    }


    @Override
    protected void onDur() {
        super.onDur();
        sharing.commit();
    }

    protected void onCycle(NAR nar) {

        if (nar.time instanceof RealTime) {
            double throttle = nar.loop.throttle.floatValue();
            double cycleNS = ((RealTime) nar.time).durSeconds() * 1.0E9;

            //TODO better idle calculation in each thread / worker
            idleTimePerCycle = Math.round(Util.clamp(cycleNS * (1 - throttle), 0, cycleNS));

            cpu.cycleTimeNS.set(Math.max(1, Math.round(cycleNS * throttle)));

        } else
            throw new TODO();

        nar.time.scheduled(this::executeLater);

    }


    protected void work(FasterList b, int concurrency) {
        //in.drainTo(b, (int) Math.ceil(in.size() * (1f / Math.max(1, (concurrency - 1)))));
        int incoming = in.size();
        if (incoming == 0)
            return;

        int batchSize = (int) Math.ceil( ((float)incoming / Math.max(concurrency, (totalConcurrency - 1))));
        int remaining = incoming;
        do {

            in.clear(b::add, batchSize);
            remaining -= batchSize;

        } while (execute(b, concurrency) && remaining > 0);


    }

    private boolean execute(FasterList b, int concurrency) {
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

    protected void play() {

        long dutyTimeStart = System.nanoTime();
        long dutyTimeEnd = System.nanoTime();
        long timeSliceNS = cpu.cycleTimeNS.longValue() - Math.max(0, (dutyTimeEnd - dutyTimeStart));
        double finalTimeSliceNS = Math.max(1, timeSliceNS * nar.loop.jiffy.doubleValue());

        can.forEachValue(c -> {
            if (c.c.instance.availablePermits() == 0)
                return;


            double iterTimeMean = c.iterTimeNS.getMean();
            int work;
            if (iterTimeMean == iterTimeMean) {
                double maxIters = (c.pri() * timeSliceNS / (iterTimeMean / Math.max(1, c.iterations.getMean())));
                work = (maxIters == maxIters) ? (int) Math.round(Math.max(1, Math.min(CAN_ITER_MAX, maxIters))) : 1;
            } else {
                work = 1;
            }
//            System.out.println(c + " " + work);

            //int workRequested = c.;
            //b.add((Runnable) (() -> { //new NLink<Runnable>(()->{

            play(c, work);


            //}));

            //c.c.run(nar, WORK_PER_CYCLE, x -> b.add(x.get()));
        });
    }

    private void play(MyAbstractWork c, int work) {

        tryCycle();

        if (c.start()) {
            try {
                c.next(work);
            } finally {
                c.stop();
            }
        }
    }

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


    public static class WorkerExec extends BufferedExec {

        public final int threads;
        final boolean affinity;

        final AffinityExecutor exe = new AffinityExecutor();

        public WorkerExec(int threads) {
            this(threads, false);
        }

        public WorkerExec(int threads, boolean affinity) {
            super(threads);
            this.threads = threads;
            this.affinity = affinity;
        }

        final AtomicReference<Runnable> narCycle = new AtomicReference(null);

        @Override
        protected void onCycle(NAR nar) {
            super.onCycle(nar);
        }

        @Override protected final boolean tryCycle() {
            Runnable r = narCycle.getAcquire();
            if (r != null) {
                //lucky worker gets to execute the NAR cycle
                nar.run();
                return true;
            }
            return false;
        }

        @Override
        protected void nextCycle(Runnable r) {
            //it will be the same instance each time.  so only concerned if it wasnt already cleared by now
            if (this.narCycle.getAndSet(r)!=null) {

                //TODO measure
                //if this happens excessively then probably need to reduce the framerate, ie. backpressure

                //logger.warn("lag trying to execute NAR cycle");

            }
        }

        @Override
        public void start(NAR n) {

            synchronized (this) {

                super.start(n);

                exe.execute(Worker::new, threads, affinity);

                /** absorb system-wide tasks rather than using the default ForkJoin commonPool */
                Exe.setExecutor(this);
            }

        }


        @Override
        public void stop() {
            synchronized (this) {
                Exe.setExecutor(ForkJoinPool.commonPool()); //TODO use the actual executor replaced by the start() call instead of assuming FJP

                exe.shutdownNow();

                in.clear(this::executeNow);

                super.stop();
            }
        }


        private final class Worker implements Runnable, Off {

            private final FasterList schedule = new FasterList(1024);

            private boolean alive = true;

            @Override
            public void run() {
                while (alive) {

                    work(schedule, 1);

                    play();

                    sleep();
                }
            }

            public void sleep() {
                if (idleTimePerCycle > 0) {

                    tryCycle();

                    Util.sleepNSWhile(idleTimePerCycle, 2 * 1000 * 1000 /* 2 ms interval */, () ->
                            in.size() > 0 || !alive
                    );
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
