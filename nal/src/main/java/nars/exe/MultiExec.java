package nars.exe;

import com.google.common.base.Joiner;
import jcog.Texts;
import jcog.Util;
import jcog.data.bit.AtomicMetalBitSet;
import jcog.data.list.FasterList;
import jcog.event.Off;
import jcog.exe.AffinityExecutor;
import jcog.exe.Exe;
import jcog.random.SplitMix64Random;
import nars.NAR;
import nars.control.DurService;
import nars.task.AbstractTask;
import nars.task.ITask;
import nars.task.NALTask;
import nars.task.TaskProxy;
import nars.time.clock.RealTime;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;

abstract public class MultiExec extends UniExec {

    static final int contextGranularity = 2;

    private static final float inputQueueSizeSafetyThreshold = 1f;
    private final Revaluator revaluator;

    final AtomicMetalBitSet sleeping = new AtomicMetalBitSet();
    private long cycleTimeNS;

    protected long idleTimePerCycle;

    /** global sleep nap period */
    private static final long NapTime = 2 * 1000 * 1000; //on the order of ~1ms

    private final float queueLatencyMeasurementProbability = 0.05f;

    MultiExec(Revaluator revaluator, int concurrency  /* TODO adjustable dynamically */) {
        super(concurrency, concurrency);
        this.revaluator = revaluator;
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

        if (!in.offer(x)) {
            logger.warn("{} blocked queue on: {}", this, x);
            //in.add(x);
            executeNow(x);
        }
    }


    private void onDur() {

        updateTiming();

        revaluator.update(nar);

        //sharing.commit();

    }


    private void updateTiming() {
        double cycleNS = nar.loop.periodNS();
        if(cycleNS < 0) {
            //paused
            idleTimePerCycle = NapTime;
            cycleTimeNS = 0;
        } else {
            double throttle = nar.loop.throttle.floatValue();

            //TODO better idle calculation in each thread / worker
            idleTimePerCycle = Math.round(Util.clamp(cycleNS * (1 - throttle), 0, cycleNS));

            cycleTimeNS = Math.max(1, Math.round(cycleNS * throttle)) * concurrency();

            if (nar.random().nextFloat() < queueLatencyMeasurementProbability) {
                input(new QueueLatencyMeasurement(System.nanoTime()));
            }
        }
    }


    @Override
    public void start(NAR n) {
        if (!(n.time instanceof RealTime))
            throw new UnsupportedOperationException("non-realtime clock not supported");

        super.start(n);
        ons.add(DurService.on(n, this::onDur));

    }


    private void prioritize() {
        int n = cpu.size();
        if (n == 0)
            return;

        float[] valMin = {Float.POSITIVE_INFINITY}, valMax = {Float.NEGATIVE_INFINITY};

        long now = nar.time();
        long maxTime = cpu.reduce((s,max)->Math.max(max, s.time.getOpaque()), Long.MIN_VALUE);
        if (maxTime < 0) {
            //shift all time up
            long shift = 1-maxTime;
            cpu.forEach(c -> {
                c.add(shift, cycleTimeNS);
            });
        }

        cpu.forEach(s -> {
            Causable c = s.get();

            boolean sleeping = c.sleeping(now);
            MultiExec.this.sleeping.set(c.scheduledID, sleeping);
            if (sleeping) {
                return;
            }

            long tUsed = s.used();
            float v = tUsed > 0 ? (float)((c.value())/(tUsed/1.0e9)) : 0;
            s.value = v;
            assert(v==v);

            if (v > valMax[0]) valMax[0] = v;
            if (v < valMin[0]) valMin[0] = v;

        });

        float valRange = valMax[0] - valMin[0];


        final double[] pSum = {0};
        if (Float.isFinite(valRange) && Math.abs(valRange) > Float.MIN_NORMAL) {
            cpu.forEach((s) -> {
                Causable c = s.get();
                if (MultiExec.this.sleeping.get(c.scheduledID)) {
                    s.pri(0);
                } else {
                    float vNorm = Util.normalize(s.value, valMin[0], valMax[0]);
                    s.pri(vNorm);
                    pSum[0] += vNorm;
                }
            });
        } else {
            cpu.forEach((s) -> {
                Causable c = s.get();
                if (MultiExec.this.sleeping.get(c.scheduledID)) {
                    s.pri(0f);
                } else {
                    float p = 0.5f;
                    s.pri(p);
                    pSum[0] += p;
                }
            });
        }
        cpu.forEach((s) -> {
            s.add( Math.max(1, Math.round(cycleTimeNS * s.priElseZero()/pSum[0])), cycleTimeNS );
        });

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

    boolean execute(FasterList b, int concurrency) {
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

            float granularity = 1 * (concurrency/2f);
            int chunkSize = Math.max(1, (int) Math.min(concurrency, b.size() / (granularity)));

            (((FasterList<?>) b).chunkView(chunkSize))
                    .parallelStream().forEach(x -> x.forEach(this::executeNow));
        }

        b.clear();
        return true;
    }

