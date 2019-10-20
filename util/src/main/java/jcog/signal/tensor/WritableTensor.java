package jcog.signal.tensor;

import jcog.pri.op.PriReturn;
import jcog.signal.Tensor;
import jcog.util.FloatFloatToFloatFunction;
import org.jetbrains.annotations.Nullable;

public interface WritableTensor extends Tensor {

    void setAt(int linearCell, float newValue);

    default void set(float newValue, int[] cell) {
        setAt(index(cell), newValue);
    }

    /** returns the new value after adding */
    default float addAt(float x, int linearCell) {
        var next = getAt(linearCell);
        if (next!=next) next = 0; //reset to zero
        next += x;
        setAt(linearCell, next);
        return next;
    }

    default void setAt(int linearCellStart, float[] values) {
        var i = linearCellStart;
        for (var v: values)
            setAt(i++, v);
    }

//    default void readFrom(Tensor from, int[] fromStart, int[] fromEnd, int[] myStart, int[] myEnd) {
//        throw new TODO();
//    }

    default /* final */ float merge(int linearCell, float arg, FloatFloatToFloatFunction x) {
        return merge(linearCell, arg, x, PriReturn.Post);
    }

    default float merge(int linearCell, float arg, FloatFloatToFloatFunction x, @Nullable PriReturn returning) {
        var prev = getAt(linearCell);

        var next = x.apply(prev, arg);

        if (prev!=next)
            setAt(linearCell, next);

        return returning!=null ? returning.apply(arg, prev, next) : Float.NaN;
    }

    default void fill(float x) {
        var v = volume();
        for (var i = 0; i < v; i++)
            setAt(i, x);
    }

    default void setAll(float[] values) {
        var v = volume();
        if(v!=values.length)
            throw new ArrayIndexOutOfBoundsException();

        for (var i = 0; i < v; i++)
            setAt(i, values[i]);
    }

    default void set(Tensor x) {
        x.forEach(this::setAt);
    }
}
