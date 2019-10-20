package jcog.math;

public class FloatPolarNormalizer extends FloatNormalizer {

    public FloatPolarNormalizer() {
        this(Float.MIN_NORMAL* 2.0F);
    }

    public FloatPolarNormalizer(float radius) {
        super(-radius, radius);
    }

    @Override
    public float valueOf(float raw) {
        if (raw==raw) {
            updateRange(Math.abs(raw));
            float range = Math.max(Math.abs(min), Math.abs(max));
            return normalize(raw, min = -range, max = range);
        } else {
            return Float.NaN;
        }
    }
}
