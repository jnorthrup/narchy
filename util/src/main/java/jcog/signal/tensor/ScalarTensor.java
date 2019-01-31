package jcog.signal.tensor;

import jcog.math.FloatSupplier;

/** dynamically updating 1-value tensor from a FloatSupplier */
public class ScalarTensor extends ArrayTensor {
    private final FloatSupplier f;

    public ScalarTensor(FloatSupplier f) {
        super(1);
        this.f = f;
    }

    @Override public float[] snapshot() {
        setAt(f.asFloat(), 0);
        return super.snapshot();
    }
}
