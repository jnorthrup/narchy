package jcog.math;

import jcog.Util;
import jcog.pri.ScalarValue;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;

public class FloatNormalizer implements FloatToFloatFunction  {
    /** precision threshold */
    private static final float epsilon = Float.MIN_NORMAL;
    protected float min;
    protected float max;
    /** relaxation rate: brings min and max closer to each other in proportion to the value. if == 0, disables */
    private float relax = 0;

    public FloatNormalizer() {
        this(Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY);
    }

    FloatNormalizer(float minStart, float maxStart) {
        this.min = minStart;
        this.max = maxStart;
    }

    public float min() {
        return min;
    }

    public float max() {
        return max;
    }


    public float valueOf(float raw) {
        if (raw!=raw)
            return Float.NaN;

        updateRange(raw);

        return normalize(raw, min(), max());
    }

    protected float normalize(float x, float min, float max) {
        if (x!=x)
            return Float.NaN;

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

    FloatNormalizer updateRange(float raw) {

        if (min > raw) {
            min = raw;
        }

        if (max < raw) {
            max = raw;
        }

        if (relax > 0) {
            float range = max - min;
            if (range > ScalarValue.EPSILON) {
                float mid = (max+min)/2;
                max = Util.lerp(relax, max, mid);
                min = Util.lerp(relax, min, mid);
            }
        }
        return this;
    }

//    public static class FloatBiasedNormalizer extends FloatNormalizer {
//        public final FloatRange bias;
//
//        public FloatBiasedNormalizer(FloatRange bias) {
//            this.bias = bias;
//        }
//
//        @Override
//        protected float normalize(float x, float min, float max) {
//            float y = super.normalize(x, min, max);
//            if (y == y) {
//                float balance = Util.unitize(bias.floatValue());
//                if (y >= 0.5f) {
//                    return Util.lerp(2f * (y - 0.5f), balance, 1f);
//                } else {
//                    return Util.lerp(2f * (0.5f - y), balance, 0f);
//                }
//            } else
//                return Float.NaN;
//
//            //return Util.unitize(y + (b - 0.5f));
//        }
//    }
}
