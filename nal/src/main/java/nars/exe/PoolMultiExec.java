package nars.exe;

import jcog.math.MutableInteger;
import nars.NAR;

import java.util.concurrent.Semaphore;

/**
 * uses a common forkjoin pool for execution
 */
public class PoolMultiExec extends AbstractExec {

    private final Revaluator revaluator;

    public final MutableInteger threads = new MutableInteger();
    private Focus focus;
    Semaphore ready;

    public PoolMultiExec(Revaluator revaluator, int capacity) {
        this(revaluator, Runtime.getRuntime().availableProcessors() - 1, capacity);
    }

    protected PoolMultiExec(Revaluator r, int threads, int capacity) {
        super(capacity);
        this.revaluator = r;
        this.threads.set(threads);
    }

    @Override
    public synchronized void start(NAR nar) {
        this.focus = new Focus(nar, revaluator);
        this.ready = new Semaphore(threads.intValue());
        super.start(nar);
    }

    @Override
    public synchronized void stop() {
        super.stop();
        focus = null;
    }

    @Override
    public boolean concurrent() {
        return true;
    }


    @Override
    public void cycle() {
        super.cycle();

        int t = threads.intValue();

        int loopTime = nar.loop.periodMS.intValue();
        int dutyTime = Math.round(nar.loop.throttle.floatValue() * loopTime);
        if (dutyTime > 0) {
            long runUntil = System.currentTimeMillis() + dutyTime;
            double jiffy = nar.loop.jiffy.floatValue();
            while (ready.tryAcquire()) {
                execute(() -> {
                    try {
                        focus.runDeadline(
                                jiffy * dutyTime/1000.0,
                                () -> (System.currentTimeMillis() <= runUntil),
                                nar.random(), nar);
                    } finally {
                        ready.release();
                    }
                });
            }

        }
    }
}
