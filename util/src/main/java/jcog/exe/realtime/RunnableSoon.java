package jcog.exe.realtime;

import java.util.concurrent.TimeUnit;

/** lighweight one time Runnable */
abstract public class RunnableSoon extends AbstractTimedFuture<Object> {

    protected RunnableSoon(int rounds) {
        super(rounds);
    }

    /** wont need rescheduled */
    @Override
    public int getOffset(long resolution) {
        return 0;
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
    final static class Wrapper extends RunnableSoon {

        private final Runnable runnable;

        Wrapper(int firstFireRounds, Runnable runnable) {
            super(firstFireRounds);
            this.runnable = runnable;
        }

        @Override
        public void run() {
            runnable.run();
        }
    }
}
