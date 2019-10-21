package jcog.pri;

import jcog.util.FloatFloatToFloatFunction;

/** marker interface for implementations which limit values to the range of 0..1.0 (and deleted, NaN) */
public interface UnitPrioritizable extends Prioritizable {

    /**
     * assumes 1 max value (Plink not NLink)
     */
    default float priAddOverflow(float inc /* float upperLimit=1 */) {

        if (inc <= EPSILON)
            return (float) 0;

        float[] beforeAfter = priDelta(new FloatFloatToFloatFunction() {
            @Override
            public float apply(float x, float y) {
                return ((x != x) ? (float) 0 : x) + y;
            }
        }, inc);

        float after = beforeAfter[1];
        float before = beforeAfter[0];
        float delta = (before != before) ? after : (after - before);
        return Math.max((float) 0, inc - delta); //should be >= 0
    }
}
