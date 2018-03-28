package com.ifesdjeen.timer;

import jcog.Util;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.ifesdjeen.timer.TimedFuture.Status.PENDING;
import static com.ifesdjeen.timer.TimedFuture.Status.READY;

public class OneShotTimedFuture<T> implements TimedFuture<T> {

    static final int GET_TIMEOUT_POLL_PERIOD_MS = 10;

    final AtomicInteger rounds; // rounds is only visible to one thread
    private final Callable<T> callable;
    private final long initialDelay;
    private final int firstFireOffset;
    protected volatile Status status;
    private volatile Object result = null;

    @Deprecated protected OneShotTimedFuture(int rounds, Callable<T> callable, long initialDelay) {
        this(-1, rounds, callable, initialDelay);
    }
    public OneShotTimedFuture(int firstFireOffset, int rounds, Callable<T> callable, long initialDelay) {
        this.firstFireOffset = firstFireOffset;
        this.rounds = new AtomicInteger(rounds);
        this.status = PENDING;
        this.callable = callable;
        this.initialDelay = initialDelay;
    }


    @Override
    public final Status state() {
        Status s = status;
        if (s == PENDING && rounds.getAndDecrement()<=0) {
            return READY;
        }
        return s;
    }

    @Override
    public boolean isCancelled() {
        return status == Status.CANCELLED;
    }
    @Override
    public boolean isDone() {
        return result!=null;
    }

    @Override
    public void decrement() {
        rounds.decrementAndGet();
    }

    @Override
    public void reset() {
        throw new RuntimeException("One Shot Registrations can not be rescheduled");
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning /* ignored */) {
        this.status = Status.CANCELLED;
        return true;
    }


    @Override
    public int getOffset() {
        return firstFireOffset;
    }


    @Override
    public long getDelay(TimeUnit unit) {
        return initialDelay;
    }

    @Override
    public int rounds() {
        return rounds.get();
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
            Util.sleep(GET_TIMEOUT_POLL_PERIOD_MS);
            if (System.currentTimeMillis() >= deadline)
                break;
        }
        return r == this ? null : r;
    }

    @Override
    public boolean isPeriodic() {
        return false;
    }
}
