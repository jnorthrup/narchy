package jcog.signal.tensor;

import jcog.util.FloatFloatToFloatFunction;

import java.util.concurrent.atomic.AtomicLongFieldUpdater;

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



    private static final float SHORT_TO_FLOAT_SCALE = Short.MAX_VALUE*2 + 1;

    //TODO atomic addAt methods
    static final int[] QUAD_16_SHAPE = new int[] { 4 };
    private static final AtomicLongFieldUpdater<AtomicFixedPoint4x16bitVector> X =
            AtomicLongFieldUpdater.newUpdater(AtomicFixedPoint4x16bitVector.class, "x");
    private volatile long x;

    /**
     * @param c quad selector: 0, 1, 2, 3
     */
    static float toFloat(long x, int c) {
        return toFloat(shortAt(x, c));
    }

    static int shortAt(long x, int c) {
        return ((int) (x >>> (c * 16))) & '\uffff';
    }

    @Override
    public void fill(float x) {
        long pattern = toShort(x);
        X.set(this, pattern | (pattern << 16) | (pattern << 32) | (pattern << 48) );
    }

    @Override
    public float sumValues() {

        long x = X.get(this);
        float /* double? */ s = 0;
        for (int i = 0; i < 4; i++)
            s += toFloat(x, i);
        return s;
    }


    public static float toFloat(long s) {
        return s / SHORT_TO_FLOAT_SCALE;
    }

    public static int toShort(float f) {
        return ((int) (f * SHORT_TO_FLOAT_SCALE));
    }

    @Override
    public String toString() {
        return getAtStr(0) + ',' + getAtStr(1) + ',' + getAtStr(2) + ',' + getAtStr(3);
    }

    private String getAtStr(int i) {
        return n4(getAt(i));
    }

    @Override public final float merge(int c, float arg, FloatFloatToFloatFunction F, boolean returnValueOrDelta) {
        int shift = c * 16;
        long mask = ~((((long)('\uffff'))) << shift);
        long _x, _y;
        float x, y;


        do {
            _x = X.get(this);

            int xi = (int) (_x >> shift) & '\uffff'; //shortAt(_x, c)
            x = toFloat(xi);

            y = F.apply(x, arg);

            int yi = toShort(y);
            if (xi == yi)
                return returnValueOrDelta ? x : 0; //no change

            _y = (_x & mask) | (((long)yi) << shift);

        } while(!X.compareAndSet(this, _x, _y));

        return returnValueOrDelta ? y : (y - x);
    }

    @Override
    public void setAt(int linearCell, float newValue) {
        int shift = linearCell * 16;
        long mask = ~((((long)('\uffff'))) << shift);
        long b = ((long)toShort(newValue))<<shift;
        long x, y;
        do {
            y = ((x = X.get(this)) & mask) | b;
        } while(x!=y && !X.compareAndSet(this, x, y));
    }

    @Override
    public float getAt(int linearCell) {
        return toFloat(X.get(this), linearCell);
    }

    @Override
    public int[] shape() {
        return QUAD_16_SHAPE;
    }

}
