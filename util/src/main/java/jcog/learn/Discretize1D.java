package jcog.learn;

public interface Discretize1D  {

    default void reset(int levels) {
        reset(levels, 0, 1);
    }

    void reset(int levels, double min, double max);

    /** trains a value */
    void put(double value);

    /** calculates the closest index of a value */
    int index(double value);

    /** estimates the value of an index */
    double value(int v);
}
