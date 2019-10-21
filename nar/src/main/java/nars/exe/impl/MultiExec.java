package nars.exe.impl;

import jcog.Texts;
import jcog.data.list.FasterList;
import jcog.event.Off;
import nars.NAR;
import nars.control.Cause;
import nars.control.MetaGoal;
import nars.exe.Exec;
import nars.time.clock.RealTime;
import org.eclipse.collections.api.block.function.primitive.IntFunction;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static java.lang.System.nanoTime;

public abstract class MultiExec extends Exec {

//    /**
//     * global sleep nap period
//     */
//    protected static final long NapTimeNS = 500 * 1000; //on the order of 0.5ms
//
//    static final double lagAdjustmentFactor =
//            0.5;

    ///** timeslice granularity */
    //long subCycleNS = 2_500_000;

    private static final float queueLatencyMeasurementProbability = 0.001f;

    private Off onDur;


    MultiExec(int concurrencyMax  /* TODO adjustable dynamically */) {
        super(concurrencyMax);
    }

    static boolean execute(FasterList b, int concurrency, Consumer each) {
        //TODO sort, distribute etc
        int bn = b.size();
        if (bn == 0)
            return false;

        if (bn > 2) {
            /** sloppy sort by type */
            b.sortThisByInt(new IntFunction() {
                @Override
                public int intValueOf(Object x) {
                    return x.getClass().hashCode();
                }
            });
        }

        if (concurrency <= 1) {
            b.clear(each);
        } else {

            float granularity = 1.0F * ((float) concurrency / 2f);
            int chunkSize = Math.max(1, (int) Math.min((float) concurrency, (float) b.size() / (granularity)));

            (((FasterList<?>) b).chunkView(chunkSize))
                    .parallelStream().forEach(x -> {
                for (Object o : x) {
                    each.accept(o);
                }
            });
        }

        return true;
    }

    @Override
    public void starting(NAR n) {

        if (!(n.time instanceof RealTime))
            throw new UnsupportedOperationException("non-realtime clock not supported");

        super.starting(n);

        onDur = n.onDur(this::update);
    }

    @Override
    protected void stopping(NAR nar) {
        onDur.close(); onDur = null;

        super.stopping(nar);
    }

    @Override
    protected final void next() {
        schedule(this::executeNow);
    }


    /**
     * execute later
     */
    protected abstract void execute(Object r);

    @Override
    public final void execute(Runnable async) {
        execute((Object) async);
    }

    protected void update() {

        updateTiming();

        BiConsumer<NAR, FasterList<Cause>> g = this.governor;
        if (g!=null)
            MetaGoal.value(nar, g);

        if (nar.random().nextFloat() < queueLatencyMeasurementProbability)
            execute(new QueueLatencyMeasurement(nanoTime()));

    }



    private void updateTiming() {

        //long last = this.lastDur;

        //long durDeltaTime = now - last

        //NARLoop loop = nar.loop;
        //cycleIdealNS = loop.periodNS();
        //if (cycleIdealNS <= 0) {
            //paused
            //threadWorkTimePerCycle = 0;
        //} else {
            //double throttle = loop.throttle.floatValue();

            //TODO better idle calculation in each thread / worker
            //this.threadWorkTimePerCycle = (long) (Util.lerp(throttle, 0, cycleIdealNS));

            //float durCycles = nar.dur();
            //long cycleActualNS = Math.round(((double)durDeltaNS)/(UPDATE_DURS * durCycles)); //(long) (1_000_000.0 * CYCLE_DELTA_MS.valueOf((float) (durDeltaNS / 1.0E6)/(UPDATE_DURS * nar.dur())));
//            long lagMeanNS = durDeltaNS - cycleIdealNS; //cycleActualNS - Math.round(UPDATE_DURS * cycleIdealNS);


            //            long threadIdleTimePerCycle = Math.max(0, cycleIdealNS - workTargetNS);

//            if (lagMeanNS > 0) {
//                long lagNS = Math.round(lagMeanNS * lagAdjustmentFactor);
////                if (threadIdleTimePerCycle >= lagNS) {
////                    //use some idle time to compensate for lag overtime
////                    threadIdleTimePerCycle = Math.max(0,threadIdleTimePerCycle - lagNS);
////                }
////                else {
////                    long idleConsumed = threadIdleTimePerCycle;
////                    //need all idle time
////                    threadIdleTimePerCycle = 0;
////                    //decrease work time for remainder
////                    threadWorkTimePerCycle = Math.max(0, threadWorkTimePerCycle - (lagNS - idleConsumed));
////                }
//            }





//            System.out.println(
//                Texts.timeStr(threadWorkTimePerCycle) + " work, " +
//                Texts.timeStr(threadIdleTimePerCycle) + " idle, " +
//                Texts.timeStr(lagMeanNS) + " lag"
//                );
        //}

    }

    private static class QueueLatencyMeasurement implements Consumer<NAR> {

        private final long start;

        QueueLatencyMeasurement(long start) {
            this.start = start;
        }

        /**
         * measure queue latency "ping"
         */
        static void queueLatency(long start, long end, NAR n) {
            long latencyNS = end - start;
            double frames = (double) latencyNS / ((double) (n.loop.periodNS()));
            //if (frames > 0.5) {
                Exec.logger.info("queue latency {} ({} frames)", Texts.timeStr((double) latencyNS), Texts.n4(frames));
            //}
        }

        @Override
        public void accept(NAR n) {
            long end = nanoTime();
            queueLatency(start, end, n);
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
