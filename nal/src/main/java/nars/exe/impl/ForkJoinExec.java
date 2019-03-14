package nars.exe.impl;

import jcog.Util;
import nars.NAR;
import nars.exe.Valuator;

import java.util.Map;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

public class ForkJoinExec extends MultiExec  {

    private ForkJoinPool pool = ForkJoinPool.commonPool(); //temporary setting

    public ForkJoinExec(Valuator valuator, int concurrency) {
        super(valuator, concurrency);



//        new Thread(()->{
//            Util.sleepMS(5000);
//        }).start();
    }

    @Override
    public void start(NAR n) {
        //public ForkJoinPool(int parallelism, ForkJoinPool.ForkJoinWorkerThreadFactory factory, UncaughtExceptionHandler handler, boolean asyncMode, int corePoolSize, int maximumPoolSize, int minimumRunnable, Predicate<? super ForkJoinPool> saturate, long keepAliveTime, TimeUnit unit) {
        pool = new ForkJoinPool(
                Runtime.getRuntime().availableProcessors(),
                ForkJoinPool.defaultForkJoinWorkerThreadFactory,
                (Thread.UncaughtExceptionHandler)null,
                false, 0, 32767, 0,
                null, 60000L, TimeUnit.MILLISECONDS);


                //ForkJoinPool.commonPool();

        new Thread(()->{
            Util.sleepMS(50000);
        }).start();

        super.start(n);
    }

    @Override
    public void stop() {
        pool.shutdownNow();
        super.stop();
    }

    @Override
    protected void onCycle(NAR nar) {
        super.onCycle(nar);
        pool.execute(()->{
           //TODO invoke causables
        });
        pool.awaitQuiescence(1, TimeUnit.SECONDS);
    }

    @Override
    protected void update() {
        super.update();
        System.out.println(summary());
    }

    protected String summary() {
        return Map.of(
            "time", nar.time(),
            "in size", in.size(),
            "pool threads", pool.getActiveThreadCount(),
            "pool tasks pending", pool.getQueuedTaskCount()
        ).toString();
    }

    @Override
    protected void executeLater(Object x) {
        pool.execute(x instanceof Runnable ? ((Runnable)x) : ()->{
            executeNow(x);
        }); //HACK
    }
}
