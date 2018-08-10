package jcog.math;

import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;

/**
 * exponential moving average of a Float source.
 * can operate in either low-pass (exponential moving average of a signal) or
 * high-pass modes (signal minus its exponential moving average).
 */
public class FloatAveraged implements FloatToFloatFunction {
    private float prev;
    private final FloatRange alpha;
    private final boolean lowOrHighPass;

    public FloatAveraged(float alpha) {
        this(alpha, true);
    }

    public FloatAveraged(float alpha, boolean lowOrHighPass) {
        this(new FloatRange(alpha, 0, 1f), lowOrHighPass);
    }

    public FloatAveraged(FloatRange alpha, boolean lowOrHighPass) {
        this.alpha = alpha;
        this.lowOrHighPass = lowOrHighPass;
    }

    @Override
    public float valueOf(float x) {


        synchronized (this) {
            if (x != x)
                x = this.prev; 

            if (x!=x)
                return Float.NaN;

            float alpha = this.alpha.get();
            float next = (alpha) * x + (1f - alpha) * prev;
            this.prev = next;
            return lowOrHighPass ? next : x - next;
        }
    }

    /** previous value computed by valueOf */
    public float floatValue() {
        return prev;
    }
}
