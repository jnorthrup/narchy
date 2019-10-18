package jcog.math;

import jcog.TODO;
import jcog.Util;
import jcog.data.MutableFloat;


public class FloatRange extends MutableFloat /*AtomicFloat*/ {

    public final float max, min;

    public FloatRange(float value, float min, float max) {
        this.min = min;
        this.max = max;
        set(value);
    }

    @Override
    public void set(float value) {
        super.set(Util.clamp(value, min, max));
    }

    public final void set(double value) {
        set((float)value);
    }

    public static FloatRange unit(float initialValue) {
        return new FloatRange(initialValue, 0, 1);
    }

    public static FloatRange unit(FloatSupplier initialValue) {
        return unit(initialValue.asFloat());
    }

    public final void setProportionally(float x) {
        set(Util.lerp(x, min, max));
    }

    public FloatRange subRange(float subMin, float subMax) {
        subMin = Math.max(min, subMin);
        //if (subMin < min) throw new NumberIsTooSmallException(subMin, min, true );
        subMax = Math.min(max, subMax);
        //if (subMax > max)throw new NumberIsTooSmallException(subMax, max, true );
        return new FloatRange(get(), subMin, subMax) {
            @Override
            public float get() {
                return FloatRange.this.get();
            }

            @Override
            public void set(float value) {
                FloatRange.this.set(value);
            }
        };
    }


    public static FloatRange mapRange(float mapMin, float mapMax) {
        throw new TODO();
    }

}
