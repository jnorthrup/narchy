package jcog.exe.realtime;

import java.util.concurrent.TimeUnit;

abstract public class AbstractTimedRunnable extends AbstractTimedFuture<Void> {

    public final Runnable run;

    protected AbstractTimedRunnable() {
        super();
        run = null;
    }

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


    }

    @Override
    public Void get(long timeout, TimeUnit unit) {
        return null;









    }

}

