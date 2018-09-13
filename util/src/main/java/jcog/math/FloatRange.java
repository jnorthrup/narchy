package jcog.math;

import jcog.data.MutableFloat;


public class FloatRange extends MutableFloat /*AtomicFloat*/ {

    public final float max;
    public final float min;
    
    public FloatRange(float value, float min, float max) {
        super(value);
        this.min = min;
        this.max = max;
    }

    public static FloatRange unit(float initialValue) {
        return new FloatRange(initialValue, 0, 1);
    }
    public static FloatRange unit(FloatSupplier initialValue) {
        return unit(initialValue.asFloat());
    }
}
