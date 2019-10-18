package jcog.math;

import jcog.Util;

public class FloatClamped implements FloatSupplier {
    final float min;
    final float max;
    private final FloatSupplier in;

    public FloatClamped(FloatSupplier in, float min, float max) {
        this.min = min;
        this.max = max;
        this.in = in;
    }

    @Override
    public float asFloat() {
        float v = in.asFloat();
        return v==v ? Util.clamp(v, min, max) : Float.NaN;
    }
}
