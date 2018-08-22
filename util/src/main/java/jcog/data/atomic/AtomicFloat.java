package jcog.data.atomic;

import jcog.data.NumberX;
import jcog.math.FloatSupplier;
import jcog.util.FloatConsumer;

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
public class AtomicFloat extends NumberX implements FloatSupplier {

    private static final AtomicFloatFieldUpdater<NumberX> F =
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
        return F.compareAndSet(this, expect, update);
    }


    public void set(float newValue) {
        f = floatToIntBits(newValue);
    }

    public final float floatValue() {
        return intBitsToFloat(f);
    }

    public final float getAndSet(float newValue) {
        return F.getAndSet(this, newValue);
    }

    public final float getAndZero() {
        return F.getAndZero(this);
    }

    public final void zero(FloatConsumer with) {
        F.zero(this, with);
    }


    protected final float getAndZero(FloatConsumer with) {
        return F.getAndZero(this, with);
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
        F.add(this,x);
    }



    public float multiply(float _y) {
        return F.updateAndGet(this, (x,y)->x*y, _y);
    }

    @Override
    public float asFloat() {
        return floatValue();
    }


    /**
     * Sets the value from any Number instance.
     *
     * @param value  the value to set, not null
     * @throws NullPointerException if the object is null
     */
    public final void set(final Number value) {
        set(value.floatValue());
    }

    public float get() {
        return floatValue();
    }


}