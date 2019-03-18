package nars.exe.impl;

import com.google.common.base.Joiner;
import jcog.Texts;
import jcog.Util;
import jcog.data.list.FasterList;
import jcog.math.FloatAveragedWindow;
import nars.NAR;
import nars.exe.Causable;
import nars.exe.Exec;
import nars.exe.Valuator;
import nars.task.AbstractTask;
import nars.task.ITask;
import nars.task.NALTask;
import nars.task.TaskProxy;
import nars.time.clock.RealTime;

import java.io.IOException;
import java.util.function.Consumer;

import static java.lang.System.nanoTime;

abstract public class MultiExec extends UniExec {

    protected static final float inputQueueSizeSafetyThreshold = 1f;

    private final Valuator valuator;

    protected long threadWorkTimePerCycle, threadIdleTimePerCycle;

    /**
     * global sleep nap period
     */
    protected static final long NapTimeNS = 500 * 1000; //on the order of 0.5ms

    static private final float queueLatencyMeasurementProbability = 0.01f;

    static final double lagAdjustmentFactor  =
            1.0;
            //1.5;

    /**
     * proportion of time spent in forced curiosity
     */
    private final float explorationRate = 0.05f;

    protected long cycleIdealNS;

    public MultiExec(Valuator valuator, int concurrency  /* TODO adjustable dynamically */) {
        super(concurrency, concurrency);
        this.valuator = valuator;
    }

    @Override
    public final void input(Object x) {
        if (x instanceof NALTask || x instanceof TaskProxy)
            input((ITask) x);
        else
            executeLater(x);
    }

    @Override
    public final void input(Consumer<NAR> r) {
        executeLater(r);
    }

    @Override
    public final void execute(Runnable r) {
        executeLater(r);
    }

    abstract protected void executeLater(/*@NotNull */Object x);

    long lastCycle = System.nanoTime();

    protected void update() {

        long now = System.nanoTime();
        long last = this.lastCycle;
        this.lastCycle = now;
        updateTiming(now - last);

        valuator.update(nar);

        prioritize();

    }

    @Override protected void onCycle(NAR nar) {
        nar.time.schedule(this::executeLater);
    }

    final FloatAveragedWindow CYCLE_DELTA_MS = new FloatAveragedWindow(16, 0.25f);

    private void updateTiming(long _cycleDeltaNS) {

        cycleIdealNS = nar.loop.periodNS();
        if (cycleIdealNS < 0) {
            //paused
            threadIdleTimePerCycle = NapTimeNS;
        } else {
            double throttle = nar.loop.throttle.floatValue();

            //TODO better idle calculation in each thread / worker
            long workTargetNS = (long)(Util.lerp(throttle, 0, cycleIdealNS));
            long cycleActualNS = (long)(1_000_000.0 * CYCLE_DELTA_MS.valueOf((float)(_cycleDeltaNS/1.0E6)));
            long lagMeanNS = cycleActualNS - cycleIdealNS;

                    //0.5;
            if (lagMeanNS > 0)
                workTargetNS = (long) Math.max(workTargetNS - lagMeanNS * lagAdjustmentFactor / concurrency(), 0);

            threadWorkTimePerCycle = workTargetNS;
            threadIdleTimePerCycle = cycleIdealNS - workTargetNS;

            if (nar.random().nextFloat() < queueLatencyMeasurementProbability) {
                input(new QueueLatencyMeasurement(nanoTime()));
            }
        }

    }


    @Override
    public void start(NAR n) {
        if (!(n.time instanceof RealTime))
            throw new UnsupportedOperationException("non-realtime clock not supported");

        super.start(n);

        //ons.add(DurService.on(n, this::update));
        ons.add(n.onCycle(this::update));

    }


    private void prioritize() {
        int n = cpu.size();
        if (n == 0)
            return;

        float[] valMin = {Float.POSITIVE_INFINITY}, valMax = {Float.NEGATIVE_INFINITY};

        long now = nar.time();


        cpu.forEach(s -> {
            Causable c = s.get();

            boolean sleeping = c.sleeping(now);
            if (sleeping)
                return;

            float vr;
            long tUsed = s.used();
            if (tUsed <= 0) {
                s.value = Float.NaN;
                vr = 0;
            } else {
                float v = Math.max(0, s.value = c.value());
                double cyclesUsed = ((double) tUsed) / cycleIdealNS;
                vr = (float) (v / (1 + cyclesUsed));
                assert (vr == vr);
            }

            s.valueRate = vr;

            if (vr > valMax[0]) valMax[0] = vr;
            if (vr < valMin[0]) valMin[0] = vr;

        });

        float valRange = valMax[0] - valMin[0];
        if (Float.isFinite(valRange) && Math.abs(valRange) > Float.MIN_NORMAL) {
            float exploreMargin = explorationRate * valRange;
            cpu.forEach(s -> {
                Causable c = s.get();
                if (c.sleeping()) {
                    s.pri(0);
                } else {
                    float vNorm = Util.normalize(s.valueRate, valMin[0] - exploreMargin, valMax[0]);
                    //pri(s, vNorm);
                    s.pri(vNorm);
                }
            });
        } else {
            //FLAT
            float pFlat = 1f / n;
            cpu.forEach(s -> s.pri(pFlat));
        }

    }

//    protected void pri(Prioritizable s, float p) {
//        //s.pri(p);
//        s.pri(s.priElseZero() * momentum + (1-momentum) * p);
//    }


    @Override
    public void print(Appendable out) {
        try {
            Joiner.on('\n').appendTo(out, cpu);
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    static private class QueueLatencyMeasurement extends AbstractTask {

        private final long start;

        QueueLatencyMeasurement(long start) {
            this.start = start;
        }

        @Override
        public ITask next(NAR n) {
            long end = nanoTime();
            queueLatency(start, end, n);
            return null;
        }

        /**
         * measure queue latency "ping"
         */
        static void queueLatency(long start, long end, NAR n) {
            long latencyNS = end - start;
            double cycles = latencyNS / ((double) (n.loop.periodNS() / n.exe.concurrency()));
            if (cycles > 0.5) {
                Exec.logger.info("queue latency {} ({} cycles)", Texts.timeStr(latencyNS), Texts.n4(cycles));
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
