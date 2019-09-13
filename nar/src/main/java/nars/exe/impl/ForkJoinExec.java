package nars.exe.impl;

import jcog.Util;
import jcog.data.list.FasterList;
import nars.NAL;
import nars.attention.What;
import nars.control.How;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.primitive.ObjectFloatPair;
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

import static org.eclipse.collections.impl.tuple.Tuples.pair;

/**
 * TODO not finished
 */
public class ForkJoinExec extends MultiExec implements Thread.UncaughtExceptionHandler {

    private static final int SYNCH_ITERATION_MS = 20;
    int granularity = 3;

    private final ForkJoinPool pool;
    volatile private List<PlayBatch> active = new FasterList();

    public ForkJoinExec() {
        this(Runtime.getRuntime().availableProcessors());
    }
    public ForkJoinExec(int concurrency) {
        super(concurrency);

        if (concurrency >= Runtime.getRuntime().availableProcessors())
            pool = ForkJoinPool.commonPool();
        else {
            //public ForkJoinPool(int parallelism, ForkJoinPool.ForkJoinWorkerThreadFactory factory, UncaughtExceptionHandler handler, boolean asyncMode, int corePoolSize, int maximumPoolSize, int minimumRunnable, Predicate<? super ForkJoinPool> saturate, long keepAliveTime, TimeUnit unit) {

            int extra = 0;

            boolean asyncMode = NAL.FORK_JOIN_EXEC_ASYNC_MODE;

            pool = new ForkJoinPool(
                    concurrency,
                    ForkJoinPool.defaultForkJoinWorkerThreadFactory,
//                    (p) -> {             return new ForkJoinWorkerThread(p) { };         },
                    this,
                    asyncMode, 0, concurrency + extra, 1,
                    null, 60L, TimeUnit.SECONDS);
//            {
//                {
//                    //this.pollSubmission()
//                }
//            };
        }

//        if (concurrency >= Runtime.getRuntime().availableProcessors()/2) //HACK TODO make parameter
//            Exe.setExecutor(pool); //set this as the global executor

    }


    @Override
    public synchronized void synch() {
        logger.info("synch");

        int iter = 0;
//        Log.enter("synch");
        while (!pool.isQuiescent()) {
            logger.info("await quiescence {}", ++iter);
            if ((Thread.currentThread()) instanceof ForkJoinWorkerThread) {
                ForkJoinTask.helpQuiesce();
            } else {
                pool.awaitQuiescence(SYNCH_ITERATION_MS, TimeUnit.MILLISECONDS);
            }
        }
        if (iter > 0)
            logger.info("synch ready");
//        Log.exit();
    }


    @Override
    public boolean delete() {
        if (super.delete()) {
            pool.shutdownNow();
            return true;
        }
        return false;
    }

    @Override
    protected void update() {
        super.update();

        play();

        //logger.info(summary());
    }


    @Override
    public final int concurrency() {
        return pool.getParallelism();
    }


    /**
     * inject play tasks
     *
     * TODO better strategy:
     * compute a matrix of WxH
     * sample from the bags to or just directly calculate the elements of this matrix using a budgeting formula
     * then partition the matrix into >= P segments, which are chosen in order to maximize the
     * utilization of singleton W or H contexts by scheduling batching along, if any,
     * non-singleton transposed dimensions.
     */
    private void play() {
        long idealCycleNS = nar.loop.periodNS();
        if (idealCycleNS <= 0)
            return;

        double efficiency = Math.min(1, idealCycleNS / Math.max(1, (1.0E9 * nar.loop.cycleTime.asDouble())));

        Random rng = ThreadLocalRandom.current();

//        nar.what.commit(null);
//        nar.how.commit(null);

        //ObjectFloatHashMap<Pair<How,What>> play = new ObjectFloatHashMap<>(nar.how.size() * nar.what.size());
        FasterList<ObjectFloatPair<Pair<How,What>>> play = new FasterList(nar.how.size() * nar.what.size());

        double priTotal = 0;
        for (How h : nar.how) {
            if (!h.isOn()) continue;

            float hPri = h.pri();

            for (What w : nar.what) {
                if (!w.isOn()) continue;

            //for (int i = 0; i < n; i++ ) {

                //What w = nar.what.sample(rng); if (!w.isOn()) continue; //HACK embed in sample

                //PlayTask pt = new PlayTask(w, h);
                //pt.fork();

                float wh = Util.and(w.pri(), hPri);
                //play.addToValue(pair(h,w), wh);
                play.add(PrimitiveTuples.pair(pair(h,w),wh));
                priTotal += wh;
            }
        }
        if (play.isEmpty())
            return;


        //MutableList<ObjectFloatPair<Pair<How, What>>> l = play.keyValuesView().toSortedList(hwSort);

        int threads = pool.getParallelism();


        int tasks = threads * granularity;


        double playTimeNS = idealCycleNS * efficiency * nar.loop.throttle.floatValue();
        double nsPerPri = priTotal > Double.MIN_VALUE ? playTimeNS / priTotal : 0;
        double priPerTask = priTotal / tasks;

        int ll = play.size();
        FasterList<PlayBatch> next = new FasterList<>(tasks);
        double priCurrentThread;
        for (int i = 0; i < ll; ) {
            int j = i;
            priCurrentThread = 0;
            do {
                float p = play.get(j++).getTwo();
                priCurrentThread += p;
            } while (priCurrentThread < priPerTask && j < ll);
            ObjectFloatPair<Pair<How, What>>[] a = play.subList(i, j).toArray(EmptyLongFloatArray);
            Arrays.sort(a, hwSort);
            next.add(new PlayBatch(a, nsPerPri));
            i = j;
        }

        next.shuffleThis(rng);

        active.forEach(PlayBatch::stop);
        this.active = next;
        next.forEach(pool::execute);



//        if (ThreadLocalRandom.current().nextFloat() < 0.01f)
//            System.out.println(pool);
    }

//    static final Comparator<ObjectFloatPair<Pair<How, What>>> hwSort = Comparator.comparingLong(x ->
//        ((long) System.identityHashCode(x.getOne().getOne())) << 32 | System.identityHashCode(x.getTwo())
//    );
    static final Comparator<ObjectFloatPair<Pair<How, What>>> hwSort =
        Comparator.<ObjectFloatPair<Pair<How, What>>>comparingInt(x -> System.identityHashCode(x.getOne().getOne()))
                .thenComparingDouble(pairObjectFloatPair -> -pairObjectFloatPair.getTwo());


