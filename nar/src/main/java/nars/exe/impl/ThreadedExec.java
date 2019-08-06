package nars.exe.impl;

import jcog.Util;
import jcog.data.list.MetalConcurrentQueue;
import jcog.event.Off;
import jcog.exe.AffinityExecutor;
import jcog.exe.Exe;
import nars.NAR;
import org.jctools.queues.atomic.MpmcAtomicArrayQueue;

import java.util.concurrent.ForkJoinPool;
import java.util.function.Supplier;

/**
 * N independent asynchronously looping worker threads
 */
abstract public class ThreadedExec extends MultiExec {

    static final int inputQueueCapacityPerThread = 512;

    protected final MpmcAtomicArrayQueue in;

    final boolean affinity;
    final AffinityExecutor exe;
    protected int workGranularity = Integer.MAX_VALUE;


    public ThreadedExec(int threads) {
        this(threads, false);
    }

    public ThreadedExec(int maxThreads, boolean affinity) {
        super(maxThreads);

        in = new MpmcAtomicArrayQueue(inputQueueCapacityPerThread * concurrencyMax());

        this.exe = new AffinityExecutor(maxThreads);
        this.affinity = affinity;

    }

    @Override
    protected final void execute(/*@NotNull */Object x) {
        if (!in.offer(x))
            executeJammed(x);
    }

    private void executeJammed(Object x) {

        //experimental: help drain queue
        Object helping = in.poll();
        if (helping!=null) {
            logger.error("{} queue jam help={}", this, helping);
            executeNow(helping);
        }

        if (!in.offer(x)) { //try again
            logger.error("{} queue blocked offer={}", this, x);
            //TODO print queue contents, but only from one thread and not more than every N seconds
            executeNow(x); //else: execute (may deadlock)
        }
    }

    @Override
    protected void update() {

        int concurrency = concurrency();

        updateThreads(concurrency);

        workGranularity =
                //concurrency() + 1;
                //Math.max(1, concurrency() - 1);
                concurrency;

        super.update();

    }

    public final int queueSize() {
        return in.size();
    }

    void flush() {
        Object next;
        while ((next = in.poll()) != null) executeNow(next);
    }

    private void updateThreads(int currentThreads) {
        if (affinity)
            return;  //HACK should somehow work in affinity mode too

        long ci = this.cycleIdealNS;
        if (ci > 0) {
            int idealThreads = Util.clamp(
                    (int) Math.ceil((nar.loop.throttle.floatValue()) * concurrencyMax()),
                    1,
                    concurrencyMax());

            //TODO fix this
            if (idealThreads > currentThreads) {
                //spawn more
                int demand = idealThreads - currentThreads;
                logger.info("add {} worker threads (ideal: {} )", demand, idealThreads);
                synchronized (exe) {
                    exe.execute(loop(), demand, false);
                }
            } else if (currentThreads > idealThreads) {
                //stop some
                int excess = currentThreads - idealThreads;
                logger.info("stop {} worker threads (ideal: {} )", excess, idealThreads);
                synchronized (exe) {
                    int c;
                    while ((c = concurrency()) > idealThreads)
                        exe.remove(c - 1);
                }
            }
        }
    }

    @Override
    public final int concurrency() {
        return exe.size();
    }


    @Override
    public void starting(NAR n) {

        super.starting(n);

        int initialThreads = 1;
        exe.execute(loop(), initialThreads, affinity);

    }

    abstract protected Supplier<Worker> loop();

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


    boolean queueSafe(int size) {
        return size > 0 && MetalConcurrentQueue.availablePct(size, inputQueueCapacityPerThread) >= alertness.floatValue();
    }


    public interface Worker extends Runnable, Off {
    }
}
