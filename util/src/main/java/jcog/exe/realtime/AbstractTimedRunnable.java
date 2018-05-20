package jcog.exe.realtime;

import java.util.concurrent.TimeUnit;

abstract public class AbstractTimedRunnable extends AbstractTimedFuture<Void> {

    private final Runnable run;

    protected AbstractTimedRunnable(int rounds, Runnable run) {
        super(rounds);
        this.run = run;
    }

    @Override
    public final Status state() {
        return rounds-- <= 0 ? Status.READY : Status.PENDING;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public String toString() {
        return run.toString();
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning /* ignored */) {
        throw new UnsupportedOperationException();
    }


    @Override
    public boolean isDone() {
        return rounds < 0;
    }

    @Override
    public void run() {
        run.run();
    }

    @Override
    public Void get() {
        return null;
//        Object r = result;
//        return r == this ? null : (T) r;
    }

    @Override
    public Void get(long timeout, TimeUnit unit) {
        return null;
//        Void r;
//
//        long deadline = System.currentTimeMillis() + unit.toMillis(timeout);
//        while ((r = get()) == null) {
//            Util.sleep(DEFAULT_TIMEOUT_POLL_PERIOD_MS);
//            if (System.currentTimeMillis() >= deadline)
//                break;
//        }
//        return r == this ? null : r;
    }

}

