package jcog.math;

import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Float.floatToIntBits;
import static java.lang.Float.intBitsToFloat;

/** https://stackoverflow.com/a/5505512
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
public class AtomicFloat extends AtomicInteger {

    public AtomicFloat() {
        this(0f);
    }

    public AtomicFloat(float initialValue) {
        super(floatToIntBits(initialValue));
    }

    public final boolean compareAndSet(float expect, float update) {
        return this.compareAndSet(floatToIntBits(expect),
                                  floatToIntBits(update));
    }




    public final void set(float newValue) {
        this.set(floatToIntBits(newValue));
    }

    @Override
    public float floatValue() {
        return intBitsToFloat(get());
    }

    public final float getAndSet(float newValue) {
        return intBitsToFloat(this.getAndSet(floatToIntBits(newValue)));
    }

    public final boolean weakCompareAndSet(float expect, float update) {
        return this.weakCompareAndSet(floatToIntBits(expect),
                                      floatToIntBits(update));
    }

    @Override
    public String toString() {
        return String.valueOf(floatValue());
    }


    @Override
    public double doubleValue() { return floatValue(); }
    @Override
    public int intValue()       { return Math.round(floatValue());  }

    public float addAndGet(float x) {
        return updateAndGet((i)-> floatToIntBits(intBitsToFloat(i) + x ));
    }

}