package nars.exe.impl;

import jcog.Util;
import jcog.data.list.FasterList;
import jcog.data.list.MetalConcurrentQueue;
import jcog.event.Off;
import jcog.exe.AffinityExecutor;
import jcog.exe.Exe;
import nars.NAR;
import nars.exe.Exec;

import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.lang.System.nanoTime;

/** N independent asynchronously looping worker threads */
abstract public class ThreadedExec extends MultiExec {

    static final int inputQueueCapacityPerThread = 256;

    final boolean affinity;
    protected int workGranularity = Integer.MAX_VALUE;
    protected final MetalConcurrentQueue in;



    final AffinityExecutor exe;


    public interface Worker extends Runnable, Off {
    }

    public ThreadedExec(int threads) {
        this(threads, false);
    }

    public ThreadedExec(int maxThreads, boolean affinity) {
        super(maxThreads);

        in = new MetalConcurrentQueue(inputQueueCapacityPerThread * concurrencyMax());

        this.exe = new AffinityExecutor(maxThreads);
        this.affinity = affinity;

    }

    @Override protected void execute(/*@NotNull */Object x) {
        in.add(x, this::executeBlocked);
    }

    private void executeBlocked(Object x) {
        Exec.logger.warn("{} exe queue blocked on: {}", this, x);
        executeNow(x);
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

    public int queueSize() {
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
                logger.info("add {} worker threads (ideal={})", demand, idealThreads);
                synchronized (exe) {
                    exe.execute(loop(), demand, affinity);
                }
            } else if (currentThreads > idealThreads) {
                //stop some
                int excess = currentThreads - idealThreads;
                logger.info("stop {} worker threads (ideal={})", excess, idealThreads);
                synchronized (exe) {
                    while (concurrency() > idealThreads)
                        exe.remove(concurrency() - 1);
                }
            }
        }
    }

    @Override
    public int concurrency() {
        return exe.size();
    }

    protected long work(float responsibility, FasterList buffer) {

        int available;
        boolean kontinue = true;
        long workStart = Long.MIN_VALUE;
        Consumer execNow = this::executeNow;
        while (kontinue && (available = in.size()) > 0) {

            do {

            if (workStart == Long.MIN_VALUE)
                workStart = nanoTime();

            int batchSize = //Util.lerp(throttle,
                    //available, /* all of it if low throttle. this allows most threads to remains asleep while one awake thread takes care of it all */
                    Math.max(1, (int) Math.floor(((responsibility * available) / workGranularity)));
                    //)


            int got = in.remove(buffer, batchSize);
            if (got > 0) {
                execute(buffer, 1, execNow);
                kontinue = !queueSafe();
            } else
                kontinue = false;

            } while (!queueSafe());

        }
        if (workStart!=Long.MIN_VALUE) {
            long workEnd = nanoTime();
            return workEnd - workStart;
        } else {
            return 0; //HACK
        }
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


    boolean queueSafe() {
        if (inputQueueSizeSafetyThreshold < 1)
            return in.availablePct(inputQueueCapacityPerThread) >= inputQueueSizeSafetyThreshold;
        else
            return in.isEmpty();
    }
}
