package jcog.math;

import jcog.Util;
import jcog.pri.Prioritized;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;

public class FloatNormalizer implements FloatToFloatFunction  {
    /** precision threshold */
    static final float epsilon = Float.MIN_NORMAL;
    protected final float minStart;
    protected final float maxStart;
    protected float min;
    protected float max;
    /** relaxation rate: brings min and max closer to each other in proportion to the value. if == 0, disables */
    private float relax;

    public FloatNormalizer() {
        this(Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY);
    }

    public FloatNormalizer(float minStart, float maxStart) {
        this.minStart = minStart;
        this.maxStart = maxStart;
        reset();
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

    public float valueOf(float raw) {

        updateRange(raw);

        return normalize(raw, min(), max());
    }

    protected float normalize(float x, float min, float max) {
        float r = max - min;
        assert(r >= 0);
        if (r <= epsilon)
            return 0.5f;
        else
            return (x - min) / r;
    }

    /**
     * decay rate = 1 means unaffected.  values less than 1 constantly
     * try to shrink the range to zero.
     * @param rate
     * @return
     */
    public FloatNormalizer relax(float rate) {
        this.relax = rate;
        return this;
    }

    public FloatNormalizer updateRange(float raw) {
        if (relax > 0) {
            float range = max - min;
            if (range > Prioritized.EPSILON) {
                float mid = (max+min)/2;
                max = Util.lerp(relax, max, mid);
                min = Util.lerp(relax, min, mid);
            }
        }


        if (min > raw) {
            min = raw;
        }

        if (max < raw) {
            max = raw;
        }

        return this;
    }
}
