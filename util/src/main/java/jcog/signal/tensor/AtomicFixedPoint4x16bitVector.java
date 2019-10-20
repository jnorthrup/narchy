package jcog.signal.tensor;

import jcog.data.atomic.MetalAtomicLongFieldUpdater;
import jcog.pri.op.PriReturn;
import jcog.util.FloatFloatToFloatFunction;
import org.jetbrains.annotations.Nullable;

import java.util.StringJoiner;
import java.util.stream.IntStream;

import static jcog.Texts.n4;

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
    public static final float SHORT_TO_FLOAT_SCALE = Short.MAX_VALUE*2 + 1;
    private static final MetalAtomicLongFieldUpdater<AtomicFixedPoint4x16bitVector> X =
        new MetalAtomicLongFieldUpdater<>(AtomicFixedPoint4x16bitVector.class, "x");
    private volatile long x;

    /**
     * @param c quad selector: 0, 1, 2, 3
     */
    private static float toFloat(long x, int c) {
        return toFloat(shortAt(x, c));
    }

    private static int shortAt(long x, int c) {
        return ((int) (x >>> (c * 16))) & '\uffff';
    }

    static float toFloat(long s) {
        return s / SHORT_TO_FLOAT_SCALE;
    }

    static int toShort(float f) {
        return ((int) (f * SHORT_TO_FLOAT_SCALE));
    }

    @Override
    public void fill(float x) {
        long pattern = toShort(x);
        X.set(this, pattern | (pattern << 16) | (pattern << 32) | (pattern << 48) );
    }

    @Override
    public float sumValues() {
        long x = X.get(this), s = 0;
        for (var i = 0; i < 4; i++)
            s += shortAt(x, i);
        return toFloat(s);
    }

    @Override
    public String toString() {
        var joiner = new StringJoiner(",");
        IntStream.range(0,4).forEach(i-> joiner.add(toString(i)));
        return joiner.toString();
    }

    private String toString(int component) {
        return n4(getAt(component));
    }

    @Override public final float merge(int c, float arg, FloatFloatToFloatFunction F, @Nullable PriReturn returning) {
        var shift = c * 16;
        var mask = ~((((long)('\uffff'))) << shift);
        long _x, _y;
        float x, y;

        do {
            _x = X.get(this);

            var xi = (int) (_x >> shift) & '\uffff'; //shortAt(_x, c)
            x = toFloat(xi);

            y = F.apply(x, arg);

            var yi = toShort(y);
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
        var shift = linearCell * 16;
        var mask = ~((((long)('\uffff'))) << shift);
        var b = ((long)toShort(newValue))<<shift;
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
