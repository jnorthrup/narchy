package jcog.math;

import jcog.Util;

public class FloatParamRounded extends FloatParam {

    float epsilon = Float.MIN_NORMAL;

    public FloatParamRounded(float value, float min, float max, float epsilon) {
        super(value, min, max);
        this.epsilon = epsilon;
        set(value); //set again
    }

    @Override
    public void set(float value) {
        super.set(Util.round(value, epsilon));
    }
}
