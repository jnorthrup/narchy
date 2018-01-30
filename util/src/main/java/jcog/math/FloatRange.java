package jcog.math;

import org.apache.commons.lang3.mutable.MutableFloat;

/**
 * Created by me on 11/18/16.
 */
public class FloatRange extends MutableFloat implements FloatSupplier {

    public final float max;
    public final float min;

    public FloatRange() {
        this(0);
    }

    /** defaults to unit range, 0..1.0 */
    public FloatRange(float value) {
        this(value, 0, 1f);
    }

    public FloatRange(float value, float min, float max) {
        super(value);
        this.min = min;
        this.max = max;
    }

    @Override
    public final float asFloat() {
        return floatValue();
    }

}
