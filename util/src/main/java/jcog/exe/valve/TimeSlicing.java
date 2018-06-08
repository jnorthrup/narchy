package jcog.exe.valve;

import com.conversantmedia.util.concurrent.MultithreadConcurrentQueue;
import jcog.TODO;
import jcog.exe.util.RunnableForkJoin;

import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.System.nanoTime;

/**
 * spawns any number of independently asynchronous worker threads that
 * progressively iterate (ex: round-robin, or random) the active customers
 * and budgeting to them a slice of a time cycle into fair
 * shares according to each's allocated supply.
 * <p>
 * configurable ideal min/max period constraints.
 * collects instrumentation metrics via progressive Iterator callback interface.
 * <p>
 * TODO abstract for different execution impl's
 */
public class TimeSlicing<Who, What> extends Mix<Who, What, InstrumentedWork<Who, What>> {

    public final AtomicLong cycleTimeNS = new AtomicLong(/* 10hz default: = 100ms = */ 100 * 1000 * 1000);

    final MultithreadConcurrentQueue<InstrumentedWork> pending = new MultithreadConcurrentQueue<>(512);
    public final MultithreadConcurrentQueue<Runnable> work = new MultithreadConcurrentQueue<>(512);


    private final Semaphore concurrency;
    private final Executor exe;

    public TimeSlicing(What what, int concurrency, Executor exe) {
        super(what);
        this.concurrency = new Semaphore(concurrency, false);
        this.exe = exe;
    }



    @Override
    public void onAdd(InstrumentedWork<Who, What> iw) {
        queue(iw);
        trySpawn();
    }

    void trySpawn() {
        if (concurrency.tryAcquire())
            spawn();
    }

    public void queue(Runnable r) {
        if (!work.offer(r)) {
            throw new TODO("overflow; use blocking queue or otherwise handle this queue overflow");
        }
        trySpawn();
    }

    protected void queue(InstrumentedWork iw) {
        if (!pending.offer(iw)) {
            throw new TODO("overflow; use blocking queue or otherwise handle this queue overflow");
        }
    }

    protected void spawn() {
        if (exe instanceof ForkJoinPool) {
            ((ForkJoinPool)exe).execute((ForkJoinTask) new ForkJoinWorker());
        } else {
            exe.execute(new RunnableWorker());
        }
    }

    @Override
    public void onRemove(InstrumentedWork<Who, What> value) {

    }


    public void stop() {
        cycleTimeNS.set(-1);
    }

    protected boolean work() {

        Runnable w;
        while ((w = work.poll())!=null) {
            try {
                w.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        long cycleNS = cycleTimeNS.longValue();
        if (cycleNS < 0) {
            return false;
        } else {

            InstrumentedWork x = pending.poll();

            if (x == null) {
                concurrency.release();
                return false;
            } else {

                try {

                    if (x.start()) {

                        float p = x.pri();
                        if (p == p) {

                            long runtimeNS = Math.round(cycleNS * p);

                            long deadlineNS = nanoTime() + runtimeNS;

                            while (x.next() && nanoTime() < deadlineNS) ;

                        }

                    } else {

                    }


                } finally {
                    x.stop();
                    queue(x);
                }



                return true;
            }
        }

    }



    private class ForkJoinWorker extends RunnableForkJoin {

        public ForkJoinWorker() {

        }

        @Override
        public boolean exec() {
            if (work()) {
                fork();
                return false;
            } else {
                return true;
            }

        }
    }

    private class RunnableWorker implements Runnable {

        public RunnableWorker() {

        }

        @Override
        public void run() {
            if (work()) {
                exe.execute(this); //re-invoke
            }
        }
    }
}
