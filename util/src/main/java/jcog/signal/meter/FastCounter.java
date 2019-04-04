package jcog.signal.meter;

import java.util.concurrent.atomic.AtomicLong;

public class FastCounter extends AtomicLong {

    private final String name;

    public FastCounter(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name + '=' + super.toString();
    }

    public final void increment() {
        incrementAndGet();
    }

    public void increment(long amount) {
        addAndGet(amount);
    }


}
