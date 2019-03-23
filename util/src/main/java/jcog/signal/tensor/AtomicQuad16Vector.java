package jcog.signal.tensor;

import java.util.concurrent.atomic.AtomicLongFieldUpdater;

import static jcog.Texts.n4;

/**
 * stores 4 (four) 16-bit fixed-point numbers, covering a unit range [0..1.0]
 * TODO test
 */
public class AtomicQuad16Vector implements WritableTensor {

    static final float SHORT_TO_FLOAT_SCALE = Short.MAX_VALUE + -Short.MIN_VALUE - 1 /* margin */;

    //TODO atomic addAt methods
    static final int[] QUAD_16_SHAPE = new int[4];
    private static final AtomicLongFieldUpdater<AtomicQuad16Vector> X =
            AtomicLongFieldUpdater.newUpdater(AtomicQuad16Vector.class, "x");
    private volatile long x;

    /**
     * @param c quad selector: 0, 1, 2, 3
     */
    static float selectFloat(long x, int c) {
        return toFloat(((x >>> (c * 16))));
    }

    /**
     * produces a mask which will set one specific quad (via c) by AND'd it with the field
     *  Merge bits from two values according to a mask
     *
     * unsigned int a;    // value to merge in non-masked bits
     * unsigned int b;    // value to merge in masked bits
     * unsigned int mask; // 1 where bits from b should be selected; 0 where from a.
     * unsigned int r;    // result of (a & ~mask) | (b & mask) goes here
     *
     * r = a ^ ((a ^ b) & mask);
     *
     * This shaves one operation from the obvious way of combining two sets of bits according to a bit mask. If the mask is a constant, then there may be no advantage.
     */
//    static long setFloatMask(long a, int c) {
//        //return ~0L & ~(((long) toShort(v)) << (c * 16));
//        long b = (((long) toShort(v)) << (c * 16));
//        return a ^ ((a^b) & (Integer.MAX_VALUE << (c*16)));
//    }

    private static float toFloat(long s) {
        return Short.toUnsignedInt((short) s) / SHORT_TO_FLOAT_SCALE;
    }

    private static long toShort(float f) {
        return Math.round(f * SHORT_TO_FLOAT_SCALE);
    }

    @Override
    public String toString() {
        return getStr4at(0) + ',' + getStr4at(1) + ',' + getStr4at(2) + ',' + getStr4at(3);
    }

    protected String getStr4at(int i) {
        return n4(getAt(i));
    }

    @Override
    public void setAt(float newValue, int linearCell) {
        int shift = linearCell * 16;
        X.accumulateAndGet(this, toShort(newValue)<<shift,
                (long x, long b) -> {
                    return x ^ ((x^b) & (((short)-1) << shift));
                });
    }

    @Override
    public float getAt(int linearCell) {
        return selectFloat(X.get(this), linearCell);
    }

    @Override
    public int[] shape() {
        return QUAD_16_SHAPE;
    }

}
