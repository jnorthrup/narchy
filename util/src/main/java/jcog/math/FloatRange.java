package jcog.math;

import org.apache.commons.lang3.mutable.MutableFloat;


public class FloatRange extends MutableFloat  {

    public final float max;
    public final float min;

    /** defaults to unit range, 0..1.0 */
    public FloatRange(float value) {
        this(value, 0, 1f);
        assert(value >=0 && value <=1f);
    }

    public FloatRange(float value, float min, float max) {
        super(value);
        this.min = min;
        this.max = max;
    }

}
