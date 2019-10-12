package nars.exe.impl;

import jcog.Texts;
import jcog.Util;
import jcog.data.list.FasterList;
import jcog.data.list.MetalConcurrentQueue;
import jcog.data.set.LongObjectArraySet;
import jcog.event.Off;
import jcog.math.FloatRange;
import nars.NAR;
import nars.attention.AntistaticBag;
import nars.attention.What;
import nars.derive.DeriverExecutor;
import nars.exe.Exec;
import nars.exe.NARLoop;
import nars.time.clock.RealTime;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.function.Consumer;

import static java.lang.System.nanoTime;

abstract public class MultiExec extends Exec {

    /**
     * global sleep nap period
     */
    protected static final long NapTimeNS = 500 * 1000; //on the order of 0.5ms

    static final double lagAdjustmentFactor =
            0.5;

    //2; //<- untested
    static private final float queueLatencyMeasurementProbability = 0.002f;
    /**
     * 0..1.0: determines acceptable reaction latency.
     * lower value allows queue to grow larger before it's processed,
     * higher value demands faster response at (a likely) throughput cost
     */
    public final FloatRange alertness = new FloatRange(1f, 0, 1f);
//    protected NARPart updater;
//    final FloatAveragedWindow CYCLE_DELTA_MS = new FloatAveragedWindow(3, 0.5f);
    //1.5;
    volatile long threadWorkTimePerCycle, threadIdleTimePerCycle;
    volatile long cycleIdealNS;
    volatile long lastDur /* TODO lastNow */ = System.nanoTime();
    private Off cycle;

    /** TODO double buffer access to this so it may be updated while read */
    protected final MetalConcurrentQueue<Schedule> schedule = new MetalConcurrentQueue<>(2);
    {
        for (int i = 0; i < 2; i++) schedule.add(new Schedule());
    }



    MultiExec(int concurrencyMax  /* TODO adjustable dynamically */) {
        super(concurrencyMax);

        //updater = new DurLoop.DurRunnable(this::update);
        //updater.durs(UPDATE_DURS);
//        add(updater);
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

        cycle = n.onCycle(this::update);
    }

    @Override
    protected void stopping(NAR nar) {
        cycle.close(); cycle = null;

        super.stopping(nar);
    }

    @Override
    protected final void next() {
        schedule(this::executeNow);
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
        long last = this.lastDur;
        this.lastDur = now;
        updateTiming(now - last);

        long subCycleNS = 2_000_000;
        Schedule schedWrite = schedule.peek(1); //access next schedule
        float workPct = workDemand();
        schedWrite.update(nar.what, ((RealTime)nar.time).durNS(), subCycleNS, workPct /* TODO adapt */, 1-nar.loop.throttle.floatValue());
        //System.out.println(schedWrite.size() + " sched entries");
        //schedWrite.print(System.out); System.out.println();
        @Nullable Schedule popped = schedule.poll(); //schedWrite now live in 0th position for readers
        schedule.offer(popped); //push to end of queue
    }

    /** estimate proportion of duty cycle that needs applied to queued work tasks.
     *  calculated by some adaptive method wrt profiled measurements */
    abstract protected float workDemand();

    /** special consumer implementing throttle instruction */
    public static final Consumer SLEEP = (e) -> {};

    /** special consumer implementing work instruction */
    public static final Consumer WORK = (e) -> {};

    static class Schedule extends LongObjectArraySet<Consumer<DeriverExecutor> /* switcher*/> {

