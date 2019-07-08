package nars.exe.impl;

import com.google.common.base.Joiner;
import jcog.Texts;
import jcog.Util;
import jcog.data.list.FasterList;
import jcog.math.FloatAveragedWindow;
import jcog.math.FloatRange;
import nars.NAR;
import nars.Task;
import nars.exe.Exec;
import nars.task.AbstractTask;
import nars.time.clock.RealTime;
import nars.time.part.DurLoop;

import java.io.IOException;
import java.util.function.Consumer;

import static java.lang.System.nanoTime;

abstract public class MultiExec extends Exec {

    /**
     * global sleep nap period
     */
    protected static final long NapTimeNS = 500 * 1000; //on the order of 0.5ms
    static final double lagAdjustmentFactor =
            1.0;
    private static final float UPDATE_DURS =
            1;
    //2; //<- untested
    static private final float queueLatencyMeasurementProbability = 0.01f;
    /**
     * 0..1.0: determines acceptable reaction latency.
     * lower value allows queue to grow larger before it's processed,
     * higher value demands faster response at (a likely) throughput cost
     */
    public final FloatRange alertness = new FloatRange(1f, 0, 1f);
    protected final DurLoop.DurRunnable updater;
    final FloatAveragedWindow CYCLE_DELTA_MS = new FloatAveragedWindow(3, 0.5f);
    //1.5;
    volatile long threadWorkTimePerCycle, threadIdleTimePerCycle;
    volatile long cycleIdealNS;
    volatile long lastCycle = System.nanoTime();


    MultiExec(int concurrencyMax  /* TODO adjustable dynamically */) {
        super(concurrencyMax);

        updater = new DurLoop.DurRunnable(this::update);
        updater.durs(UPDATE_DURS);
        add(updater);
    }

    static boolean execute(FasterList b, int concurrency, Consumer each) {
        //TODO sort, distribute etc
        int bn = b.size();
        if (bn == 0)
            return false;

        if (bn > 2) {
            /** sloppy sort by type */
            b.sortThisByInt(x -> x.getClass().hashCode());
        }

        if (concurrency <= 1) {
            b.clear(each);
        } else {

            float granularity = 1 * (concurrency / 2f);
            int chunkSize = Math.max(1, (int) Math.min(concurrency, b.size() / (granularity)));

            (((FasterList<?>) b).chunkView(chunkSize))
                    .parallelStream().forEach(x -> x.forEach(each));
        }

        return true;
    }

    @Override
    public void starting(NAR n) {

        if (!(n.time instanceof RealTime))
            throw new UnsupportedOperationException("non-realtime clock not supported");

        super.starting(n);
    }

    @Deprecated
    @Override
    public void print(Appendable out) {
        try {
            Joiner.on('\n').appendTo(out, nar.how);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected final void next(NAR nar) {
        nar.exe.schedule(this::execute);
    }

    @Override
    public final void input(Consumer<NAR> r) {
        execute(r);
    }

    /**
     * execute later
     */
    abstract protected void execute(Object r);

    @Override
    public final void execute(Runnable async) {
        execute((Object) async);
    }

    protected void update() {
        long now = System.nanoTime();
        long last = this.lastCycle;
        this.lastCycle = now;
        updateTiming(now - last);
    }

    private void updateTiming(long _cycleDeltaNS) {

        cycleIdealNS = nar.loop.periodNS();
        if (cycleIdealNS < 0) {
            //paused
            threadWorkTimePerCycle = 0;
            threadIdleTimePerCycle = NapTimeNS;
        } else {
            double throttle = nar.loop.throttle.floatValue();

            //TODO better idle calculation in each thread / worker
            long workTargetNS = (long) (Util.lerp(throttle, 0, cycleIdealNS));
            long cycleActualNS = (long) (1_000_000.0 * CYCLE_DELTA_MS.valueOf((float) (_cycleDeltaNS / 1.0E6)));
            long lagMeanNS = cycleActualNS - cycleIdealNS;

            long threadWorkTimePerCycle = workTargetNS;
            long threadIdleTimePerCycle = Math.max(0, cycleIdealNS - workTargetNS);

            long lagNS = Math.round(lagMeanNS * lagAdjustmentFactor);
            if (lagNS > 0) {
                if (threadIdleTimePerCycle >= lagNS) {
                    //use some idle time to compensate for lag overtime
                    threadIdleTimePerCycle -= lagNS;
                } else {
                    long idleConsumed = threadIdleTimePerCycle;
                    //need all idle time
                    threadIdleTimePerCycle = 0;
                    //decrease work time for remainder
                    threadWorkTimePerCycle = Math.max(0, threadWorkTimePerCycle - (lagNS - idleConsumed));
                }
            }


            if (nar.random().nextFloat() < queueLatencyMeasurementProbability)
                accept(new QueueLatencyMeasurement(nanoTime()));

            this.threadWorkTimePerCycle = threadWorkTimePerCycle;
            this.threadIdleTimePerCycle = threadIdleTimePerCycle;
        }

    }

    static private class QueueLatencyMeasurement extends AbstractTask {

        private final long start;

        QueueLatencyMeasurement(long start) {
            this.start = start;
        }

        /**
         * measure queue latency "ping"
         */
        static void queueLatency(long start, long end, NAR n) {
            long latencyNS = end - start;
            double cycles = latencyNS / ((double) (n.loop.periodNS() / n.exe.concurrency()));
            if (cycles > 0.5) {
                Exec.logger.atInfo().log("queue latency {} ({} cycles)", Texts.timeStr(latencyNS), Texts.n4(cycles));
            }
        }

        @Override
        public Task next(Object n) {
            long end = nanoTime();
            queueLatency(start, end, (NAR) n);
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
