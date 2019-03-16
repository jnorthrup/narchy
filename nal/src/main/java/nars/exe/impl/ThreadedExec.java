package nars.exe.impl;

import jcog.data.list.FasterList;
import jcog.event.Off;
import jcog.exe.AffinityExecutor;
import jcog.exe.Exe;
import nars.NAR;
import nars.exe.Exec;
import nars.exe.Valuator;

import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Supplier;

import static java.lang.System.nanoTime;

/** N independent asynchronously looping worker threads */
abstract public class ThreadedExec extends MultiExec {


    final int threads;
    final boolean affinity;
    protected int workGranularity;


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
    @Override protected void executeLater(/*@NotNull */Object x) {

        in.add(x, (xx)->{
            Exec.logger.warn("{} blocked queue on: {}", this, xx);
            executeNow(xx);
        });
    }

    @Override
    protected void update() {

        super.update();

        workGranularity =
                //Math.max(1, concurrency() + 1);
                Math.max(1, concurrency() - 1);
    }

    protected long work(float responsibility, FasterList buffer) {

        int available = in.size();
        if (available > 0) {
            //do {
            long workStart = nanoTime();

            int batchSize = //Util.lerp(throttle,
                    //available, /* all of it if low throttle. this allows most threads to remains asleep while one awake thread takes care of it all */
                    (int) Math.ceil(((responsibility * available) / workGranularity))
                    //)
                    ;

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
        if (inputQueueSizeSafetyThreshold < 1)
            return in.availablePct(inputQueueCapacityPerThread) >= inputQueueSizeSafetyThreshold;
        else
            return in.isEmpty();
    }
}
