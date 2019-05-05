package jcog.signal.tensor;

import jcog.signal.Tensor;
import jcog.util.ArrayUtil;

/** constant shape */
public abstract class AbstractShapedTensor extends AbstractTensor {
    public final int[] shape;
    protected transient final int[] stride;

    protected AbstractShapedTensor(int[] shape) {

        if (shape.length > 1) {
            this.stride = Tensor.stride(shape);


        } else {

            this.stride = ArrayUtil.EMPTY_INT_ARRAY;
        }

        this.shape = shape;
    }


    @Override
    public int[] stride() {
        return stride;
    }

    @Override
    public int[] shape() {
        return shape;
    }

}