    public static class WorkerExec extends MultiExec {

        final int threads;
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


        @Override
        public void start(NAR n) {

            int procs = Runtime.getRuntime().availableProcessors();

            super.start(n);

            workers = exe.execute(Worker::new, threads, affinity);

            if (concurrency() > procs / 2) {
                /** absorb system-wide tasks rather than using the default ForkJoin commonPool */
                Exe.setExecutor(this);
            }

        }


        @Override
        public void stop() {
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

            Worker() {
                 rng = new SplitMix64Random((31L * System.identityHashCode(this)) + System.nanoTime());
            }

            @Override
            public void run() {

                while (alive) {

                    long workTime = work();

                    long cycleTimeNS =
                            //cpu.cycleTimeNS.longValue();
                            nar.loop.periodNS();

                    long playTime = cycleTimeNS - workTime;
                    if (playTime > 0) {
                        play(playTime);
                    }


                    sleep();
                }
            }


            private long work() {
                long workStart = System.nanoTime();

                float granularity =
                    Math.max(1, concurrency() + 1);
                    //Math.max(1, concurrency() - 1);
                    //concurrency() / 2f;
                    //1;
                float throttle = nar.loop.throttle.floatValue();

                do {
                    int available = in.size(); //in.capacity();
                    if (available == 0)
                        break;

                    int batchSize =
                            Util.lerp(throttle,
                                    available, /* all of it if low throttle. this allows most threads to remains asleep while one awake thread takes care of it all */
                                    Math.max(1, (int) ((available / Math.max(1, granularity))))
                            );

                    int drained = in.remove(schedule, batchSize);
                    if (drained > 0)
                        execute(schedule, 1);
                    else
                        break;

                } while (!queueSafe());

                long workEnd = System.nanoTime();

                return workEnd - workStart;
            }

            private void play(long playTime) {

                long until = System.nanoTime() + playTime;
                int n = cpu.items.size();
                if (n == 0)
                    return;
                do {
                    TimedLink s = cpu.get(rng.nextInt(n));
                    if (s == null)
                        break;

                    Causable c = s.get();

                    if (!sleeping.get(c.scheduledID)) {

                        boolean singleton = c.singleton();
                        if (!singleton || c.busy.compareAndSet(false, true)) {
                            try {

                                long runtimeNS =
                                        s.time.getOpaque() / contextGranularity;
                                if (runtimeNS > 0) {
                                    long before = System.nanoTime();
                                    c.runUntil(before + runtimeNS, nar);
                                    long after = System.nanoTime();
                                    s.use(after - before);
                                }

                            } finally {
                                if (singleton)
                                    c.busy.set(false);
                            }
                        }
                    }

                } while (/*queueSafe() && */(until > System.nanoTime()));

            }

            void sleep() {
                long i = WorkerExec.this.idleTimePerCycle;
                if (i > 0) {

                    Util.sleepNSwhile(i, NapTime, WorkerExec.this::queueSafe);
                }
            }

            @Override
            public void off() {
                if (alive) {
                    synchronized (this) {
                        alive = false;

                        //execute remaining tasks in callee's thread
                        schedule.removeIf(x -> {
                            executeNow(x);
                            return true;
                        });
                    }
                }
            }
        }


        private boolean queueSafe() {
            return in.availablePct(inputQueueCapacityPerThread) >= inputQueueSizeSafetyThreshold;
        }
    }

    static private class QueueLatencyMeasurement extends AbstractTask {

        private final long start;

        QueueLatencyMeasurement(long start) {
            this.start = start;
        }

        @Override
        public ITask next(NAR n) {
            long end = System.nanoTime();
            queueLatency(start, end, n);
            return null;
        }

        /** measure queue latency "ping" */
        static void queueLatency(long start, long end, NAR n) {
            long latencyNS = end - start;
            double cycles = latencyNS / ((double)n.loop.cycleTimeNS);
            if (cycles > 0.5) {
                logger.info("queue latency {} ({} cycles)", Texts.timeStr(latencyNS), Texts.n4(cycles));
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
