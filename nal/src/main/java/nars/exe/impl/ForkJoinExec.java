package nars.exe.impl;

import jcog.data.list.FasterList;
import nars.NAR;
import nars.control.How;

import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

/**
 * TODO not finished
 */
public class ForkJoinExec extends MultiExec implements Thread.UncaughtExceptionHandler {

    private ForkJoinPool pool = ForkJoinPool.commonPool(); //intermediate state

    public ForkJoinExec(int concurrency) {
        super(concurrency);
    }

    @Override
    public void start(NAR n) {
        super.start(n);


        //public ForkJoinPool(int parallelism, ForkJoinPool.ForkJoinWorkerThreadFactory factory, UncaughtExceptionHandler handler, boolean asyncMode, int corePoolSize, int maximumPoolSize, int minimumRunnable, Predicate<? super ForkJoinPool> saturate, long keepAliveTime, TimeUnit unit) {
        int proc = Runtime.getRuntime().availableProcessors();
        pool = new ForkJoinPool(
                proc,
                ForkJoinPool.defaultForkJoinWorkerThreadFactory,
                this,
                true, proc, proc + 1, 0,
                null, 10000L, TimeUnit.MILLISECONDS);

        //Exe.setExecutor(pool);

//        ForkJoinPool prevPool = this.pool;
//        prevPool.awaitQuiescence(1, TimeUnit.SECONDS);

//        pool.submit(()->{
//
//        });
        //ForkJoinPool.commonPool();

//        new Thread(()->{
//            Util.sleepMS(50000);
//        }).start();

        //super.start(n);
    }

    @Override
    public void synch() {
//        System.out.println(pool.toString());
        pool.awaitQuiescence(10, TimeUnit.SECONDS);
    }

    @Override
    public void stop() {
        pool.shutdownNow();
        super.stop();
    }


    @Override
    protected void update() {
        super.update();

        play();

    }


    /**
     * inject play tasks
     */
    private void play() {
        //HACK
        int throttle = 6 * concurrency();

        FasterList<Runnable> batch = new FasterList();
        nar.control.how.forEach(can -> {

            if (can.sleeping())
                return; //HACK

            How.Causation t = can.timing();
            float pri = can.pri();

            for (int i = 0; i < Math.max(1, pri * throttle); i++) {
                float ms = 0.35f;
                long durationNS = Math.round(1_000_000.0 * ms);
//                    System.out.println(pri);
                batch.add(() -> t.runFor(durationNS));
            }
        });
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
        pool.execute(x instanceof Runnable ? ((Runnable) x) : () -> {
            executeNow(x);
        }); //HACK
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        throwable.printStackTrace(); //TODO
    }
}
