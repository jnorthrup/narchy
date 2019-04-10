package nars.exe.impl;

import jcog.Util;
import nars.attention.What;
import nars.control.How;

import java.util.Random;
import java.util.concurrent.*;

/**
 * TODO not finished
 */
public class ForkJoinExec extends MultiExec implements Thread.UncaughtExceptionHandler {

    private static final int SYNCH_ITERATION_MS = 50;

    private ForkJoinPool pool;

    public ForkJoinExec(int concurrency) {
        super(concurrency);

        //public ForkJoinPool(int parallelism, ForkJoinPool.ForkJoinWorkerThreadFactory factory, UncaughtExceptionHandler handler, boolean asyncMode, int corePoolSize, int maximumPoolSize, int minimumRunnable, Predicate<? super ForkJoinPool> saturate, long keepAliveTime, TimeUnit unit) {
        //int proc = Runtime.getRuntime().availableProcessors();
        int proc = concurrency;
        pool = new ForkJoinPool(
                proc,
                //orkJoinPool.defaultForkJoinWorkerThreadFactory,
                (p)->{
                    ForkJoinWorkerThread t = new ForkJoinWorkerThread(p) {
                        {
                        }
                    };
                    return t;
                },
                this,
                true, 0, proc*2, 1,
                null, 60L, TimeUnit.SECONDS);
        //Exe.setExecutor(pool);
    }


    @Override
    public void synch() {
        while (!pool.isQuiescent()) {
            logger.info("synch {}", this);
            pool.awaitQuiescence(SYNCH_ITERATION_MS, TimeUnit.MILLISECONDS);
        }
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

        logger.info(summary());
    }


    @Override
    public final int concurrency() {
        return pool.getParallelism();
    }

    class Play extends RecursiveAction {

        @Override
        protected void compute() {

        }
    }
    /**
     * inject play tasks
     */
    private void play() {

        float rate = 8; //HACK
        float load =
                0f; //TODO ...pool.getQueuedTaskCount()

        @Deprecated int n = Math.round(rate * nar.loop.throttle.floatValue() * (1-load) * concurrency());
        if (n == 0)
            return;

        long durationMinNS = TimeUnit.MICROSECONDS.toNanos(200);
        long durationMaxNS = TimeUnit.MICROSECONDS.toNanos(1000);

        Random rng = ThreadLocalRandom.current();

        while (n > 0) {

            What w = nar.what.sample(rng); if (!w.isOn()) continue; //HACK embed in sample

            for (int i = 0; i < n; i++ ) {

                How h = nar.how.sample(rng); if (!h.isOn()) continue; //HACK embed in sample

                float pri = (float) Math.sqrt(Util.and(w.pri(), h.pri())); //sqrt in attempt to compensate for the bag sampling's priority bias
                long dur = Util.lerp(pri, durationMinNS, durationMaxNS);

                boolean single = h.singleton();

                pool.invoke(ForkJoinTask.adapt(() -> {
                    if (!single || h.busy.compareAndSet(false, true)) {
                        try {

                            h.runFor(w, dur);

                        } finally {
                            if (single)
                                h.busy.set(false);
                        }
                    }
                }));
            }
        }




//        if (ThreadLocalRandom.current().nextFloat() < 0.01f)
//            System.out.println(pool);
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
            pool.execute(() -> executeNow(x));
//        }
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        throwable.printStackTrace(); //TODO
    }
}
