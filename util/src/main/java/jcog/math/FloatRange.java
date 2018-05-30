package jcog.math;

import org.apache.commons.lang3.mutable.MutableFloat;


public class FloatRange extends MutableFloat  {

    public final float max;
    public final float min;







    public FloatRange(float value, float min, float max) {
        super(value);
        this.min = min;
        this.max = max;
    }

}
