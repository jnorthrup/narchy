package jcog.signal.tensor;

import jcog.math.FloatSupplier;

/** dynamically updating 1-value tensor from a FloatSupplier.
 * TODO dont override ArrayTensor but something simpler */
public class ScalarTensor extends ArrayTensor {
    private final FloatSupplier f;

    public ScalarTensor(FloatSupplier f) {
        super(1);
        this.f = f;
    }

    @Override public float[] snapshot() {
        return new float[] { this.data[0] = f.asFloat() };
    }
}
