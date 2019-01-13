package jcog.exe.realtime;

import java.util.concurrent.TimeUnit;

/** lighweight one time procedure */
abstract public class Soon extends AbstractTimedFuture<Object> {

    protected Soon(int rounds) {
        super(rounds);
    }

    /** wont need rescheduled, executes immediately */
    @Override
    public int offset(long resolution) {
        return -1;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public final Object get() {
        run();
        return null;
    }

    @Override
    public final Object get(long timeout, TimeUnit unit) {
        run();
        return null;
    }

    /** wraps a Runnable */
    final static class Run extends Soon {

        private final Runnable runnable;

        Run(Runnable runnable) {
            super(0 /* immediately */);
            this.runnable = runnable;
        }

        @Override
        public void run() {
            runnable.run();
        }
    }
}
