package jcog.math;

import jcog.pri.Pri;

/** balances at zero, balanced normalization of positive and negative ranges (radius)
 *  output is normalized to range 0..1.0
 * */
public class FloatPolarNormalized extends FloatNormalized {

//    public FloatPolarNormalized(FloatSupplier in, float midPoint, float radius) {
//        this(() -> in.asFloat() - midPoint, radius );
//    }

    public FloatPolarNormalized(FloatSupplier in) {
        this(in, Pri.EPSILON);
    }

    public FloatPolarNormalized(FloatSupplier in, float radius) {
        super(in, -radius, radius);
    }

    @Override
    public float normalize(float raw) {
        if (raw==raw) {
            updateRange(Math.abs(raw));
            min = 0;
            return normalize(raw, -max(), max());
        } else {
            return Float.NaN;
        }
    }



    //    final RangeNormalizedFloat positive;
//    final RangeNormalizedFloat negative;
//    private final FloatSupplier in;
//
//
//    public PolarRangeNormalizedFloat(FloatSupplier in) {
//        this.in = in;
//        positive = new RangeNormalizedFloat(null);
//        negative = new RangeNormalizedFloat(null);
//    }
//
//    public void reset() {
//        positive.reset();
//        negative.reset();
//    }
//
//    @Override
//    public float asFloat() {
//
//        float v = in.asFloat();
//        if (!Float.isFinite(v))
//            return Float.NaN;
//
//        float w;
//
//        float nonpolarity =
//                (positive.ranged() && negative.ranged()) ?
//                Math.abs(positive.min() - negative.min()) / Math.abs(positive.max() + negative.max()) :
//                0
//                ;
//
//        if (!Float.isFinite(nonpolarity))
//            nonpolarity = 0; //HACK
//
//        if (v < 0) {
//            w = -expand(negative.normalize(-v), nonpolarity);
//        } else if (v > 0) {
//            w = expand(positive.normalize(v), nonpolarity);
//        } else {
//            //TODO within threshold
//            //normalize to set range, but return 0 regardless
//            positive.normalize(0);
//            negative.normalize(0);
//            w = 0;
//        }
//
//
//        if (!Float.isFinite(w))
//            throw new MathArithmeticException();
//
//        return w;
//    }
//
//    public float expand(float v, float nonpolarity) {
//        return v*(1f-nonpolarity) + nonpolarity;
//    }
}
