package jcog.math;

import com.google.common.util.concurrent.AtomicDouble;
import jcog.signal.tensor.RingBufferTensor;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;

/**
 *
 *  exponential moving average with fixed rolling window (thread-safe)
 *  * <p>
 *  * https://en.wikipedia.org/wiki/Exponential_smoothing
 *  * https://dsp.stackexchange.com/a/20336
 *  * <p>
 * */
public class FloatAveragedWindow implements FloatSupplier, FloatToFloatFunction {

    private final FloatRange alpha;
    final RingBufferTensor window;
    final AtomicDouble current = new AtomicDouble(Double.NaN);


    public FloatAveragedWindow(int windowSize, float alpha) {
        this(windowSize, new FloatRange(alpha, 0, 1f));
    }

    public FloatAveragedWindow(int windowSize, FloatRange alpha) {
        this.window = new RingBufferTensor(1, windowSize);
        window.fill(Float.NaN);
        this.alpha = alpha;
    }

    public final float asFloat(FloatSupplier next) {
        put(next);
        return asFloat();
    }

    @Override public final float valueOf(float next) {
        put(next);
        return asFloat();
    }
    public final float valueOf(float next, boolean recalc) {
        put(next);
        return recalc ? asFloat() : (float) asCached();
    }

    public final void put(FloatSupplier next) {
        put(next.asFloat());
    }

    public void put(float v) {
        if (v == v) {
            //TODO make AtomicRingBufferTensor
            window.commit(new float[] {  v });
            invalidate();
        }
    }

    private void invalidate() {
        current.set(Double.NaN);
    }

    protected double calculate() {
        //TODO dont synch
        float a = alpha.get();

        final double[] next = {Double.NaN};

//        synchronized (window) {
            //TODO option for reverse calculation?

            //TODO Tensor reduce function
            window.forEach((int i, float n)->{
                if (n != n)
                    return;

                double prev = next[0];
                next[0] = (prev==prev) ?
                    ((1 - a) * prev) + (a * n)
                    :
                    n; //initial value
            });
            return next[0];
//        }
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

    protected double asCached() {
        return current.get();
    }

//        @Override
//        public float valueOf(float x) {
//
//
//            synchronized (this) {
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
//            }
//        }
//
//        /** previous value computed by valueOf */
//        public float floatValue() {
//            return prev;
//        }
}
