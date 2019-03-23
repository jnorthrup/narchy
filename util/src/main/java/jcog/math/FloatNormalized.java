package jcog.math;

public class FloatNormalized implements FloatSupplier {

    private final FloatSupplier in;

    private final FloatNormalizer normalizer;

    public FloatNormalized(FloatSupplier in) {
        this(in, -Float.MIN_NORMAL, +Float.MIN_NORMAL);
    }

    public FloatNormalized(FloatSupplier in, float minStart, float maxStart) {
        this(in, minStart, maxStart, Math.signum(minStart) != Math.signum(maxStart) );
    }

    public FloatNormalized(FloatSupplier in, float minStart, float maxStart, boolean polar) {
        normalizer =
                polar ?
                    new FloatPolarNormalizer(Math.max(Math.abs(minStart), Math.abs(maxStart))) :
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
