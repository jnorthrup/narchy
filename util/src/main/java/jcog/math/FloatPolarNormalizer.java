package jcog.math;

import jcog.pri.Prioritized;

public class FloatPolarNormalizer extends FloatNormalizer {

    public FloatPolarNormalizer() {
        this(Prioritized.EPSILON);
    }

    public FloatPolarNormalizer(float radius) {
        super(-radius, radius);
    }

    @Override
    public float valueOf(float raw) {
        if (raw==raw) {
            updateRange(Math.abs(raw));
            min = -max;
            return normalize(raw, min(), max());
        } else {
            return Float.NaN;
        }
    }
}
