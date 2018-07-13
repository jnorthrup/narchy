package jcog.math;

import jcog.util.AtomicFloat;


public class FloatRange extends AtomicFloat {

    public final float max;
    public final float min;
    
    public FloatRange(float value, float min, float max) {
        super(value);
        this.min = min;
        this.max = max;
    }

}
