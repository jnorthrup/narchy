package jcog.signal.meter;

import java.util.concurrent.atomic.LongAdder;

/** could also be LongAdder .. */
public class FastCounter extends LongAdder /* AtomicLong*/ {

    private final String name;

    public FastCounter(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name + '=' + super.toString();
    }

//    public final void increment() {
//        incrementAndGet();
//    }
//    public void add(long amount) {
//        addAndGet(amount);
//    }

}
