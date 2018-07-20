package jcog.math;

import java.util.function.DoubleSupplier;


public class FloatNormalized implements FloatSupplier {

    private final FloatSupplier in;

    private final FloatNormalizer normalizer;

    public FloatNormalized(FloatSupplier in) {
        this(in, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY);
    }

    public FloatNormalized(FloatSupplier in, float minStart, float maxStart) {
        this(in, minStart, maxStart, false);
    }

    public FloatNormalized(FloatSupplier in, float minStart, float maxStart, boolean polar) {
        normalizer =
                polar ?
                    new FloatPolarNormalizer(minStart) :
                    new FloatNormalizer(minStart, maxStart);

        this.in = in;
    }

    @Override
    public String toString() {
        return "RangeNormalizedFloat{" +
                "min=" + normalizer.min() +
                ", max=" + normalizer.max() +
                '}';
    }

    @Override
    public float asFloat() {
        float raw = in.asFloat();
        return raw != raw ? Float.NaN : normalizer.valueOf(raw);
    }

    public FloatNormalized relax(float rate) {
        normalizer.relax(rate);
        return this;
    }

    public FloatNormalized updateRange(float x) {
        normalizer.updateRange(x);
        return this;
    }
}
