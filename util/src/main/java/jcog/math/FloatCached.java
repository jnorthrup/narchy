package jcog.math;

import java.util.function.LongSupplier;

/**
 * buffers a FloatSupplier's value until supplied clock signal changes
 */
public class FloatCached implements FloatSupplier {

    final FloatSupplier in;
    final LongSupplier clock;
    /*volatile*/ float current = Float.NaN;
    /*volatile*/ long lastTime;

    public FloatCached(FloatSupplier in, LongSupplier clock) {
        this.in = in;
        this.clock = clock;
        this.lastTime = clock.getAsLong()-1; 
        asFloat();
    }

    @Override
    public float asFloat() {
        synchronized (this) {
            long now = clock.getAsLong();
            if (now!=lastTime) {
                lastTime = now;
                current = in.asFloat();
            }
        }
        return current;
    }
}
