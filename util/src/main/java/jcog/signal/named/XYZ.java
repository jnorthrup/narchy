package jcog.signal.named;

import jcog.math.FloatRange;

/**
 * labeled FloatRange controls underlying ArrayTensor.
 */
public class XYZ extends XY {
    public final FloatRange z;

    public XYZ(float min, float max) {
        super(min, max, new float[3]);

        z = new FloatRange(1f, min, max) {
            @Override
            public void set(float newValue) {
                super.set(data[2] = newValue);
            }
        };
    }
}
