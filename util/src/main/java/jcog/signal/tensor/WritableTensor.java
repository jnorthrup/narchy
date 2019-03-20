package jcog.signal.tensor;

import jcog.TODO;
import jcog.signal.Tensor;

public interface WritableTensor extends Tensor {

    void setAt(float newValue, int linearCell);

    default void set(float newValue, int[] cell) {
        setAt(newValue, index(cell));
    }

    /** returns the new value after adding */
    default float addAt(float x, int linearCell) {
        float next = getAt(linearCell);
        if (next!=next) next = 0; //reset to zero
        next += x;
        setAt(next, linearCell);
        return next;
    }

    default void setAt(float[] values, int linearCellStart) {
        int i = linearCellStart;
        for (float v: values)
            setAt(v, i++);
    }

    default void readFrom(Tensor from, int[] fromStart, int[] fromEnd, int[] myStart, int[] myEnd) {
        throw new TODO();
    }

    default void fill(float x) {
        int v = volume();
        for (int i = 0; i < v; i++)
            setAt(x, i);
    }

}