        public void update(AntistaticBag<What> w, double durNS, long subCycleNS, double workPct, double sleepPct) {
            clear();

            if (workPct + sleepPct > 1) {
                //only work and sleep but calculate the correct proportion:
                workPct = (workPct / (workPct+sleepPct)); //renormalize
                sleepPct = 1-workPct;
            }

            double workNS = (durNS * workPct);
            double sleepNS = (durNS * sleepPct);
            int nw = (int)Math.max(1, workNS / subCycleNS);
            for (int i = 0; i < nw; i++)
                addDirect(subCycleNS, WORK);

            int ns = (int) Math.floor(sleepNS / subCycleNS);
            for (int i = 0; i < ns; i++)
                addDirect(subCycleNS, SLEEP);

            double playPct = Math.max(0, 1f - (workPct + sleepPct));
            if (playPct>0) {
                double playNS = durNS * playPct;
                double mass = w.mass();
                for (What ww : w) {
                    double priNorm = ww.priElseZero() / mass;
                    double wPlayNS = (priNorm * playNS);
                    int np = (int) Math.max(1, wPlayNS / subCycleNS);
                    Consumer<DeriverExecutor> wws = ww.switcher;
                    for (int i = 0; i < np; i++)
                        addDirect(subCycleNS, wws);
                }
            }
        }

        public void print(PrintStream out) {
            forEachEvent((when,what)->out.println(Texts.timeStr(when) + "\t" + what));
        }
    }



    private void updateTiming(long durDeltaNS) {

        NARLoop loop = nar.loop;
        cycleIdealNS = loop.periodNS();
        if (cycleIdealNS < 0) {
            //paused
            threadWorkTimePerCycle = 0;
            threadIdleTimePerCycle = NapTimeNS;
        } else {
            double throttle = loop.throttle.floatValue();

            //TODO better idle calculation in each thread / worker
            long workTargetNS = (long) (Util.lerp(throttle, 0, cycleIdealNS));
            //float durCycles = nar.dur();
            //long cycleActualNS = Math.round(((double)durDeltaNS)/(UPDATE_DURS * durCycles)); //(long) (1_000_000.0 * CYCLE_DELTA_MS.valueOf((float) (durDeltaNS / 1.0E6)/(UPDATE_DURS * nar.dur())));
            long lagMeanNS = durDeltaNS - cycleIdealNS; //cycleActualNS - Math.round(UPDATE_DURS * cycleIdealNS);


            long threadWorkTimePerCycle = workTargetNS;
            long threadIdleTimePerCycle = Math.max(0, cycleIdealNS - workTargetNS);

            if (lagMeanNS > 0) {
                long lagNS = Math.round(lagMeanNS * lagAdjustmentFactor);
                if (threadIdleTimePerCycle >= lagNS) {
                    //use some idle time to compensate for lag overtime
                    threadIdleTimePerCycle = Math.max(0,threadIdleTimePerCycle - lagNS);
                }
//                else {
//                    long idleConsumed = threadIdleTimePerCycle;
//                    //need all idle time
//                    threadIdleTimePerCycle = 0;
//                    //decrease work time for remainder
//                    threadWorkTimePerCycle = Math.max(0, threadWorkTimePerCycle - (lagNS - idleConsumed));
//                }
            }


            if (nar.random().nextFloat() < queueLatencyMeasurementProbability)
                execute(new QueueLatencyMeasurement(nanoTime()));

            this.threadWorkTimePerCycle = threadWorkTimePerCycle;
            this.threadIdleTimePerCycle = threadIdleTimePerCycle;

//            System.out.println(
//                Texts.timeStr(threadWorkTimePerCycle) + " work, " +
//                Texts.timeStr(threadIdleTimePerCycle) + " idle, " +
//                Texts.timeStr(lagMeanNS) + " lag"
//                );
        }

    }

    static private class QueueLatencyMeasurement implements Consumer<NAR> {

        private final long start;

        QueueLatencyMeasurement(long start) {
            this.start = start;
        }

        /**
         * measure queue latency "ping"
         */
        static void queueLatency(long start, long end, NAR n) {
            long latencyNS = end - start;
            double frames = latencyNS / ((double) (n.loop.periodNS()));
            //if (frames > 0.5) {
                Exec.logger.info("queue latency {} ({} frames)", Texts.timeStr(latencyNS), Texts.n4(frames));
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
