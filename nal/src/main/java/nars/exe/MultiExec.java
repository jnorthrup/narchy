package nars.exe;

import com.google.common.base.Joiner;
import jcog.TODO;
import jcog.Texts;
import jcog.Util;
import jcog.data.bit.AtomicMetalBitSet;
import jcog.data.list.FasterList;
import jcog.event.Off;
import jcog.exe.AffinityExecutor;
import jcog.exe.Exe;
import jcog.exe.valve.InstrumentedWork;
import jcog.exe.valve.TimeSlicing;
import jcog.math.FloatRange;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

abstract public class MultiExec extends UniExec {

    static final int contextGranularity = 4;

    private static final float inputQueueSizeSafetyThreshold = 1f;
    private final Revaluator revaluator;

    final AtomicMetalBitSet sleeping = new AtomicMetalBitSet();


    /**
     * increasing the rate closer to 1 reduces the dynamic range of concentration specificity allocation
     */
    public final FloatRange explorationRate = FloatRange.unit(0.1f);

    protected long idleTimePerCycle;

    /** global sleep nap period */
    private static final long NapTime = 2 * 1000 * 1000; //on the order of ~1ms

    private final float queueLatencyMeasurementProbability = 0.05f;
    private Runnable nextCycle;

    MultiExec(Revaluator revaluator, int concurrency  /* TODO adjustable dynamically */) {
        super(concurrency, concurrency);
        this.revaluator = revaluator;
    }

//    @Override
//    public void input(Collection<? extends ITask> xx) {
//        int CHUNK_THRESH = 2;
//        int conc = concurrency();
//        if (xx.size() < conc * CHUNK_THRESH) {
//            executeLater(new AbstractTask.TasksIterable(xx));
//        } else {
//            if (xx instanceof FasterList) {
//                ((FasterList<ITask>)xx).chunk(conc).forEach((c)->{
//                    executeLater(new AbstractTask.TasksIterable(c));
//                });
//            } else {
//                executeLater(new AbstractTask.TasksIterable(xx)); //TODO
//            }
//        }
//    }

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
                input(new QueueLatencyMeasurement(System.nanoTime()));
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

        this.nextCycle = n.loop.run;

    }

    @Override public TimeSlicing<Object, String> scheduler() {
        return new TimeSlicing<>("CPU", 1, nar.exe) {


            @Deprecated
            @Override
            protected void trySpawn() {

            }

            @Override
            @Deprecated
            protected boolean work() {
                throw new UnsupportedOperationException();
            }

            @Override
            public TimeSlicing commit() {

                int n = size();
                if (n == 0)
                    return this;

                /** min time granularity */
                long epsilonNS =
                        1000 /* 1 uS */;

                //nar.loop.cycleTimeNS / (n * 2);

                double[] valMin = {Double.POSITIVE_INFINITY}, valMax = {Double.NEGATIVE_INFINITY};

                long now = nar.time();

                this.forEach((InstrumentedWork s) -> {
                    Causable c = (Causable) s.who;

                    boolean sleeping = c.sleeping(now);
                    MultiExec.this.sleeping.set(c.scheduledID, sleeping);
                    if (sleeping) {
                        s.pri(0);
                        return;
                    }

                    double v = c.value();
                    assert(v==v);

                    s.value = v;
                    if (v > valMax[0]) valMax[0] = v;
                    if (v < valMin[0]) valMin[0] = v;

                });

                double valRange = valMax[0] - valMin[0];


                int sleeping = MultiExec.this.sleeping.cardinality();
                if (Double.isFinite(valRange) && Math.abs(valRange) > Double.MIN_NORMAL) {

                    final double[] valRateMin = {Double.POSITIVE_INFINITY}, valRateMax = {Double.NEGATIVE_INFINITY};
                    this.forEach((InstrumentedWork s) -> {
                        Causable c = (Causable) s.who;
                        if (MultiExec.this.sleeping.get(c.scheduledID))
                            return;

//                            Causable x = (Causable) s.who;
                        //if (x instanceof Causable) {

                        long accumTimeNS = Math.max(epsilonNS, s.accumulatedTime(true));
                        double accumTimeMS = accumTimeNS / 1_000_000.0;
                        double valuePerMS = (s.value / accumTimeMS);
                        s.valueRate = valuePerMS;
                        if (valuePerMS > valRateMax[0]) valRateMax[0] = valuePerMS;
                        if (valuePerMS < valRateMin[0]) valRateMin[0] = valuePerMS;
                    });
                    double valRateRange = valRateMax[0] - valRateMin[0];
                    if (Double.isFinite(valRateRange) && valRateRange > Double.MIN_NORMAL * n) {

                        float explorationRate = MultiExec.this.explorationRate.floatValue();

                        double[] valueRateSum = {0};

                        forEach((InstrumentedWork s) -> {
                            Causable c = (Causable) s.who;
                            if (MultiExec.this.sleeping.get(c.scheduledID))
                                return;

                            double v = s.valueRate, vv;
                            if (v == v) {
                                vv = (v - valRateMin[0]) / valRateRange;
                            } else {
                                vv = 0;
                            }
                            s.valueRateNormalized = vv;
                            valueRateSum[0] += vv;
                        });

                        if (valueRateSum[0] > Double.MIN_NORMAL) {

                            float fair = 1f/(n-sleeping);

                            forEach((InstrumentedWork s) -> {
                                Causable c = (Causable) s.who;
                                if (MultiExec.this.sleeping.get(c.scheduledID))
                                    return;


                                float p = (float) (s.valueRateNormalized / valueRateSum[0]);

                                s.pri(
                                        Util.lerp(explorationRate, p, fair)
                                );
                                //System.out.println(s + " " + c);
                            });

                            return this;
                        }
                    }

                }

                /** flat */
                float flatDemand = n > 1 ? (1f / (n- sleeping)) : 1f;
                forEach((InstrumentedWork s) -> {
                    Causable c = (Causable) s.who;
                    if (!MultiExec.this.sleeping.get(c.scheduledID))
                        s.pri(flatDemand);
                });


                return this;
            }
        };
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


    @Override
    public void print(Appendable out) {
        try {
            Joiner.on('\n').appendTo(out, can.values());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean execute(FasterList b, int concurrency) {
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

                double minJiffyTime = 50 * 1000; //50uS
                double maxJiffyTime = playTime / ((double) (contextGranularity));
                        //* Math.max(1, can.size() )));

                long until = System.nanoTime() + playTime;
                do {
                    InstrumentedCausable c = can.getIndex(rng);
                    if (c == null) break; //empty

                    if (!sleeping.get(c.c.scheduledID)) {

                        boolean singleton = c.c.singleton();
                        if (!singleton || c.c.busy.compareAndSet(false, true)) {
                            try {

                                long runtimeNS =
                                        Math.round(Util.lerp(c.pri(), minJiffyTime, maxJiffyTime));

                                if (runtimeNS > 0) {
                                    long before = System.nanoTime();
                                    c.runUntil(before, runtimeNS);
                                    /*
                                    long after = System.nanoTime();
                                    long excessNS = (after - before) - runtimeNS;
                                    if (excessNS > runtimeNS/2) {
                                        System.out.println(c + " exceeded runtime of " + Texts.timeStr(runtimeNS) + " by " + Texts.timeStr(excessNS));
                                    }*/
                                }

                            } finally {
                                if (singleton)
                                    c.c.busy.set(false);
                            }
                        }
                    }

                } while (queueSafe() && (until > System.nanoTime()));

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

    private class QueueLatencyMeasurement extends AbstractTask {

        private final long start;

        QueueLatencyMeasurement(long start) {
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
