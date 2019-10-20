package jcog.exe.realtime;

import java.util.concurrent.Delayed;
import java.util.concurrent.RunnableScheduledFuture;
import java.util.concurrent.TimeUnit;

public interface TimedFuture<T> extends RunnableScheduledFuture<T>, Runnable {

    int rounds();


    int CANCELLED = -1;
    int PENDING = 0;
    int READY = 1;
    int state();

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
        if (other == this) return 0;

        long r1 = rounds();
        long r2 = other.rounds();
        if (r1 == r2) {
            return Integer.compare(System.identityHashCode(this), System.identityHashCode(other));
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


}
