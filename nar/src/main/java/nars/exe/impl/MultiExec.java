package nars.exe.impl;

import jcog.Texts;
import jcog.Util;
import jcog.data.atomic.MetalAtomicReferenceArray;
import jcog.data.list.FasterList;
import jcog.event.Off;
import nars.NAR;
import nars.attention.AntistaticBag;
import nars.attention.What;
import nars.exe.Exec;
import nars.exe.NARLoop;
import nars.time.clock.RealTime;

import java.io.PrintStream;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static java.lang.System.nanoTime;

abstract public class MultiExec extends Exec {

//    /**
//     * global sleep nap period
//     */
//    protected static final long NapTimeNS = 500 * 1000; //on the order of 0.5ms
//
//    static final double lagAdjustmentFactor =
//            0.5;

    /** timeslice granularity */
    long subCycleNS = 1_000_000;


    static private final float queueLatencyMeasurementProbability = 0.001f;
    /**
     * 0..1.0: determines acceptable reaction latency.
     * lower value allows queue to grow larger before it's processed,
     * higher value demands faster response at (a likely) throughput cost
     */
    //public final FloatRange alertness = new FloatRange(1f, 0, 1f);
//    protected NARPart updater;
//    final FloatAveragedWindow CYCLE_DELTA_MS = new FloatAveragedWindow(3, 0.5f);
    //1.5;
    volatile long threadWorkTimePerCycle;
    volatile long cycleIdealNS;
    volatile long lastDur /* TODO lastNow */ = System.nanoTime();
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
    abstract protected void execute(Object r);

    @Override
    public final void execute(Runnable async) {
        execute((Object) async);
    }

    protected void update() {

        updateTiming();



//        float workPct = 0; //HACK Math.max(0.5f, workDemand());


        schedule(nar.what, ((RealTime)nar.time).durNS(), subCycleNS, 3,
            1-nar.loop.throttle.floatValue());
        //System.out.println(schedWrite.size() + " sched entries");
        //schedWrite.print(System.out); System.out.println();
    }

//    /** estimate proportion of duty cycle that needs applied to queued work tasks.
//     *  calculated by some adaptive method wrt profiled measurements */
//    abstract protected float workDemand();


//    /** special consumer implementing work instruction */
//    static final Consumer WORK = (e) -> {};

    /** hijackbag-like array to sample tasks from, tiled in proportional amounts like a discrete
     * roulette select on to a MetalAtomicReferenceArray*/
    @Deprecated static class Schedule<X>  {


        MetalAtomicReferenceArray<X /* switcher*/> tasks = new MetalAtomicReferenceArray<>(0);
        final AtomicInteger writer = new AtomicInteger(0);
        final AtomicBoolean writing = new AtomicBoolean(false);

        void resize(double durNS, double timeSliceNS, float supersampling) {
            resize( (int)Math.max(1, Math.ceil((durNS * supersampling) / timeSliceNS)) );
        }

        void resize(int capacity) {
            MetalAtomicReferenceArray<X> t = this.tasks;
            if (t.length() != capacity) {
                MetalAtomicReferenceArray<X> newTasks = new MetalAtomicReferenceArray<>(capacity);
                newTasks.fill(null); //TODO copy the original array, cyclically tiled into next
                this.tasks = newTasks;
            }
        }

        private void add(int k, X c) {
            MetalAtomicReferenceArray<X> t = this.tasks;
            int n = writer.get();
            int len = t.length();
            for (int i = 0; i < k; i++) {
                if (n == len)
                    n = 0;
                t.setFast(n++, c);
            }
            writer.set(n);
        }

        public X get(Random r) {
            MetalAtomicReferenceArray<X> t = this.tasks;
            return t.getFast(r.nextInt(t.length()));
        }

        public Schedule() {
            resize(1);
        }


        public void print(PrintStream out) {
            //forEachEvent((when,what)->out.println(Texts.timeStr(when) + "\t" + what));
        }
    }

    final Schedule<What> schedule = new Schedule<>();
    public void schedule(AntistaticBag<What> w, double durNS, double timeSliceNS, float supersampling, double sleepPct) {
        if (!schedule.writing.compareAndSet(false, true))
            return;

        try {

            schedule.resize(durNS, timeSliceNS, supersampling); //durNS *= supersampling;

//                if (workPct + sleepPct > 1) {
//                    //only work and sleep but calculate the correct proportion:
//                    workPct = (workPct / (workPct + sleepPct)); //renormalize
//                    sleepPct = 1 - workPct;
//                }

//                double workNS = (durNS * workPct);

//                int nw = (int) Math.max(1, workNS * supersampling / timeSliceNS);
//                add(nw, WORK);

            if (sleepPct > 0) {
                double sleepNS = (durNS * sleepPct);
                schedule.add(
                    (int) Math.floor(sleepNS * supersampling / timeSliceNS),
                    null);
            }

            double playPct = Math.max(0, 1f - sleepPct);
            if (playPct > 0) {
                double playNS = durNS * playPct;
                double mass = w.mass();
                for (What ww : w) {
                    double priNorm = ww.priElseZero() / mass;
                    double wPlayNS = (priNorm * playNS);
                    schedule.add((int) Math.max(1, wPlayNS * supersampling / timeSliceNS),
                        ww);
                    //System.out.println(ww + " " + priNorm + " " + np);
                }
            }
        } finally {
            schedule.writing.set(false);
        }
    }

    private void updateTiming() {

        long now = System.nanoTime();
        //long last = this.lastDur;
        this.lastDur = now;

        //long durDeltaTime = now - last

        NARLoop loop = nar.loop;
        cycleIdealNS = loop.periodNS();
        if (cycleIdealNS <= 0) {
            //paused
            threadWorkTimePerCycle = 0;
        } else {
            double throttle = loop.throttle.floatValue();

            //TODO better idle calculation in each thread / worker
            long workTargetNS = (long) (Util.lerp(throttle, 0, cycleIdealNS));
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

            this.threadWorkTimePerCycle = workTargetNS;

            if (nar.random().nextFloat() < queueLatencyMeasurementProbability)
                execute(new QueueLatencyMeasurement(nanoTime()));



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