    static final ObjectFloatPair[] EmptyLongFloatArray = new ObjectFloatPair[0];


//    final static class PlayTask extends RecursiveAction {
//
//        final What w;
//        final How h;
//
//        PlayTask(What w, How h) {
//            this.w = w;
//            this.h = h;
//        }
//
//        @Override
//        protected void compute() {
//            boolean single = h.singleton();
//            if (!single || h.busy.compareAndSet(false, true)) {
//                try {
//                    long dur = dur(w.pri(), h.pri());
//                    h.runFor(w, dur);
//
//                } finally {
//                    if (single)
//                        h.busy.set(false);
//                }
//            }
//        }
//        static final long durationMinNS = TimeUnit.MICROSECONDS.toNanos(200);
//        static final long durationMaxNS = TimeUnit.MICROSECONDS.toNanos(1000);
//
//        static long dur(float whatPri, float howPri) {
//            float pri = (float) Math.sqrt(Util.and(whatPri, howPri)); //sqrt in attempt to compensate for the bag sampling's priority bias
//            return Util.lerp(pri, durationMinNS, durationMaxNS);
//        }
//
//    }


    //    @Override
//    protected void update() {
//        super.update();
//        System.out.println(summary());
//    }

    protected String summary() {
        return pool.toString();
//        return Map.of(
//                "pool", pool.toString(),
//                "time", nar.time(),
//                "pool threads", pool.getActiveThreadCount(),
//                "pool tasks pending", pool.getQueuedTaskCount()
//        ).toString();
    }

    @Override
    protected void execute(Object x) {
//        if (Thread.currentThread() instanceof ForkJoinWorkerThread) //TODO more robust test, in case multiple pools involved then we probably need to differentiate between local and remotes
//            executeNow(x);
//        else {
        pool.execute(x instanceof Runnable ? ((Runnable)x) : new MyRunnable(x));
//        }
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        throwable.printStackTrace(); //TODO
    }


    private final class MyRunnable implements Runnable {
        private final Object x;

        MyRunnable(Object x) {
            this.x = x;
        }

        @Override
        public void run() {
            ForkJoinExec.this.executeNow(x);
        }
    }

    private static class PlayBatch extends RecursiveAction {
        private final ObjectFloatPair[] a;
        private final double nsPerPri;
        volatile boolean running = true;

        int loops = 2;

        /** even if nsPerPri == 0, one iteration should execute */
        public PlayBatch(ObjectFloatPair[] a, double nsPerPri) {
            this.a = a;
            this.nsPerPri = nsPerPri;
        }

        public void stop() {
            running = false;
            tryUnfork();
        }

        @Override
        protected void compute() {

            //ForkJoinPool pool = ForkJoinTask.getPool();

            int remain = a.length;
            do {
                How cur = null;
                //boolean single = false;
                for (int i = 0, aLength = a.length; i < aLength; i++) {
                    if (!running)
                        break;

                    ObjectFloatPair<Pair<How, What>> aa = a[i];
                    if (aa == null)
                        continue;

                    How next = aa.getOne().getOne();
                    if (cur != next) {
//                        if (cur != null && single) {
//                            if (single) {
//                                cur.busy.set(false);
//                                single = false;
//                            }
//                        }

//                        single = next.singleton();
//                        if (single && !next.busy.compareAndSet(false, true)) {
//                            //TODO skip ahead the same nexts
//                            //while (i < aLength && )
//
//                            single = false;
//                            continue;
//                        }
                        cur = next;
                    }

                    long dur = Math.round(nsPerPri * aa.getTwo());

                    cur.runFor(aa.getOne().getTwo(), dur);

                    a[i] = null;
                    remain--;

                }
//                if (single) {
//                    cur.busy.set(false);
//                }


//                {
//                    //check for any work
//                    ForkJoinTask<?> work = ForkJoinTask.pollSubmission();
//                    if (work!=null) {
//                        try {
//                            work.get();
//                        } catch (InterruptedException | ExecutionException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                }

            } while (remain > 0 && --loops > 0);

        }

    }
}
