package nars.exe;

import com.google.common.base.Joiner;
import jcog.Texts;
import jcog.Util;
import jcog.data.list.FasterList;
import jcog.event.Off;
import jcog.exe.AffinityExecutor;
import jcog.exe.Exe;
import jcog.random.SplitMix64Random;
import jcog.util.ArrayUtils;
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
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

import static java.lang.System.nanoTime;
import static nars.time.Tense.ETERNAL;

abstract public class MultiExec extends UniExec {

    private static final float inputQueueSizeSafetyThreshold = 0.999f;
    private final Valuator valuator;


    protected long threadWorkTimePerCycle, threadIdleTimePerCycle;

    /**
     * global sleep nap period
     */
    private static final long NapTime = 2 * 1000 * 1000; //on the order of ~1ms

    static private final float queueLatencyMeasurementProbability = 0.05f;

    /**
     * proportion of time spent in forced curiosity
     */
    private float explorationRate = 0.2f;


    protected long cycleNS;

    MultiExec(Valuator valuator, int concurrency  /* TODO adjustable dynamically */) {
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

        if (!in.offer(x)) {
            logger.warn("{} blocked queue on: {}", this, x);
            executeNow(x);
        }
    }


    private void onDur() {

        updateTiming();

        valuator.update(nar);

        //sharing.commit();

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

            float granularity = 1 * (concurrency / 2f);
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

        double granularity = 2;

        final AffinityExecutor exe = new AffinityExecutor();
        private List<Worker> workers;

        public WorkerExec(Valuator r, int threads) {
            this(r, threads, false);
        }

        public WorkerExec(Valuator valuator, int threads, boolean affinity) {
            super(valuator, threads);
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

            private long subCycleMinNS = 50_000;
            private long subCycleMaxNS;

            private final FasterList schedule = new FasterList(inputQueueCapacityPerThread);

            TimedLink.MyTimedLink[] play = new TimedLink.MyTimedLink[0];

            private boolean alive = true;

            final SplitMix64Random rng;
            private long deadline;

            int i = 0;
            long prioLast = ETERNAL;
            private int n;

            private long prioritizeEveryCycles;

            Worker() {
                rng = new SplitMix64Random((31L * System.identityHashCode(this)) + nanoTime());
            }

            @Override
            public void run() {

                while (alive) {

                    long workTime = work();

                    long playTime = threadWorkTimePerCycle - workTime;
                    if (playTime > 0)
                        play(playTime);

                    sleep();
                }
            }


            private long work() {
                long workStart = nanoTime();

                float workGranularity =
                        //Math.max(1, concurrency() + 1);
                        Math.max(1, concurrency() - 1);
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
                                    Math.max(1, (int) ((available / Math.max(1, workGranularity))))
                            );

                    int drained = in.remove(schedule, batchSize);
                    if (drained > 0)
                        execute(schedule, 1);
                    else
                        break;

                } while (!queueSafe());

                long workEnd = nanoTime();

                return workEnd - workStart;
            }

            private final BooleanSupplier deadlineFn = this::deadline;

            private void play(long playTime) {

                n = cpu.size();
                if (n == 0)
                    return;

                long now = nar.time();
                if (now > prioLast + prioritizeEveryCycles) {
                    prioritizeEveryCycles =
                            nar.dur(); //update current dur
                    //2 * nar.dtDither();

                    prioLast = now;
                    prioritize(threadWorkTimePerCycle);
                }

                long start = nanoTime();
                long until = start + playTime, after = start /* assigned for safety */;


                int skip = 0;
                do {

                    TimedLink.MyTimedLink s = play[i++];
                    if (i == n) i = 0;

                    long sTime = s.time;

                    Causable c = s.can;

                    boolean played = false;
                    if (sTime <= 0 || c.sleeping()) {

                    } else {

                        boolean singleton = c.singleton();
                        if (!singleton || c.busy.compareAndSet(false, true)) {

                            long before = nanoTime();

                            long runtimeNS = Math.min(until - before, Math.min(sTime, subCycleMaxNS));

                            try {

                                if (runtimeNS > 0) {
                                    deadline = before + runtimeNS;

                                    c.next(nar, deadlineFn);

                                    played = true;
                                    after = nanoTime();
                                    s.use(after - before);
                                }
                            } catch (Throwable t) {
                                logger.error("{} {}", this, t);
                            } finally {
                                if (singleton)
                                    c.busy.set(false);
                            }

                        }
                    }

                    if (!played && ++skip == n)
                        break; //go to work early

                } while ((until > after) && queueSafe());
//                System.out.println(
//                    this + "\tplaytime=" + Texts.timeStr(playTime) + " " +
//                        Texts.n2((((double)(after - start))/playTime)*100) + "% used"
//                );
            }

            private void prioritize(long workTimeNS) {

                int n = this.n;
                if (n == 0)
                    return;

//                /** expected time will be equal to or less than the max due to various overheads on resource constraints */
//                double expectedWorkTimeNS = (((double)workTimeNS) * expectedWorkTimeFactor); //TODO meter and predict

                subCycleMaxNS = (long) (cycleNS / granularity);

                if (play.length != n) {
                    //TODO more careful test for change
                    play = new TimedLink.MyTimedLink[n];
                    for (int i = 0; i < n; i++)
                        play[i] = cpu.get(i).my();
                }

                if (n > 2)
                    ArrayUtils.shuffle(play, rng); //each worker gets unique order


                //schedule
                //TODO Util.max((TimedLink.MyTimedLink m) -> m.time, play);
                long minTime = -Util.max((TimedLink.MyTimedLink x) -> -x.time, play);
                long existingTime = Util.sum((TimedLink.MyTimedLink x) -> Math.max(0, x.time), play);
                long remainingTime = workTimeNS - existingTime;
                if (remainingTime > subCycleMinNS) {
                    long shift = minTime < 0 ? 1 - minTime : 0;
                    for (TimedLink.MyTimedLink m : play) {
                        int t = Math.round(shift + remainingTime * m.pri());
                        m.add(Math.max(subCycleMinNS, t), -workTimeNS, +workTimeNS);
                    }
                }
            }

            private boolean deadline() {
                return nanoTime() < deadline;
            }

            void sleep() {
                long i = WorkerExec.this.threadIdleTimePerCycle;
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
