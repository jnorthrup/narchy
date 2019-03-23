package jcog.math;

import jcog.pri.ScalarValue;

public class FloatPolarNormalizer extends FloatNormalizer {

    public FloatPolarNormalizer() {
        this(ScalarValue.EPSILON);
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
