package jcog.math;

import jcog.pri.Pri;

public class FloatPolarNormalizer extends FloatNormalizer {

    public FloatPolarNormalizer() {
        this(Pri.EPSILON);
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
