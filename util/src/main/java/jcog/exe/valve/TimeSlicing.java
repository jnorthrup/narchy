package jcog.exe.valve;

import com.conversantmedia.util.concurrent.MultithreadConcurrentQueue;
import jcog.TODO;
import jcog.exe.util.RunnableForkJoin;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RunnableFuture;
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
public class TimeSlicing<Who, What> extends Valve.Mix<Who, What> {

    public final AtomicLong cycleTimeNS = new AtomicLong(/* 10hz default: = 100ms = */ 100 * 1000 * 1000);

    final MultithreadConcurrentQueue<InstrumentedWork> pending = new MultithreadConcurrentQueue<>(512);
    //final MultithreadConcurrentQueue<InstrumentedWork> running = new MultithreadConcurrentQueue<>(512);

    private final Semaphore concurrency;

    public TimeSlicing(What what, int concurrency) {
        super(what);
        this.concurrency = new Semaphore(concurrency, false);
    }

    //TODO update fps rate: setFPS

    @Override
    public Share<Who, What> put(Share<Who, What> s) {
        if (!(s instanceof Work)) {
            throw new UnsupportedOperationException(s + " must implement Work");
        }
        return super.put(s);
    }

    @Override
    public void onAdd(Share<Who, What> s) {
        Work w = ((Work) s);
        InstrumentedWork iw = new InstrumentedWork(w, s);

        queue(iw);

        //will spawn up to concurrency then no more will be created
        if (concurrency.tryAcquire()) {
            spawn();
        }
    }

    protected void queue(InstrumentedWork iw) {
        if (!pending.offer(iw)) {
            throw new TODO("use blocking queue or otherwise handle this queue overflow");
        }
    }

    protected void spawn() {
        //TODO make this impl by different ways: dedicated worker threads, or re-entrant ForkJoinPool submissions
        ForkJoinPool.commonPool().execute((RunnableFuture)new Worker());
    }

    @Override
    public void onRemove(Share<Who, What> value) {
        //TODO remove associated InstrumentedWork from queue
    }

    @Override
    public Valve.Mix<Who, What> commit() {
        //TODO wake sleeping queue items by moving them to the pending queue
        return super.commit();
    }

    public void stop() {
        cycleTimeNS.set(-1);
    }

    private class Worker extends RunnableForkJoin {

        public Worker() {

        }

        @Override
        public boolean exec() {

            long cycleNS = cycleTimeNS.longValue();
            if (cycleNS < 0) {
                return true; //TimeSlicing has been stopped
            }

            InstrumentedWork x = pending.poll();

            if (x == null) {
                concurrency.release();
                return true; //DONE
            } else {

                try {

                    if (x.start()) {

                        float p = x.share.pri();
                        if (p == p) {

                            long runtimeNS = Math.round(cycleNS * p);

                            long deadlineNS = nanoTime() + runtimeNS;

                            while (x.next() && nanoTime() < deadlineNS) ;

                        }

                    } else {
                        //TODO move to sleep queue
                    }


                } finally {
                    x.stop();
                }

                queue(x);

                fork();

                return false;
            }
        }
    }
}
