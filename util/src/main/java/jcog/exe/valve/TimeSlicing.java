package jcog.exe.valve;

import com.conversantmedia.util.concurrent.MultithreadConcurrentQueue;
import jcog.TODO;
import jcog.data.list.MetalConcurrentQueue;
import jcog.exe.util.RunnableForkJoin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;

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

    public static final int workQueueCapacity = 64;
    public final AtomicLong cycleTimeNS = new AtomicLong(/* 10hz default: = 100ms = */ 100 * 1000 * 1000);

    final MultithreadConcurrentQueue<InstrumentedWork> work;
    public final MetalConcurrentQueue<Runnable> async;


    private final Semaphore concurrency;
    private final Executor exe;

    public TimeSlicing(What what, int concurrency, Executor exe) {
        super(what);
        this.concurrency = new Semaphore(concurrency, false);
        this.exe = exe;
        this.work = new MultithreadConcurrentQueue<>(workQueueCapacity);
        this.async = new MetalConcurrentQueue<>(workQueueCapacity);
    }



    @Override
    public void onAdd(InstrumentedWork<Who, What> iw) {
        queue(iw);
        trySpawn();
    }

    protected void trySpawn() {
        if (concurrency.tryAcquire()) {
            if (exe instanceof ForkJoinPool) {
                ((ForkJoinPool) exe).execute((ForkJoinTask) new ForkJoinWorker());
            } else {
                exe.execute(new RunnableWorker());
            }
        }
    }

    final public void queue(Runnable r) {
        if (!async.offer(r)) {
            //throw new TODO("overflow; use blocking queue or otherwise handle this queue overflow");
            r.run();
        } else {
            trySpawn();
        }
    }

    final private void queue(InstrumentedWork iw) {
        if (!work.offer(iw)) {
            throw new TODO("overflow; use blocking queue or otherwise handle this queue overflow");
        }
    }

    @Override
    public void onRemove(InstrumentedWork<Who, What> value) {

    }


    public void stop() {
        cycleTimeNS.set(-1);
    }

    private static final Logger logger = LoggerFactory.getLogger(TimeSlicing.class);

    protected boolean work() {



        Runnable w = null;
        try {
            while ((w = async.poll()) != null) {
                try {
                    w.run();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Throwable t) {
            logger.error("{} {}", w,t);
        }


        long cycleNS = cycleTimeNS.longValue();
        if (cycleNS < 0) {
            return false;
        } else {

            InstrumentedWork x = work.poll();

            if (x == null) {
                concurrency.release();
                return false;
            } else {

                try {

                    x.runFor(cycleNS);

                } catch (Throwable t) {
                    logger.error("{} {}", x,t);
                } finally {

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
//            if (work()) {
//                exe.execute(this); //re-invoke
//            }

            while (work());
        }
    }
}
