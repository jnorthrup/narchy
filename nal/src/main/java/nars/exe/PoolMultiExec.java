package nars.exe;

import jcog.Util;
import nars.NAR;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RunnableFuture;

/**
 * uses global ForkJoinPool for scheduling execution
 */
public class PoolMultiExec extends AbstractExec {

    private final Revaluator revaluator;
    private final int parallelization;
    private Focus focus;

    public PoolMultiExec(Revaluator revaluator, int capacity) {
        this(Util.concurrencyDefault(), capacity, revaluator);
    }

    public PoolMultiExec(int parallelization, int capacity, Revaluator revaluator) {
        super(capacity);

        this.parallelization = parallelization;
        this.revaluator = revaluator;
    }

    @Override
    public boolean concurrent() {
        return true;
    }

    @Override
    public void start(NAR nar) {
        synchronized (this) {
            super.start(nar);
            this.focus = new Focus(nar, revaluator);
        }

        for (int i = 0; i < parallelization; i++)
            new Next();

    }

    @Override
    public void execute(Runnable async) {
        ForkJoinPool.commonPool().execute(async);
    }

    /**
     * from: ForkJoinPool.java
     */
    abstract static class RunnableForkJoin extends ForkJoinTask<Void> implements RunnableFuture<Void> {
        public final Void getRawResult() {
            return null;
        }

        public final void setRawResult(Void v) {
        }

        abstract public boolean exec();

        public final void run() {
            invoke();
        }
    }

//    final static AtomicInteger serial = new AtomicInteger(0);

    private class Next extends RunnableForkJoin {


        //        final int id = serial.getAndIncrement();
        private int cursor;

        private final int batchSize = 4;

        public Next() {
            cursor = 0;

            execute(this); //start
        }

        @Override
        public boolean exec() {
            try {
                work();

            } catch (Throwable t) {
                t.printStackTrace();
            } finally {

                try {

                    play();

                } catch (Throwable t) { t.printStackTrace(); } finally {

                    fork();
                }
            }
            return false;
        }

        public void play() {
            int[] i = focus.sliceIters;
            int n = i.length;
            if (n == 0)
                return;

            int remain = batchSize;
            for (int a = 0; a < n; a++) { //safety limit
                if (++cursor >= n)
                    cursor = 0;
                if (focus.tryRun(cursor)) {
                    if (--remain <= 0)
                        break; //done
                }
            }
        }

        public void work() {

//            if (getSurplusQueuedTaskCount()>0) {
//                ForkJoinTask.helpQuiesce();
//            }

            ForkJoinTask<?> next;
            while ((next = peekNextLocalTask()) != null) {
                if (!(next instanceof Next)) {
                    pollNextLocalTask();
//                        try {
                    ((Runnable)next).run();
//                        } catch (InterruptedException | ExecutionException e) {
//                            e.printStackTrace();
//                        }

                } else {
                    return;
                }

            }
        }
    }
}
