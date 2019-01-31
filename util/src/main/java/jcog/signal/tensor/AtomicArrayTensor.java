package jcog.signal.tensor;

import java.util.concurrent.atomic.AtomicIntegerArray;

import static java.lang.Float.floatToIntBits;
import static java.lang.Float.intBitsToFloat;

/** stores 32-bit float values in AtomicIntegerArray */
public class AtomicArrayTensor extends AbstractVector {
    private final AtomicIntegerArray data;

    public AtomicArrayTensor(int length) {
        this.data = new AtomicIntegerArray(length);
    }

    @Override
    public final float getAt(int linearCell) {
        return Float.intBitsToFloat( data.getOpaque(linearCell) );
    }

    @Override
    public final void setAt(float newValue, int linearCell) {
        data.set(linearCell, Float.floatToIntBits(newValue));
    }

    /** @see jcog.data.atomic.AtomicFloatFieldUpdater */
    @Override public final void addAt(float x, int linearCell) {
        int prev, next;
        do {
            prev = data.getAcquire(linearCell);
            next = floatToIntBits(intBitsToFloat(prev) + x); //next = floatToIntBits(f.apply(intBitsToFloat(prev), y));
        } while(!data.weakCompareAndSetRelease(linearCell, prev, next));
    }

    @Override
    public final int volume() {
        return data.length();
    }

    @Override
    public void fill(float x) {
        int xx = floatToIntBits(x);
        int v = volume();
        for (int i = 0; i < v; i++)
            data.set(i, xx);
    }

}
