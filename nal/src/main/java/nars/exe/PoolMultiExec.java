package nars.exe;

import jcog.math.MutableInteger;
import nars.NAR;

/** uses a common forkjoin pool for execution */
public class PoolMultiExec extends AbstractExec {

    private final Revaluator revaluator;

    public final MutableInteger threads = new MutableInteger();
    private Focus focus;

    public PoolMultiExec(Revaluator revaluator, int capacity) {
        this(revaluator, Runtime.getRuntime().availableProcessors()-1, capacity);
    }

    protected PoolMultiExec(Revaluator r, int threads, int capacity) {
        super(capacity);
        this.revaluator = r;
        this.threads.set(threads);
    }

    @Override
    public synchronized void start(NAR nar) {
        this.focus = new Focus(nar, revaluator);
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

        long runUntil = System.currentTimeMillis() + nar.loop.periodMS.intValue();
        int t = threads.intValue();
        for (int i = 0; i < t; i++) {
            execute(()->{
                focus.runDeadline(
                    0.001,
                        () -> (System.currentTimeMillis() <= runUntil),
                        nar.random(), nar);
            });
        }
    }
}
