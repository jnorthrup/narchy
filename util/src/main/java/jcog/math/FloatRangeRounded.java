package jcog.math;

import jcog.Util;

public class FloatRangeRounded extends FloatRange {

    float epsilon;

    public FloatRangeRounded(float value, float min, float max, float epsilon) {
        super(value, min, max);
        this.epsilon = epsilon;
        set(value); 
    }

    @Override
    public void set(float value) {
        super.set(Util.round(value, epsilon));
    }
}
