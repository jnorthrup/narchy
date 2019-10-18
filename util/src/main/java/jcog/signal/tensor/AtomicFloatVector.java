package jcog.signal.tensor;

import jcog.data.atomic.AtomicFloatFieldUpdater;
import jcog.pri.op.PriReturn;
import jcog.util.FloatFloatToFloatFunction;

import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.lang.Float.floatToIntBits;
import static java.lang.Float.intBitsToFloat;

/** stores 32-bit float values in AtomicIntegerArray */
public class AtomicFloatVector extends AbstractVector implements WritableTensor {

    public static final AtomicFloatVector Empty = new AtomicFloatVector(0);

    private final AtomicIntegerArray data;

    public AtomicFloatVector(int length) {
        this(length, 0);
    }

    /** the initial data will be zero */
    public AtomicFloatVector(int length, float initialValue) {
        this.data = new AtomicIntegerArray(length);
        fill(initialValue);
    }


    @Override
    public final float getAt(int linearCell) {
        return Float.intBitsToFloat( data.getOpaque(linearCell) );
    }

    public float getAndSet(int i, float v) {
        return Float.intBitsToFloat( data.getAndSet(i, Float.floatToIntBits(v) ) );
    }

    public float getAndZero(int i) {
        return Float.intBitsToFloat( data.getAndSet(i, AtomicFloatFieldUpdater.iZero ) );
    }

    @Override
    public final void setAt(int linearCell, float newValue) {
        data.set(linearCell, floatToIntBits(newValue));
    }

    public final void setAtLazy(float newValue, int linearCell) {
        data.lazySet(linearCell, floatToIntBits(newValue));
    }

    /** @see jcog.data.atomic.AtomicFloatFieldUpdater */
    @Override public final float addAt(float x, int linearCell) {
        if (Math.abs(x) < Float.MIN_NORMAL)
            return 0; //no effect

        AtomicIntegerArray data = this.data;

        float nextFloat;
        int prev, next;
        do {
            prev = data.getAcquire(linearCell);
            next = floatToIntBits(nextFloat = (intBitsToFloat(prev) + x)); //next = floatToIntBits(f.apply(intBitsToFloat(prev), y));
        } while (prev!=next && data.compareAndExchangeRelease(linearCell, prev, next)!=prev);
        return nextFloat;
    }

    @Override public final float merge(int linearCell, float arg, FloatFloatToFloatFunction x, PriReturn returning) {
        int prevI, nextI;
        float prev, next;
        AtomicIntegerArray data = this.data;

        do {
            prevI = data.getAcquire(linearCell);
            prev = intBitsToFloat(prevI);
            next = x.apply(prev, arg);
            nextI = floatToIntBits(next);
        } while(prevI!=nextI && data.compareAndExchangeRelease(linearCell, prevI, nextI)!=prevI);

        return returning.apply(arg, prev, next);
    }

    @Override
    public final int volume() {
        return data.length();
    }

    @Override
    public void fill(float x) {
        int xx = floatToIntBits(x);
        int v = volume();
        AtomicIntegerArray data = this.data;
        for (int i = 0; i < v; i++)
            data.set(i, xx);
    }

    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(",");
        int bound = volume();
        for (int x = 0; x < bound; x++) {
            String s = Float.toString(
                    getAt(x)
            );
            joiner.add(s);
        }
        return joiner.toString();
    }


}
