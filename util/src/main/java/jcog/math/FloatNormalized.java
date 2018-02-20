package jcog.math;

import jcog.Util;
import jcog.pri.Pri;

import java.util.function.DoubleSupplier;


public class FloatNormalized implements FloatSupplier {

    private final FloatSupplier in;
    private float min;
    private float max;
    private final float minStart;
    private final float maxStart;

    /** precision threshold */
    static final float epsilon = Float.MIN_NORMAL;

    /** relaxation rate: brings min and max closer to each other in proportion to the value. if == 0, disables */
    private float relax;


    public FloatNormalized(DoubleSupplier in) {
        this(()->(float)in.getAsDouble());
    }
    public FloatNormalized(FloatSupplier in) {
        this(in, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY);
    }

    public FloatNormalized(FloatSupplier in, float minStart, float maxStart) {
        this.in = in;
        this.minStart = minStart;
        this.maxStart = maxStart;
        reset();
    }

    @Override
    public String toString() {
        return "RangeNormalizedFloat{" +
                "min=" + min() +
                ", max=" + max() +
                '}';
    }

    public float min() {
        return min;
    }

    public float max() {
        return max;
    }

    public void reset() {
        min = minStart;
        max = maxStart;
    }

    @Override
    public float asFloat() {
        float raw = in.asFloat();
        return raw != raw ? Float.NaN : normalize(raw);
    }

    public float normalize(float raw) {

        updateRange(raw);

        return normalize(raw, min(), max());
    }

    public float normalize(float x, float min, float max) {
        float r = max - min;
        assert(r >= 0);
        if (r <= epsilon)
            return x;
        else
            return (x - min) / r;
    }

    /**
     * decay rate = 1 means unaffected.  values less than 1 constantly
     * try to shrink the range to zero.
     * @param rate
     * @return
     */
    public FloatNormalized relax(float rate) {
        this.relax = rate;
        return this;
    }

    public FloatNormalized updateRange(float raw) {

        boolean incMin = false, incMax = false;

        if (min > raw) {
            min = raw;
        } else {
            incMin = true;
        }

        if (max < raw) {
            max = raw;
        } else {
            incMax = true;
        }

        if (relax > 0) {
            float range = max - min;
            if (range > Pri.EPSILON) {
                float max0 = max;
                if (incMax)
                    max = Util.lerp(relax, max, min);
                if (incMin)
                    min = Util.lerp(relax, min, max0);
            }
        }

        return this;
    }

}
