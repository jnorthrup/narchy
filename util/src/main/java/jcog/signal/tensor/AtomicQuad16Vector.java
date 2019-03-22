package jcog.signal.tensor;

import java.util.concurrent.atomic.AtomicLongFieldUpdater;

/** stores 4 (four) 16-bit fixed-point numbers, covering a unit range [0..1.0]
 * TODO test
 * */
public class AtomicQuad16Vector implements WritableTensor {

    private volatile long x;

    //TODO atomic addAt methods

    @Override
    public void setAt(float newValue, int linearCell) {
        X.accumulateAndGet(this,  setFloatMask(newValue, linearCell),
            (long x, long mask) -> x & mask);
    }

    @Override
    public float getAt(int linearCell) {
        return selectFloat(X.get(this), linearCell);
    }

    /** @param c quad selector: 0, 1, 2, 3 */
    static float selectFloat(long x, int c) {
        return toFloat( ((x >> (c *16))&0xffff));
    }

    /** produces a mask which will set one specific quad (via c) by AND'd it with the field */
    static long setFloatMask(float v, int c) {
        return ~0L & ~(((long) toShort(v)) << (c*16));
    }

    private static float toFloat(long s) {
        return Short.toUnsignedInt((short)s) / SHORT_TO_FLOAT_SCALE;
    }
    private static int toShort(float f) {
        return Math.round(f * SHORT_TO_FLOAT_SCALE);
    }

    @Override
    public int[] shape() {
        return QUAD_16_SHAPE;
    }

    static final float SHORT_TO_FLOAT_SCALE = Short.MAX_VALUE + -Short.MIN_VALUE;
    static final int[] QUAD_16_SHAPE = new int[4];

    private static final AtomicLongFieldUpdater<AtomicQuad16Vector> X =
            AtomicLongFieldUpdater.newUpdater(AtomicQuad16Vector.class, "x");

}
