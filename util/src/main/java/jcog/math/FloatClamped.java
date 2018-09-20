package jcog.math;

import jcog.Util;

public class FloatClamped implements FloatSupplier {
    final float min, max;
    private final FloatSupplier in;

    public FloatClamped(FloatSupplier in, float min, float max) {
        this.min = min;
        this.max = max;
        this.in = in;
    }

    @Override
    public float asFloat() {
        return Util.clamp(in.asFloat(), min, max);
    }
}
