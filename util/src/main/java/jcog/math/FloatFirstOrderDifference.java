package jcog.math;

import java.util.function.LongSupplier;


public class FloatFirstOrderDifference implements FloatSupplier {

    final FloatSupplier in;

    float lastValue;
    private long lastUpdate;
    private final LongSupplier clock;

    public FloatFirstOrderDifference(LongSupplier clock, FloatSupplier in) {
        this.in = in;
        this.clock = clock;
        this.lastUpdate = clock.getAsLong();
        this.lastValue = Float.NaN;
    }

    @Override
    public float asFloat() {

        long now = clock.getAsLong();
        float currentValue = in.asFloat();

        if (lastValue!=lastValue) {
            this.lastValue = currentValue;
            return 0;
        }

        float result = currentValue - lastValue;

        if (now!=lastUpdate) {
            lastUpdate = now;
            lastValue = currentValue;
        }


        return result;
    }
}
