package nars.exe.impl;

import nars.NAR;

import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

/** TODO not finished */
public class ForkJoinExec extends MultiExec implements Thread.UncaughtExceptionHandler {

    private ForkJoinPool pool = ForkJoinPool.commonPool(); //intermediate state

    public ForkJoinExec(int concurrency) {
        super(concurrency);
    }

    @Override
    public void start(NAR n) {
        super.start(n);

        ForkJoinPool prevPool = this.pool;

        //public ForkJoinPool(int parallelism, ForkJoinPool.ForkJoinWorkerThreadFactory factory, UncaughtExceptionHandler handler, boolean asyncMode, int corePoolSize, int maximumPoolSize, int minimumRunnable, Predicate<? super ForkJoinPool> saturate, long keepAliveTime, TimeUnit unit) {
        int proc = Runtime.getRuntime().availableProcessors();
        pool = new ForkJoinPool(
                proc,
                ForkJoinPool.defaultForkJoinWorkerThreadFactory,
                this,
                true, proc, proc /* ?? */, 0,
                null, 60000L, TimeUnit.MILLISECONDS);

        //Exe.setExecutor(pool);

        prevPool.awaitQuiescence(1, TimeUnit.SECONDS);

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

        //inject play

        //HACK
        int p = 6;
        for (int i = 0; i < p; i++) {
            nar.control.active.forEach(x -> {
                execute(() -> {
                    float ms = 0.5f;
                    long durationNS = Math.round(1_000_000.0 * ms);
                    long start = System.nanoTime();
                    long deadline = start + durationNS;
                    x.next(nar, () -> System.nanoTime() < deadline);
                });
            });
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
        return Map.of(
            "pool", pool.toString(),
            "time", nar.time(),
            "pool threads", pool.getActiveThreadCount(),
            "pool tasks pending", pool.getQueuedTaskCount()
        ).toString();
    }

    @Override
    protected void execute(Object x) {
        pool.execute(x instanceof Runnable ? ((Runnable)x) : ()->{
            executeNow(x);
        }); //HACK
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        throwable.printStackTrace(); //TODO
    }
}
