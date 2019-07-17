package jcog.signal.meter;

import java.util.concurrent.atomic.AtomicLong;

/** NOTE: this can extend EITHER AtomicLong or LongAdder */
public class FastCounter extends AtomicLong /*LongAdder*/{

    private final String name;

    public FastCounter(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name + '=' + super.toString();
    }

    public final void add(long x) {
        addAndGet(x);
    }

    public final void increment() {
        incrementAndGet();
    }
}
