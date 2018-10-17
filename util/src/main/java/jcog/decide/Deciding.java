package jcog.decide;

import jcog.Util;

import java.util.function.ToIntFunction;

/**
 * returns an integer in the range of 0..n for the input vector of length n
 */
public interface Deciding extends ToIntFunction<float[]> {

    default int applyAsInt(double[] x) {
        return applyAsInt(Util.toFloat(x));
    }

}
