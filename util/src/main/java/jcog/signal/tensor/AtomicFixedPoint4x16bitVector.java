package jcog.signal.tensor;

import jcog.data.atomic.MetalAtomicLongFieldUpdater;
import jcog.pri.op.PriReturn;
import jcog.util.FloatFloatToFloatFunction;
import org.jetbrains.annotations.Nullable;

import java.util.StringJoiner;

import static jcog.Texts.*;

/**
 * stores 4 (four) 16-bit fixed-point numbers, covering a unit range [0..1.0]
 * TODO use VarHandle with offsets to CAS the specific sub-vector.  see
 *             XX = MethodHandles.arrayElementVarHandle(X[].class);
 *             .getAndSet(a, (cap - 1) & --s, null)
 *             VarHandleBytes
 * TODO test
 * TODO make an array version of this using common static methods
 */
public class AtomicFixedPoint4x16bitVector implements WritableTensor {

    //TODO atomic addAt methods
    private static final int[] QUAD_16_SHAPE = { 4 };
    public static final float SHORT_TO_FLOAT_SCALE = (float) (Short.MAX_VALUE * 2 + 1);
    private static final MetalAtomicLongFieldUpdater<AtomicFixedPoint4x16bitVector> X =
        new MetalAtomicLongFieldUpdater<>(AtomicFixedPoint4x16bitVector.class, "x");
    private volatile long x;

    /**
     * @param c quad selector: 0, 1, 2, 3
     */
    private static float toFloat(long x, int c) {
        return toFloat((long) shortAt(x, c));
    }

    private static int shortAt(long x, int c) {
        return ((int) (x >>> (c * 16))) & (int) '\uffff';
    }

    static float toFloat(long s) {
        return (float) s / SHORT_TO_FLOAT_SCALE;
    }

    static int toShort(float f) {
        return ((int) (f * SHORT_TO_FLOAT_SCALE));
    }

    @Override
    public void fill(float x) {
        long pattern = (long) toShort(x);
        X.set(this, pattern | (pattern << 16) | (pattern << 32) | (pattern << 48) );
    }

    @Override
    public float sumValues() {
        long x = X.get(this), s = 0L;
        for (int i = 0; i < 4; i++)
            s = s + (long) shortAt(x, i);
        return toFloat(s);
    }

    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(",");
        for (int i = 0; i < 4; i++) {
            joiner.add(toString(i));
        }
        return joiner.toString();
    }

    private String toString(int component) {
        return INSTANCE.n4(getAt(component));
    }

    @Override public final float merge(int c, float arg, FloatFloatToFloatFunction F, @Nullable PriReturn returning) {
        int shift = c * 16;
        long mask = ~((((long)('\uffff'))) << shift);
        long _x, _y;
        float x, y;

        do {
            _x = X.get(this);

            int xi = (int) (_x >> shift) & (int) '\uffff'; //shortAt(_x, c)
            x = toFloat((long) xi);

            y = F.apply(x, arg);

            int yi = toShort(y);
            if (xi == yi) {
                y = x; //no change
                break;
            }

            _y = (_x & mask) | (((long)yi) << shift);

        } while (!X.compareAndSet(this, _x, _y));

        return returning!=null ? returning.apply(arg, x, y) : Float.NaN;
    }

    @Override
    public final void setAt(int linearCell, float newValue) {
        int shift = linearCell * 16;
        long mask = ~((((long)('\uffff'))) << shift);
        long b = ((long)toShort(newValue))<<shift;
        long x, y;
        do {
            y = ((x = X.get(this)) & mask) | b;
        } while(x!=y && !X.compareAndSet(this, x, y));
    }

    @Override
    public final float getAt(int linearCell) {
        return toFloat(X.get(this), linearCell);
    }

    @Override
    public final int[] shape() {
        return QUAD_16_SHAPE;
    }

    public final long data() {
        return X.get(this);
    }

    public final void data(long y) {
        X.set(this, y);
    }

}
