package nars.exe.impl;

import com.google.common.base.Joiner;
import jcog.Texts;
import jcog.Util;
import jcog.data.list.FasterList;
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

    protected static final float inputQueueSizeSafetyThreshold = 0.999f;

    private final Valuator valuator;

    protected long threadWorkTimePerCycle, threadIdleTimePerCycle;

    /**
     * global sleep nap period
     */
    protected static final long NapTime = 2 * 1000 * 1000; //on the order of ~1ms

    static private final float queueLatencyMeasurementProbability = 0.05f;

    /**
     * proportion of time spent in forced curiosity
     */
    private float explorationRate = 0.05f;

    protected long cycleNS;
    protected int workGranularity;

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

    private void executeLater(/*@NotNull */Object x) {

        in.add(x, (xx)->{
            Exec.logger.warn("{} blocked queue on: {}", this, xx);
            executeNow(xx);
        });
    }


    protected  void update() {

        updateTiming();

        valuator.update(nar);
    }


    private void updateTiming() {
        cycleNS = nar.loop.periodNS();
        if (cycleNS < 0) {
            //paused
            threadIdleTimePerCycle = NapTime;
        } else {
            double throttle = nar.loop.throttle.floatValue();

            //TODO better idle calculation in each thread / worker
            threadIdleTimePerCycle = Math.round(Util.clamp(cycleNS * (1 - throttle), 0, cycleNS));
            threadWorkTimePerCycle = cycleNS - threadIdleTimePerCycle;

            if (nar.random().nextFloat() < queueLatencyMeasurementProbability) {
                input(new QueueLatencyMeasurement(nanoTime()));
            }
        }
        workGranularity =
                //Math.max(1, concurrency() + 1);
                Math.max(1, concurrency() - 1);
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
                double cyclesUsed = ((double) tUsed) / cycleNS;
                vr = (float) (v / (1 + cyclesUsed));
                assert (vr == vr);
            }

            s.valueRate = vr;

            if (vr > valMax[0]) valMax[0] = vr;
            if (vr < valMin[0]) valMin[0] = vr;

        });

        float valRange = valMax[0] - valMin[0];
        if (Float.isFinite(valRange) && Math.abs(valRange) > Float.MIN_NORMAL) {
            float exp = explorationRate * valRange;
            cpu.forEach(s -> {
                Causable c = s.get();
                if (c.sleeping()) {
                    s.pri(0);
                } else {
                    float vNorm = Util.normalize(s.valueRate, valMin[0] - exp, valMax[0]);
                    s.pri(vNorm);
                }
            });
        } else {
            //FLAT
            float p = 1f / n;
            cpu.forEach(s -> s.pri(p));
        }

    }

    protected void onCycle(NAR nar) {
        nar.time.schedule(this::executeLater);

        prioritize();
    }


    @Override
    public void print(Appendable out) {
        try {
            Joiner.on('\n').appendTo(out, cpu);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected long work(float responsibility, FasterList buffer) {

        int available = in.size();
        if (available > 0) {
            //do {
            long workStart = nanoTime();

            int batchSize = //Util.lerp(throttle,
                    //available, /* all of it if low throttle. this allows most threads to remains asleep while one awake thread takes care of it all */
                    (int) Math.ceil(((responsibility * available) / workGranularity))
                    //)
                    ;

            int got = in.remove(buffer, batchSize);
            if (got > 0)
                execute(buffer, 1, MultiExec.this::executeNow);


            long workEnd = nanoTime();
            //} while (!queueSafe());

            return workEnd - workStart;
        }

        return 0;
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
            b.forEach(each);
        } else {

            float granularity = 1 * (concurrency / 2f);
            int chunkSize = Math.max(1, (int) Math.min(concurrency, b.size() / (granularity)));

            (((FasterList<?>) b).chunkView(chunkSize))
                    .parallelStream().forEach(x -> x.forEach(each));
        }

        b.clear();
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