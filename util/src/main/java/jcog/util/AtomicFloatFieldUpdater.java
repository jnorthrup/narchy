package jcog.util;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/** @see AtomicFloat */
public class AtomicFloatFieldUpdater<X>  {

    private final AtomicIntegerFieldUpdater<X> updater;

    /** for whatever reason, the field updater needs constructed from within the target class
     * so it must be passed as a parameter here.
     */
    public AtomicFloatFieldUpdater(AtomicIntegerFieldUpdater<X> u) {
        this.updater = u;
    }


    public void set(X x, float value) {
        updater.set(x, ivalue(value));
    }

    public void add(X x, float add) {
        updater.updateAndGet(x, v-> ivalue(fvalue(v) + add));
    }

    public float getAndSet(X x, float value) {
        return fvalue(updater.getAndSet(x, ivalue(value)));
    }

    public float getAndZero(X x) {
        return fvalue(updater.getAndSet(x, ZERO));
    }

    public float get(X x) {
        return fvalue(updater.get(x));
    }

    final static int ZERO = Float.floatToIntBits(0f);
    static int ivalue(float x) { return Float.floatToIntBits(x); }
    static float fvalue(int x) { return Float.intBitsToFloat(x); }

}
