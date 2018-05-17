package nars.exe;

import jcog.Util;
import jcog.exe.util.RunnableForkJoin;
import nars.NAR;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;

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

        for (int i = 0; i < parallelization; i++) {
            Next n = new Next();
            n.cursor = i; //stagger TODO this isnt great
            execute(n);
        }

    }

    @Override
    public void execute(Runnable async) {
        ForkJoinPool.commonPool().execute(async);
    }

    //    final static AtomicInteger serial = new AtomicInteger(0);

    private class Next extends RunnableForkJoin {


        //        final int id = serial.getAndIncrement();
        private int cursor;

        private final int batchSize = 4;

        public Next() {
            cursor = 0;

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
                if (!(next instanceof Next) && (next instanceof Runnable)) {
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
