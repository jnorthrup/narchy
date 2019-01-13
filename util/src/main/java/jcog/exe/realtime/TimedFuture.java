package jcog.exe.realtime;

import java.util.concurrent.Delayed;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.TimeUnit;

public interface TimedFuture<T> extends RunnableScheduledFuture<T>, Runnable {

    int rounds();


    /**
     * Reset the Registration
     */
    void reset(long resolution, int wheels);


    Status state();

    /**
     * Get the offset of the Registration relative to the current cursor position
     * to make it fire timely.
     *
     * @return the offset of current Registration
     */
    int offset(long resolution);

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
    T get();

    @Override
    T get(long timeout, TimeUnit unit);

    default void execute(HashedWheelTimer t) {
        t.execute(this);
    }


    enum Status {
        CANCELLED,
        PENDING,
        READY
        
    }

}
