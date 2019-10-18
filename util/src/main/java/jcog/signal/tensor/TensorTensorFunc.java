package jcog.signal.tensor;

import jcog.signal.Tensor;
import jcog.util.FloatFloatToFloatFunction;
import org.eclipse.collections.api.block.procedure.primitive.IntFloatProcedure;

/** view applying a specified function to an element from 2 input tensors */
public class TensorTensorFunc implements Tensor {

    public final FloatFloatToFloatFunction func;
    private final Tensor a;
    private final Tensor b;

    public TensorTensorFunc(Tensor a, Tensor b, FloatFloatToFloatFunction func) {
        assert(a.equalShape(b));
        this.a = a;
        this.b = a;
        this.func = func;
    }

    @Override
    public float get(int... cell) {
        return func.apply(a.get(cell), b.get(cell));
    }

    @Override
    public float getAt(int linearCell) {
        return func.apply(a.getAt(linearCell), b.getAt(linearCell));
    }

    @Override
    public int index(int... cell) {
        return a.index(cell);
    }

    @Override
    public float[] snapshot() {
        float[] ab = new float[volume()];
        for (int i = 0; i < ab.length; i++)
            ab[i] = getAt(i);
        return ab;
    }

    @Override
    public int[] shape() {
        return a.shape();
    }

    @Override
    public void forEach(IntFloatProcedure sequential, int start, int end) {
        a.forEach((i, aa) -> sequential.value(i, func.apply(aa, b.getAt(i))), start, end);
    }
}
