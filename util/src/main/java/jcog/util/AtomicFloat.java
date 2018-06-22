package jcog.util;

import jcog.math.FloatSupplier;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import static java.lang.Float.floatToIntBits;
import static java.lang.Float.intBitsToFloat;

/** https:
 * warning: do not call int get() or set(int), and
 * mostly all other of the superclass methods
 * since this will invoke the superclass's final methods
 * that i cant override and stub out
 * this int value is the coded representation of the float as an integer
 *
 * instead use floatValue(), intValue()
 *
 * sorry
 */
public class AtomicFloat extends Number implements FloatSupplier {

    private static final AtomicFloatFieldUpdater<AtomicFloat> _f =
            new AtomicFloatFieldUpdater(
                    AtomicIntegerFieldUpdater.newUpdater(AtomicFloat.class, "f"));

    private volatile int f;

    public AtomicFloat() {
        this(0f);
    }

    public AtomicFloat(float initialValue) {
        set(initialValue);
    }

    public final boolean compareAndSet(float expect, float update) {
        return _f.compareAndSet(this, expect, update);
    }


    public final void set(float newValue) {
        f = floatToIntBits(newValue);
    }

    public final float floatValue() {
        return intBitsToFloat(f);
    }

    public final float getAndSet(float newValue) {
        return _f.getAndSet(this, newValue);
    }

    public final float getAndZero() {
        return _f.getAndZero(this);
    }

    public final void zero(FloatConsumer with) {
        _f.zero(this, with);
    }

    public final void zeroIfNonZero(FloatConsumer with) {
        _f.zeroIfNonZero(this, with);
    }
    protected final float getAndZero(FloatConsumer with) {
        return _f.getAndZero(this, with);
    }


    @Override
    public String toString() {
        return String.valueOf(floatValue());
    }

    @Override
    public double doubleValue() { return floatValue(); }

    @Override
    public int intValue()       { return Math.round(floatValue());  }

    @Override
    public long longValue() {
        return Math.round(floatValue());
    }

    public void add(float x) {
        _f.add(this,x);
    }
    protected void addUpdate(float v, Runnable r) {
        _f.addUpdate(this, v, r);
    }

    @Override
    public float asFloat() {
        return floatValue();
    }
}