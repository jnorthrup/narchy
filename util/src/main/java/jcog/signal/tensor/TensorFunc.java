package jcog.signal.tensor;

import jcog.signal.Tensor;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;
import org.eclipse.collections.api.block.procedure.primitive.IntFloatProcedure;

/** view applying a specified function to each element */
public class TensorFunc implements Tensor {

    public final FloatToFloatFunction func;
    private final Tensor from;

    public TensorFunc(Tensor from, FloatToFloatFunction func) {
        this.from = from;
        this.func = func;
    }

    @Override
    public float get(int... cell) {
        return func.valueOf(from.get(cell));
    }

    @Override
    public float getAt(int linearCell) {
        return func.valueOf(from.getAt(linearCell));
    }

    @Override
    public int index(int... cell) {
        return from.index(cell);
    }

    @Override
    public float[] snapshot() {
        var x = from.snapshot();
        for (var i = 0; i < x.length; i++)
            x[i] = func.valueOf(x[i]);
        return x;
    }

    @Override
    public int[] shape() {
        return from.shape();
    }

    @Override
    public void forEach(IntFloatProcedure sequential, int start, int end) {
        from.forEach((i,v) -> sequential.value(i, func.valueOf(v)), start, end);
    }

}
