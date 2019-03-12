package nars.exe.impl;

import jcog.event.Off;
import jcog.exe.AffinityExecutor;
import jcog.exe.Exe;
import nars.NAR;
import nars.exe.Valuator;

import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Supplier;

/** N independent asynchronously looping worker threads */
abstract public class ThreadedExec extends MultiExec {


    final int threads;
    final boolean affinity;

    double granularity = 4;

    final AffinityExecutor exe = new AffinityExecutor();
    private List<Worker> workers;

    public interface Worker extends Runnable, Off {
    }

    public ThreadedExec(Valuator r, int threads) {
        this(r, threads, false);
    }

    public ThreadedExec(Valuator valuator, int threads, boolean affinity) {
        super(valuator, threads);
        this.threads = threads;
        this.affinity = affinity;
    }

    @Override
    public void start(NAR n) {

        int procs = Runtime.getRuntime().availableProcessors();

        super.start(n);

        workers = exe.execute(loop(), threads, affinity);

        if (concurrency() > procs / 2) {
            /** absorb system-wide tasks rather than using the default ForkJoin commonPool */
            Exe.setExecutor(this);
        }

    }



    abstract protected Supplier<Worker> loop();


    @Override
    public void stop() {
        Exe.setExecutor(ForkJoinPool.commonPool()); //TODO use the actual executor replaced by the start() call instead of assuming FJP

        workers.forEach(Worker::off);
        workers.clear();

        exe.shutdownNow();

        sync();

        super.stop();
    }


    public boolean queueSafe() {
        return in.availablePct(inputQueueCapacityPerThread) >= inputQueueSizeSafetyThreshold;
    }
}
