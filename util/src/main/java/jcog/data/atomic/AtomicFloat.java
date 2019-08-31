package jcog.data.atomic;

import jcog.data.NumberX;
import jcog.math.FloatSupplier;
import jcog.util.FloatConsumer;

/**
 * https:
 * warning: do not call int get() or setAt(int), and
 * mostly all other of the superclass methods
 * since this will invoke the superclass's final methods
 * that i cant override and stub out
 * this int value is the coded representation of the float as an integer
 * <p>
 * instead use floatValue(), intValue()
 * <p>
 * sorry
 * TODO maybe use ScalarValue
 */
public class AtomicFloat extends NumberX implements FloatSupplier {

    private static final AtomicFloatFieldUpdater<AtomicFloat> F =
            new AtomicFloatFieldUpdater<>(AtomicFloat.class, "f");

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
        F.set(this, newValue);
    }
    public void setLazy(float newValue) {
        F.setLazy(this, newValue);
    }

    public final float floatValue() {
        return F.getOpaque(this);
    }

    public final float getAndSet(float newValue) {
        return F.getAndSet(this, newValue);
    }

    public final float getAndZero() {
        return F.getAndSetZero(this);
    }

    public final void zero(FloatConsumer with) {
        F.zero(this, with);
    }


    protected final float getAndZero(FloatConsumer with) {
        return F.getAndSetZero(this, with);
    }


    @Override
    public String toString() {
        return String.valueOf(floatValue());
    }

    @Override
    public double doubleValue() {
        return floatValue();
    }

    @Override
    public int intValue() {
        return Math.round(floatValue());
    }

    @Override
    public long longValue() {
        return Math.round(floatValue());
    }

    public final void add(float x) {
        F.add(this, x);
    }


    public float multiply(float _y) {
        return F.updateAndGet(this, (x, y) -> x * y, _y);
    }

    @Override
    public float asFloat() {
        return floatValue();
    }


    /**
     * Sets the value from any Number instance.
     *
     * @param value the value to setAt, not null
     * @throws NullPointerException if the object is null
     */
    public final void set(final Number value) {
        set(value.floatValue());
    }

    public float get() {
        return floatValue();
    }


}