package jcog.signal.tensor;

import jcog.TODO;
import jcog.pri.op.PriReturn;
import jcog.signal.Tensor;
import jcog.util.FloatFloatToFloatFunction;

public interface WritableTensor extends Tensor {

    void setAt(int linearCell, float newValue);

    default void set(float newValue, int[] cell) {
        setAt(index(cell), newValue);
    }

    /** returns the new value after adding */
    default float addAt(float x, int linearCell) {
        float next = getAt(linearCell);
        if (next!=next) next = 0; //reset to zero
        next += x;
        setAt(linearCell, next);
        return next;
    }

    default void setAt(int linearCellStart, float[] values) {
        int i = linearCellStart;
        for (float v: values)
            setAt(i++, v);
    }

    default void readFrom(Tensor from, int[] fromStart, int[] fromEnd, int[] myStart, int[] myEnd) {
        throw new TODO();
    }

    default /* final */ float merge(int linearCell, float arg, FloatFloatToFloatFunction x) {
        return merge(linearCell, arg, x, PriReturn.Post);
    }

    default float merge(int linearCell, float arg, FloatFloatToFloatFunction x, PriReturn returning) {
        float prev = getAt(linearCell);

        float next = x.apply(prev, arg);

        setAt(linearCell, next);

        return returning.apply(arg, prev, next);
    }

    default void fill(float x) {
        int v = volume();
        for (int i = 0; i < v; i++)
            setAt(i, x);
    }

    default void set(Tensor x) {
        x.forEach(this::setAt);
    }
}
