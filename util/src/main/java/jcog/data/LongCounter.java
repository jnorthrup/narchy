package jcog.data;

import java.util.concurrent.atomic.AtomicLongFieldUpdater;

import static java.util.concurrent.atomic.AtomicLongFieldUpdater.newUpdater;

/**
 * from Hazelcast
 *
 * A {@link Counter} that is thread-safe; so can be incremented by multiple threads concurrently.
 *
 * The MwCounter is not meant for a huge amount of contention. In that case it would be better to create a counter
 * on the {@link java.util.concurrent.atomic.LongAdder}.
 *
 * This counter does not provide padding to prevent false sharing.
 */
public final class LongCounter {

    private static final AtomicLongFieldUpdater<LongCounter> COUNTER = newUpdater(LongCounter.class, "value");

    private volatile long value;

    public LongCounter() {
        this(0);
    }

    public LongCounter(long initialValue) {
        this.value = initialValue;
    }


    public long get() {
        return value;
    }

    public long getThenZero() {
        return COUNTER.getAndSet(this, 0);
    }

    public long inc() {
        return COUNTER.incrementAndGet(this);
    }


    public long add(long amount) {
        return COUNTER.addAndGet(this, amount);
    }

    @Override
    public String toString() {
        return "Counter{"
                + "value=" + value
                + '}';
    }




//    /**
//     * Creates a new MwCounter with 0 as its initial value.
//     *
//     * @return the new MwCounter, set to 0.
//     */
//    public static MwCounter newMwCounter() {
//        return newMwCounter(0);
//    }
//
//    /**
//     * Creates a new MwCounter with the given initial value.
//     *
//     * @param initialValue the initial value of the counter.
//     * @return the new MwCounter, set to the given initial value.
//     */
//    public static MwCounter newMwCounter(long initialValue) {
//        return new MwCounter(initialValue);
//    }
}
