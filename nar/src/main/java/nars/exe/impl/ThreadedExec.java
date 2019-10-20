package nars.exe.impl;

import jcog.TODO;
import jcog.Util;
import jcog.event.Off;
import jcog.exe.AffinityExecutor;
import jcog.exe.Exe;
import nars.NAR;
import org.jctools.queues.MpmcArrayQueue;

import java.util.concurrent.ForkJoinPool;
import java.util.function.Supplier;

/**
 * N independent asynchronously looping worker threads
 */
public abstract class ThreadedExec extends MultiExec {

    static final int inputQueueCapacityPerThread = 512;

    protected final MpmcArrayQueue in;

    final boolean affinity;
    final AffinityExecutor exe;

    /** cached concurrency (thread count) */
    private transient int concurrency = 1;


    public ThreadedExec(int threads) {
        this(threads, false);
    }

    public ThreadedExec(int maxThreads, boolean affinity) {
        super(maxThreads);

        in = new MpmcArrayQueue(inputQueueCapacityPerThread * concurrencyMax);

        this.exe = new AffinityExecutor(maxThreads);
        this.affinity = affinity;

    }

    @Override
    protected final void execute(/*@NotNull */Object x) {
        if (!in.offer(x))
            executeJammed(x);
    }

    private void executeJammed(Object x) {

//        //experimental: help drain queue
//        Object helping = in.poll();
//        if (helping!=null) {
//            logger.error("{} queue jam help={}", this, helping);
//            executeNow(helping);
//        }

        //if (!in.offer(x)) { //try again
            logger.error("{} queue blocked offer={}", this, x);
            //TODO print queue contents, but only from one thread and not more than every N seconds
            executeNow(x); //else: execute (may deadlock)
        //}
    }


    @Override
    protected void update() {

        _concurrency();

        //HACK should somehow work in affinity mode too
        if (!affinity) {
            //long ci = this.cycleIdealNS;
            //if (ci > 0)
            {
                var idealThreads = Util.clamp(
                    (int) Math.ceil((nar.loop.throttle.floatValue()) * concurrencyMax),
                    1,
                    concurrencyMax);

                //TODO fix this
                var concurrency = concurrency();
                if (idealThreads > concurrency) {
                    //spawn more
                    int demand;
                    synchronized (exe) {
                        demand = idealThreads - _concurrency();
                        if (demand > 0) {
                            exe.execute(loop(), demand, affinity);
                            _concurrency();
                        }
                    }
                    logger.info("start +{} worker threads (ideal: {} )", demand, idealThreads);
                } else if (concurrency > idealThreads) {
                    //stop some
                    int excess;
                    synchronized (exe) {
                        excess = concurrency() - idealThreads;
                        int c;
                        while ((c = _concurrency()) > idealThreads)
                            exe.remove(c - 1);
                    }
                    logger.info("stop {} worker threads (ideal: {} )", excess, idealThreads);
                }
            }
        } else
            throw new TODO();


        super.update();

    }

    private int _concurrency() {
        return concurrency = exe.size();
    }

    public final int queueSize() {
        return in.size();
    }

    void flush() {
        Object next;
        while ((next = in.poll()) != null) executeNow(next);
    }

    @Override
    public final int concurrency() {
        return concurrency;
    }


    @Override
    public void starting(NAR n) {

        super.starting(n);

        var initialThreads = 1;
        exe.execute(loop(), initialThreads, affinity);

    }

    protected abstract Supplier<Worker> loop();

    @Override
    public boolean delete() {
        if (super.delete()) {

            if (Exe.executor() == this) //HACK
                Exe.setExecutor(ForkJoinPool.commonPool()); //TODO use the actual executor replaced by the start() call instead of assuming FJP

            exe.shutdownNow();

            flush();

            return true;
        }
        return false;
    }


//    boolean queueSafe(int size) {
//        return size > 0 && MetalConcurrentQueue.availablePct(size, inputQueueCapacityPerThread) >= alertness.floatValue();
//    }


    public interface Worker extends Runnable, Off {
    }
}
