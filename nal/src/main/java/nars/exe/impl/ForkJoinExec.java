package nars.exe.impl;

import jcog.Util;
import jcog.data.list.FasterList;
import nars.attention.What;
import nars.control.How;

import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

/**
 * TODO not finished
 */
public class ForkJoinExec extends MultiExec implements Thread.UncaughtExceptionHandler {

//    private static final int SYNCH_ITERATION_MS = 50;

    private ForkJoinPool pool;

    public ForkJoinExec(int concurrency) {
        super(concurrency);

        //public ForkJoinPool(int parallelism, ForkJoinPool.ForkJoinWorkerThreadFactory factory, UncaughtExceptionHandler handler, boolean asyncMode, int corePoolSize, int maximumPoolSize, int minimumRunnable, Predicate<? super ForkJoinPool> saturate, long keepAliveTime, TimeUnit unit) {
        //int proc = Runtime.getRuntime().availableProcessors();
        int proc = concurrency;
        pool = new ForkJoinPool(
                proc,
                ForkJoinPool.defaultForkJoinWorkerThreadFactory,
//                (p)->{
//                    ForkJoinWorkerThread t = new ForkJoinWorkerThread(p) {
//                        {
//                            setName();
//                        }
//                    };
//                    return t;
//                },
                this,
                true, proc, proc, 0,
                null, 100L, TimeUnit.MILLISECONDS);
        //Exe.setExecutor(pool);
    }


    @Override
    public void synch() {

        logger.info("synch {}", this);
        int i = 0;
        while (pool.getQueuedTaskCount() > 0) {
//            if (Thread.currentThread() instanceof ForkJoinWorkerThread) {
//                try {
//                    ((ForkJoinWorkerThread)Thread.currentThread()).join(SYNCH_ITERATION_MS);
//                } catch (InterruptedException e) {
//                    Util.pauseSpin(i++);
//                }
//            }
            Util.pauseSpin(i++);
            //Thread.yield();

        }
//        if (!pool.isQuiescent()) {
//            do {
//                logger.info("synch {} waiting for quiescence {}", this, summary());
//            } while (pool.awaitQuiescence(5, TimeUnit.SECONDS));
//        logger.info("synch {}", this);
//        }
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
    }


    @Override
    public final int concurrency() {
        return concurrencyMax();
    }

    /**
     * inject play tasks
     */
    private void play() {
        //HACK
        @Deprecated int throttle = 2 * concurrency();
        @Deprecated float ms = 0.25f;
        @Deprecated long durationNS = Math.round(1_000_000.0 * ms);

        FasterList<Runnable> batch = new FasterList();
        for (What w : nar.what) {

            if (!w.isOn())
                continue;

            for (How h : nar.how) {

                if (!h.isOn())
                    continue;

                boolean single = h.singleton();
                if (!single || h.busy.compareAndSet(false, true)) {
                    try {
                        How.Causation t = h.timing();
                        float pri = h.pri();

                        for (int i = 0; i < Math.max(1, pri * throttle); i++) {
                            //                    System.out.println(pri);
                            batch.add(() -> t.runFor(w, durationNS));
                        }

                    } finally {
                        if (single)
                            h.busy.set(false);
                    }
                }
            }
        }
        batch.shuffleThis();
        batch.forEach(pool::execute);

//        if (ThreadLocalRandom.current().nextFloat() < 0.01f)
//            System.out.println(pool);
    }

    //    @Override
//    protected void update() {
//        super.update();
//        System.out.println(summary());
//    }

    protected String summary() {
        return Map.of(
                "pool", pool.toString(),
                "time", nar.time(),
                "pool threads", pool.getActiveThreadCount(),
                "pool tasks pending", pool.getQueuedTaskCount()
        ).toString();
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
