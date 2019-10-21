package jcog.signal.tensor;

import jcog.Util;
import jcog.math.FloatSupplier;
import jcog.signal.Tensor;
import jcog.util.FloatFloatToFloatFunction;

/** rate between 0 and 1 */
public class TensorLERP extends TensorMerge implements FloatFloatToFloatFunction {

    private final FloatSupplier rate;
    private float currentRate;

    public TensorLERP(Tensor from, float rate) {
        this(from, new FloatSupplier() {
            @Override
            public float asFloat() {
                return rate;
            }
        });
    }

    public TensorLERP(Tensor from, FloatSupplier rate) {
        super(from);
        this.rate = rate;
        commit();
    }



    @Override protected void commit() {
        currentRate = rate.asFloat();
    }

    @Override
    public float apply(float current, float next) {
        return Util.lerp(currentRate, current, next);
    }

}
