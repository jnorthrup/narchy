package jcog.exe.realtime;

import java.util.concurrent.*;

public interface TimedFuture<T> extends RunnableScheduledFuture<T>, Runnable {

    int rounds();

    /**
     * Decrement an amount of runs Registration has to run until it's elapsed
     */
    void decrement();

    /**
     * Reset the Registration
     */
    void reset();


    Status state();

    /**
     * Get the offset of the Registration relative to the current cursor position
     * to make it fire timely.
     *
     * @return the offset of current Registration
     */
    int getOffset();

    long getDelay(TimeUnit unit);

    @Override
    default int compareTo(Delayed o) {
        TimedFuture other = (TimedFuture) o;
        long r1 = rounds();
        long r2 = other.rounds();
        if (r1 == r2) {
            return other == this ? 0 : -1;
        } else {
            return Long.compare(r1, r2);
        }
    }

    @Override
    T get() throws InterruptedException, ExecutionException;

    @Override
    T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException;

    default void execute(HashedWheelTimer t) {
        t.execute(this);
    }


    enum Status {
        CANCELLED,
        PENDING,
        READY
        // COMPLETED ??
    }

}
