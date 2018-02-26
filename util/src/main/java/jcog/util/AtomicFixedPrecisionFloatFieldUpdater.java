package jcog.util;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/** represents a float value as an integer by multiplying
 * by a specific factor.
 * TODO an alternate way is how AtomicFloat is implemented by converting
 * the float to its canonical integer representation directly.
 */
public class AtomicFixedPrecisionFloatFieldUpdater<X>  {

    private final AtomicIntegerFieldUpdater<X> updater;
    private final int multiplier;

    public AtomicFixedPrecisionFloatFieldUpdater(AtomicIntegerFieldUpdater<X> u, int multiplier) {
        this.updater = u;
        assert(multiplier > 1);
        this.multiplier = multiplier;
    }

    public void set(X x, float value) {
        updater.set(x, ivalue(value));
    }

    public void add(X x, float value) {
        updater.addAndGet(x, ivalue(value));
    }

    public float getAndSet(X x, float value) {
        return fvalue(updater.getAndSet(x, ivalue(value)));
    }

    int ivalue(float x) { return (int)(x * multiplier); }
    float fvalue(int x) { return ((float)x) / multiplier; }

    public float get(X x) {
        return fvalue(updater.get(x));
    }
}
