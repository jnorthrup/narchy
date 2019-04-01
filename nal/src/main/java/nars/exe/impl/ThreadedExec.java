package nars.exe.impl;

import jcog.Util;
import jcog.data.list.FasterList;
import jcog.event.Off;
import jcog.exe.AffinityExecutor;
import jcog.exe.Exe;
import nars.NAR;
import nars.exe.Exec;
import nars.exe.Valuator;

import java.util.concurrent.ForkJoinPool;
import java.util.function.Supplier;

import static java.lang.System.nanoTime;

/** N independent asynchronously looping worker threads */
abstract public class ThreadedExec extends MultiExec {


    final boolean affinity;
    protected int workGranularity = Integer.MAX_VALUE;


    final AffinityExecutor exe;


    public interface Worker extends Runnable, Off {
    }

    public ThreadedExec(Valuator r, int threads) {
        this(r, threads, false);
    }

    public ThreadedExec(Valuator valuator, int maxThreads, boolean affinity) {
        super(valuator, maxThreads);

        this.exe = new AffinityExecutor(maxThreads);
        this.affinity = affinity;


//        if (maxThreads > Runtime.getRuntime().availableProcessors() / 2) {
//            /** absorb system-wide tasks rather than using the default ForkJoin commonPool */
//            Exe.setExecutor(this);
//        }

    }

    @Override protected void executeLater(/*@NotNull */Object x) {

        in.add(x, (xx)->{
            Exec.logger.warn("{} blocked queue on: {}", this, xx);
            executeNow(xx);
        });
    }

    @Override
    protected void update() {

        if (!affinity ) { //HACK should somehow work in affinity mode too
            long ci = this.cycleIdealNS;
            if (ci > 0) {
                int idealThreads = Util.clamp(
                        (int) Math.ceil((nar.loop.throttle.floatValue()) * concurrencyMax()),
                    1,
                        concurrencyMax());

                //TODO fix this
                int currentThreads = concurrency();
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

        workGranularity =
                //concurrency() + 1;
                //Math.max(1, concurrency() - 1);
                concurrency();

        super.update();

    }

    @Override
    public int concurrency() {
        return exe.size();
    }

    protected long work(float responsibility, FasterList buffer) {

        int available = in.size();
        if (available > 0) {
            //do {
            long workStart = nanoTime();

            int batchSize = //Util.lerp(throttle,
                    //available, /* all of it if low throttle. this allows most threads to remains asleep while one awake thread takes care of it all */
                    Math.max(1, (int) Math.floor(((responsibility * available) / workGranularity)));
                    //)

            int got = in.remove(buffer, batchSize);
            if (got > 0)
                execute(buffer, 1, ThreadedExec.this::executeNow);


            long workEnd = nanoTime();
            //} while (!queueSafe());

            return workEnd - workStart;
        }

        return 0;
    }


    @Override
    public void start(NAR n) {

        super.start(n);

        int initialThreads = 1;
        exe.execute(loop(), initialThreads, affinity);

    }



    abstract protected Supplier<Worker> loop();


    @Override
    public void stop() {

        if (Exe.executor() == this) //HACK
            Exe.setExecutor(ForkJoinPool.commonPool()); //TODO use the actual executor replaced by the start() call instead of assuming FJP

        exe.shutdownNow();

        sync();

        super.stop();
    }


    public boolean queueSafe() {
        if (inputQueueSizeSafetyThreshold < 1)
            return in.availablePct(inputQueueCapacityPerThread) >= inputQueueSizeSafetyThreshold;
        else
            return in.isEmpty();
    }
}
