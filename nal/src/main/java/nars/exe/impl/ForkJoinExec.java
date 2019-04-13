package nars.exe.impl;

import jcog.Util;
import jcog.data.list.FasterList;
import nars.attention.What;
import nars.control.How;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.api.tuple.primitive.ObjectLongPair;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectLongHashMap;

import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

import static java.lang.Integer.min;
import static org.eclipse.collections.impl.tuple.Tuples.pair;

/**
 * TODO not finished
 */
public class ForkJoinExec extends MultiExec implements Thread.UncaughtExceptionHandler {

    private static final int SYNCH_ITERATION_MS = 20;
    static final long durationMinNS = TimeUnit.MICROSECONDS.toNanos(200);
    static final long durationMaxNS = TimeUnit.MICROSECONDS.toNanos(1000);

    private ForkJoinPool pool;

    public ForkJoinExec() {
        this(Runtime.getRuntime().availableProcessors());
    }
    public ForkJoinExec(int concurrency) {
        super(concurrency);

        if (concurrency >= Runtime.getRuntime().availableProcessors())
            pool = ForkJoinPool.commonPool();
        else {
            //public ForkJoinPool(int parallelism, ForkJoinPool.ForkJoinWorkerThreadFactory factory, UncaughtExceptionHandler handler, boolean asyncMode, int corePoolSize, int maximumPoolSize, int minimumRunnable, Predicate<? super ForkJoinPool> saturate, long keepAliveTime, TimeUnit unit) {

            pool = new ForkJoinPool(
                    concurrency,
                    ForkJoinPool.defaultForkJoinWorkerThreadFactory,
//                    (p) -> {             return new ForkJoinWorkerThread(p) { };         },
                    this,
                    true, 0, concurrency, 1,
                    null, 60L, TimeUnit.SECONDS) {
                {
                    //this.pollSubmission()
                }
            };
        }

//        pool = ForkJoinPool.commonPool();

//        if (concurrency >= Runtime.getRuntime().availableProcessors()/2) //HACK TODO make parameter
//            Exe.setExecutor(pool); //set this as the global executor

    }


    @Override
    public void synch() {
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

        //logger.info("{}", pool);

        float mix = 0.1f; //work to play ratio?
        float rate = 3; //HACK
        float load =
                Math.min(1, (mix * (pool.getQueuedSubmissionCount() /*+ pool.getQueuedTaskCount()*/)) / concurrency());
        //0f; //TODO ...pool.getQueuedTaskCount()

        @Deprecated int n = Math.round(rate * nar.loop.throttle.floatValue() * (1-load) * concurrency());
        if (n == 0)
            return;


        Random rng = ThreadLocalRandom.current();



        nar.what.commit(null);
        nar.how.commit(null);

        ObjectLongHashMap<Pair<How,What>> play = new ObjectLongHashMap(32);

        final int perN = nar.how.size();

        for (int j = 0; j < perN; j++ ) {

            How h = nar.how.sample(rng); if (!h.isOn()) continue; //HACK embed in sample

            for (int i = 0; i < n; i++ ) {

                What w = nar.what.sample(rng); if (!w.isOn()) continue; //HACK embed in sample

                //PlayTask pt = new PlayTask(w, h);
                //pt.fork();

                play.addToValue(pair(h,w), dur(w.pri(), h.pri()));
            }
        }

        MutableList<ObjectLongPair<Pair<How, What>>> l = play.keyValuesView().toSortedList(Comparator.comparingLong(x ->
                ((long)System.identityHashCode(x.getOne().getOne())) << 32 | System.identityHashCode(x.getTwo())
        ));

        if (l.isEmpty())
            return;

        int ll = l.size();
        int para = pool.getParallelism();
        int nPerThread = Math.max(1, ll / para);
        List<ForkJoinTask> t = new FasterList(para);
        for (int i = 0; i < ll; ) {
            int start = i, end = min(ll, i + nPerThread);
            t.add(new PlayBatch(l.subList(start, end).toArray(EMPTY_LONG_PAIR_ARRAY)));
            i += nPerThread;
        }
        t.forEach(ForkJoinTask::fork);


//        if (ThreadLocalRandom.current().nextFloat() < 0.01f)
//            System.out.println(pool);
    }

    static final ObjectLongPair[] EMPTY_LONG_PAIR_ARRAY = new ObjectLongPair[0];


    final static class PlayTask extends RecursiveAction {

        final What w;
        final How h;

        PlayTask(What w, How h) {
            this.w = w;
            this.h = h;
        }

        @Override
        protected void compute() {
            boolean single = h.singleton();
            if (!single || h.busy.compareAndSet(false, true)) {
                try {
                    long dur = dur(w.pri(), h.pri());
                    h.runFor(w, dur);

                } finally {
                    if (single)
                        h.busy.set(false);
                }
            }
        }


    }

    static long dur(float whatPri, float howPri) {
        float pri = (float) Math.sqrt(Util.and(whatPri, howPri)); //sqrt in attempt to compensate for the bag sampling's priority bias
        return Util.lerp(pri, durationMinNS, durationMaxNS);
    }

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
        private final ObjectLongPair[] a;

        public PlayBatch(ObjectLongPair[] a) {
            this.a = a;
        }

        @Override
        protected void compute() {

            ForkJoinPool pool = ForkJoinTask.getPool();

            for (ObjectLongPair<Pair<How, What>> aa : a) {

                aa.getOne().getOne().runFor(aa.getOne().getTwo(), aa.getTwo());

//                if (pool.getQueuedSubmissionCount() > pool.getParallelism()) {
//                    ForkJoinTask.helpQuiesce();
//                }
            }
        }

    }
}
