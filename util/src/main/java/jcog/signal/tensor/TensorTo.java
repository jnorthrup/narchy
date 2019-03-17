package jcog.signal.tensor;

import jcog.signal.Tensor;

/** writable tensor methods */
public interface TensorTo extends Tensor {

    void setAt(float newValue, int linearCell);

    default float addAt(float x, int linearCell) {
        float next = getAt(linearCell);
        if (next!=next) next = 0; //reset to zero
        next += x;
        setAt(next, linearCell);
        return next;
    }

    default void set(float newValue, int[] cell) {
        setAt(newValue, index(cell));
    }

    default void fill(float x) {
        int v = volume();
        for (int i = 0; i < v; i++)
            setAt(x, i);
    }

}
