package jcog.learn;

import jcog.Util;

/** Map-like interface for learning associations of numeric vectors */
public interface Predictor {


    /** learn; analogous to Map.put */
    void learn(double[] x, double[] y);

    /** predict: analogous to Map.get */
    double[] predict(double[] x);


    default void learn(float[] x, float[] y) {
        learn(Util.toDouble(x), Util.toDouble(y));
    }

    default double[] predict(float[] x) {
        return predict(Util.toDouble(x));
    }

}
