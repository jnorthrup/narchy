package jcog.math;

import com.google.common.util.concurrent.AtomicDouble;
import jcog.TODO;
import jcog.signal.Tensor;
import jcog.signal.tensor.AtomicFloatVector;
import jcog.signal.tensor.TensorRing;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;

/**
 *
 *  exponential moving average with fixed rolling window (thread-safe)
 *
 *  * <p>
 *  * https://en.wikipedia.org/wiki/Exponential_smoothing
 *  * https://dsp.stackexchange.com/a/20336
 *  * <p>
 *
 *  TODO make the set/add fully atomic.  it is relatively easy
 * */
public class FloatAveragedWindow implements FloatSupplier, FloatToFloatFunction {

    private final FloatRange alpha;
    final TensorRing window;
    final AtomicDouble current = new AtomicDouble(Double.NaN);

    @Override
    public String toString() {
        return window.toString();
    }

    public FloatAveragedWindow clear(float f) {
        window.fillAll(f);
        return this;
    }

    private interface AverageStrategy {
        double apply(Tensor window, float alpha);
    }

    public enum Mode implements AverageStrategy {

        Mean {
            /** alpha param ignored */
            @Override
            public double apply(Tensor window, float alpha) {
                double total = 0;
                int count = 0;
                //TODO Tensor reduce function
                int vol = window.volume();
                for (int i = 0; i < vol; i++) { //reverse
                    float v = window.getAt(i);
                    if (v != v)
                        continue;

                    total += v;
                    count++;
                }
                return count > 0 ? total / count : Double.NaN;
            }
        },
        Exponential {
            @Override
            public double apply(Tensor window, float a) {
                double next = Double.NaN;
                //TODO Tensor reduce function
                int vol = window.volume();
                for (int i = 0; i < vol; i++) { //reverse
                    float v = window.getAt(i);
                    if (v != v)
                        continue;

                    double prev = next;
                    next = (prev == prev) ?
                            ((1 - a) * prev) + (a * v)
                            :
                            v; //initial value
                }
                return next;
            }
        }
    }

    public Mode mode = Mode.Exponential;

    public FloatAveragedWindow(int windowSize, float alpha) {
        this(windowSize, new FloatRange(alpha, 0, 1f));
    }

    public FloatAveragedWindow(int windowSize, float alpha, float fill) {
        this(windowSize, alpha);
        window.fillAll(fill);
    }

    public FloatAveragedWindow(int windowSize, FloatRange alpha) {
        this.window = new TensorRing(new AtomicFloatVector(windowSize), 1, windowSize);
        this.alpha = alpha;
        window.fillAll(Float.NaN);
    }

    public FloatAveragedWindow(int windowSize, FloatRange alpha, float fill) {
        this(windowSize, alpha);
        window.fillAll(fill);
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
        next(next);
        return asFloat();
    }




    /** set current value */
    public final void set(float x) {
        if (x == x) {
            _set(x);
        }
    }

    /** for recording */
    public void next(float x) {
        _set(x);
        next();
    }

    /** for accumulator use */
    public void reset(float x) {
        next();
        _set(x);
    }

    public void next() {
        window.targetSpin();
    }

    /** add to current value. for use in ordinary accumulator mode, it should not be necessary to invalidate after each add but only on reset. */
    public final void add(float inc) {
        add(inc, false);
    }

    public final void add(float inc, boolean invalidate) {
        if (inc == inc) {
            window.addAtDirect(inc, window.target());
            if (invalidate)
                invalidate();
        }
    }
    protected void _set(float x) {
        window.setAtDirect(x, window.target());
        invalidate();
    }

    public void resetNext(float initialValueForNext) {
        //window.targetSpin((i)-> window.setAt(initialValueForNext, i));
        window.setSpin(initialValueForNext);
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


    public double mean() {
        return window.sumValues() / window.volume();
    }

    /** @standard deviation */
    public double stddev(){
        throw new TODO();
//        float mean = mean();
//        float sum = 0;
//        for(int i = 0; i < values.length; i++){
//            sum += (values[i] - mean) * (values[i] - mean);
//        }
//
//        return (float)Math.sqrt(sum / values.length);
    }
}