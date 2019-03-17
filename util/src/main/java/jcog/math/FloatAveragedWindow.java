package jcog.math;

import com.google.common.util.concurrent.AtomicDouble;
import jcog.TODO;
import jcog.signal.Tensor;
import jcog.signal.tensor.AtomicArrayTensor;
import jcog.signal.tensor.RingTensor;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;

/**
 *
 *  exponential moving average with fixed rolling window (thread-safe)
 *
 *  * <p>
 *  * https://en.wikipedia.org/wiki/Exponential_smoothing
 *  * https://dsp.stackexchange.com/a/20336
 *  * <p>
 * */
public class FloatAveragedWindow implements FloatSupplier, FloatToFloatFunction {

    private final FloatRange alpha;
    final RingTensor window;
    final AtomicDouble current = new AtomicDouble(Double.NaN);



    private interface AverageStrategy {
        double apply(Tensor window, float alpha);
    }

    public enum Mode implements AverageStrategy {
        Mean {
            @Override
            public double apply(Tensor window, float alpha) {
                //how to use the alpha parameter?

//                Sum sum = new Sum();
//                double sampleSize = length;
//
//                // Compute initial estimate using definitional formula
//                double xbar = sum.evaluate(values, begin, length) / sampleSize;
//
//                // Compute correction factor in second pass
//                double correction = 0;
//                for (int i = begin; i < begin + length; i++) {
//                    correction += values[i] - xbar;
//                }
//                return xbar + (correction/sampleSize);
                throw new TODO();
            }
        },
        Exponential {
            @Override
            public double apply(Tensor window, float a) {
                final double[] next = {Double.NaN};
                //TODO Tensor reduce function
                window.forEach((int i, float n) -> {
                    if (n != n)
                        return;

                    double prev = next[0];
                    next[0] = (prev == prev) ?
                            ((1 - a) * prev) + (a * n)
                            :
                            n; //initial value
                });
                return next[0];
            }
        };
    }

    public Mode mode = Mode.Exponential;

    public FloatAveragedWindow(int windowSize, float alpha) {
        this(windowSize, new FloatRange(alpha, 0, 1f));
    }

    public FloatAveragedWindow(int windowSize, FloatRange alpha) {
        this.window = new RingTensor(new AtomicArrayTensor(windowSize), 1, windowSize);
        window.fill(Float.NaN);
        this.alpha = alpha;
    }


    /**
     * supplies a "high-pass filter" view of this window
     */
    public FloatAveragedWindow highpass() {
//                if (x != x)
//                    return this.prev;
//
//                float p = prev, next;
//                if (p == p) {
//                    float alpha = this.alpha.get();
//                    next = (alpha) * x + (1f - alpha) * p;
//                } else {
//                    next = x;
//                }
//                this.prev = next;
//                return lowOrHighPass ? next : x - next;

        throw new TODO();
    }

    public FloatAveragedWindow mode(Mode mode) {
        this.mode = mode;
        return this;
    }

    /** warning: commits */
    public final float asFloat(FloatSupplier next) {
        return valueOf(next.asFloat());
    }

    /** warning: commits */
    @Override public final float valueOf(float next) {
        setAndCommit(next);
        return asFloat();
    }


    /** add to current value */
    public final void add(float inc) {
        if (inc == inc) {
            window.addAt(inc, window.target());
        }
    }

    /** set current value */
    public final void set(float x) {
        if (x == x) {
            window.setAt(x, window.target());
        }
    }

    public void setAndCommit(float v) {
        if (v == v) {
            //TODO make AtomicRingBufferTensor
            window.commit(new float[]{v});
            invalidate();
        }
    }

    public void commit(float initialValueForNext) {
        window.spin((i)-> window.setAt(initialValueForNext, i));
        invalidate();
    }

    private void invalidate() {
        current.set(Double.NaN);
    }

    protected double calculate() {
        return mode.apply(window, alpha.get());
    }

    @Override
    public float asFloat() {
        return (float) asDouble();
    }

    public double asDouble() {
        double c = asCached();
        if (c != c) {
            double calculated = calculate();
            current.compareAndSet(c, calculated);
            c = calculated;
        }
        return c;
    }

    public double asCached() {
        return current.get();
    }

}