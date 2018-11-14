package jcog.exe.realtime;

import jcog.Util;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

abstract public class AbstractTimedCallable<T> extends AbstractTimedFuture<T> {
    static final int DEFAULT_TIMEOUT_POLL_PERIOD_MS = 10;

    private final Callable<T> callable;
    private volatile Object result = null;
    protected /*volatile*/ Status status = Status.PENDING;

    protected AbstractTimedCallable(int rounds, Callable<T> callable) {
        super(rounds);
        this.callable = callable;
    }

    @Override
    public final Status state() {
        Status s = status;
        if (s == Status.PENDING && rounds--<=0) {
            return Status.READY;
        }
        return s;
    }

    @Override
    public boolean isCancelled() {
        return status == Status.CANCELLED;
    }


    @Override
    public boolean cancel(boolean mayInterruptIfRunning /* ignored */) {
        this.status = Status.CANCELLED;
        return true;
    }


    @Override
    public boolean isDone() {
        return result!=null;
    }

    @Override
    public void run() {
        try {
            Object r = callable.call();
            if (r == null)
                r = this;
            this.result = r;
        } catch (Exception e) {
            result = e;
        }
    }

    @Override
    public T get() {
        Object r = result;
        return r == this ? null : (T) r;
    }

    @Override
    public T get(long timeout, TimeUnit unit) {
        T r;

        long deadline = System.currentTimeMillis() + unit.toMillis(timeout);
        while ((r = get()) == null) {
            Util.sleepMS(DEFAULT_TIMEOUT_POLL_PERIOD_MS);
            if (System.currentTimeMillis() >= deadline)
                break;
        }
        return r == this ? null : r;
    }

}
