package jcog.math;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;


public class FloatFirstOrderDifference implements FloatSupplier {

    final FloatSupplier in;

    float lastValue;
    float lastDifference = 0;
    private final AtomicLong lastUpdate;
    private final LongSupplier clock;

    public FloatFirstOrderDifference(LongSupplier clock, FloatSupplier in) {
        this.in = in;
        this.clock = clock;
        this.lastUpdate = new AtomicLong(clock.getAsLong());
        this.lastValue = Float.NaN;
    }

    @Override
    public float asFloat() {

        long now = clock.getAsLong();
        long before = lastUpdate.get();
        if (!lastUpdate.compareAndSet(before, now)) {
            return lastDifference; //cached
        }

        float currentValue = in.asFloat();

        float lastValue = this.lastValue;

        this.lastValue = currentValue;

        if (lastValue!=lastValue) {
            return lastDifference = 0;
        } else {
            return (lastDifference = (currentValue - lastValue));
        }

    }
}
