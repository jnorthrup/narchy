package jcog.math;

import jcog.Util;

public class IntRange extends MutableInteger {

    public final int max;
    public final int min;

    public IntRange(int value, int min, int max) {
        super(value);
        this.min = min;
        this.max = max;
    }

    @Override
    public final IntRange set(int value) {
        super.set(Util.clamp(value, min, max));
        return this;
    }



}
