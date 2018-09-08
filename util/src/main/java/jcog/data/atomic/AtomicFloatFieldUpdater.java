package jcog.data.atomic;

import jcog.util.FloatConsumer;
import jcog.util.FloatFloatToFloatFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToIntFunction;

import java.util.function.IntUnaryOperator;

import static java.lang.Float.floatToIntBits;
import static java.lang.Float.intBitsToFloat;

/** @see AtomicFloat */
public final class AtomicFloatFieldUpdater<X>  {

    private final static int NAN = floatToIntBits(Float.NaN);
    private final static int ZERO = floatToIntBits(0f);
    public final MetalAtomicIntegerFieldUpdater<X> updater;


//    /** for whatever reason, the field updater needs constructed from within the target class
//     * so it must be passed as a parameter here.
//     * ex: AtomicIntegerFieldUpdater.newUpdater(AtomicFloat.class, "f")
//     */
//    @Deprecated public AtomicFloatFieldUpdater(AtomicIntegerFieldUpdater<X> u) {
//        this.updater = u;
//    }

    public AtomicFloatFieldUpdater(Class<X> cl, String f) {
        this.updater = new MetalAtomicIntegerFieldUpdater<>(cl, f);
    }

    public void setNaN(X x) {
        updater.set(x, NAN);
    }

    public void set(X x, float value) {
        updater.set(x, floatToIntBits(value));
    }

//    public void lazySet(X x, float value) {
//        updater.lazySet(x, floatToIntBits(value));
//    }

    public void add(X x, float add) {
        updater.updateAndGet(x, v -> floatToIntBits(intBitsToFloat(v) + add));
    }

    private float updateGet(X x, IntUnaryOperator y) {
        return intBitsToFloat(updater.updateAndGet(x, y));
    }

    private void update(X x, IntUnaryOperator y) {
        updater.updateAndGet(x, y);
    }


    public float updateIntAndGet(X x, FloatToIntFunction f) {
        return updateGet(x, v -> f.valueOf(intBitsToFloat(v)));
    }

    public float updateAndGet(X x, FloatToFloatFunction f) {
        return updateGet(x, v -> floatToIntBits(f.valueOf(intBitsToFloat(v))));
    }

    public float updateAndGet(X x, FloatFloatToFloatFunction f, float y) {
        return updateGet(x, v -> floatToIntBits(f.apply(intBitsToFloat(v), y)));
    }

    public void update(X x, FloatFloatToFloatFunction f, float y) {
        update(x, v -> floatToIntBits(f.apply(intBitsToFloat(v), y)));
    }

    public float getAndSet(X x, float value) {
        return intBitsToFloat(updater.getAndSet(x, floatToIntBits(value)));
    }


    public float getAndZero(X x) {
        return intBitsToFloat(updater.getAndSet(x, ZERO));
    }

    public void zero(X x) {
        updater.set(x, ZERO);
    }

    public float get(X x) {
        return get(updater.get(x));
    }
    public float getOpaque(X x) {
        return get(updater.getOpaque(x));
    }

    public static float get(int x) {
        return intBitsToFloat(x);
    }

    public void zero(X v, FloatConsumer with) {
        this.updater.getAndUpdate(v, x->{
            with.accept(intBitsToFloat(x));
            return AtomicFloatFieldUpdater.ZERO;
        });
    }


    float getAndZero(X v, FloatConsumer with) {
        return intBitsToFloat(this.updater.getAndUpdate(v, (x)->{ with.accept(intBitsToFloat(x)); return AtomicFloatFieldUpdater.ZERO; } ));
    }

    public boolean compareAndSet(X x, float expected, float newvalue) {
        return updater.compareAndSet(x, floatToIntBits(expected), floatToIntBits(newvalue));
    }

    /** unary */
    public float updateAndGet(X x, FloatToFloatFunction update, FloatToFloatFunction post) {
        return updateAndGet(x, (v)-> post.valueOf(update.valueOf(v)));
    }
//    public float updateIntAndGet(X x, FloatToFloatFunction update, FloatToIntFunction post) {
//        return updateIntAndGet(x, (v)-> post.valueOf(update.valueOf(v)));
//    }

    /** unary + arg */
    public float updateAndGet(X x, float arg, FloatFloatToFloatFunction update, FloatToFloatFunction post) {
        return updateAndGet(x, (xx,yy)-> post.valueOf(update.apply(xx,yy)), arg);
    }

    /** unary + arg */
    public void update(X x, float arg, FloatFloatToFloatFunction update, FloatToFloatFunction post) {
        update(x, (xx,yy)-> post.valueOf(update.apply(xx,yy)), arg);
    }



}
