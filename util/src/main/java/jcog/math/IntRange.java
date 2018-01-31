package jcog.math;

public class IntRange extends MutableInteger {

    public final int max;
    public final int min;

    public IntRange(int value, int min, int max) {
        super(value);
        this.min = min;
        this.max = max;
    }


}
