package jcog.math.tensor;

import jcog.util.FloatFloatToFloatFunction;

/** tensor computed by applying a function to its previous value */
public class TensorMerge extends BatchArrayTensor {

    private final FloatFloatToFloatFunction func;
    private final Tensor from;

    protected TensorMerge(Tensor from) {
        super(from.shape());
        this.from = from;
        this.func = (FloatFloatToFloatFunction)this;
    }

    public TensorMerge(Tensor from, FloatFloatToFloatFunction func) {
        super(from.shape());
        this.from = from;
        this.func = func;
    }

    @Override
    public void update() {
        from.get();
        from.writeTo(func, data);//trigger any updates but using the iterator HACK, not:
    }


}
