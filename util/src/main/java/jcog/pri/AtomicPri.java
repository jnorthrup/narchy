package jcog.pri;

import jcog.WTF;
import jcog.data.atomic.AtomicFloatFieldUpdater;
import jcog.math.FloatSupplier;
import jcog.util.FloatFloatToFloatFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

import static java.lang.Float.floatToIntBits;
import static java.lang.Float.intBitsToFloat;

public abstract class AtomicPri implements ScalarValue {
    private static final AtomicFloatFieldUpdater<jcog.pri.AtomicPri> PRI =
            new AtomicFloatFieldUpdater(jcog.pri.AtomicPri.class, "pri");

    private static final VarHandle INT;

    static {
        try {
            INT = MethodHandles.lookup().in(jcog.pri.AtomicPri.class)
                .findVarHandle(jcog.pri.AtomicPri.class,"pri",int.class);
        } catch (Exception e) {
            throw new WTF(e);
        }
    }


    private final static int iNaN = floatToIntBits(Float.NaN);

    /** initialized to zero */
    private volatile int pri = floatToIntBits(0);

    @Override
    public String toString() {
        return String.valueOf(pri());
    }

    public final float priElseZero() {
        int i = _pri();
        return i == iNaN ? 0 : intBitsToFloat(i);
    }

    @Override
    public boolean delete() {
        return ((int)INT.getAndSet(this, iNaN)) != iNaN;
        //if the above doesnt work, try converting with intToFloatBits( then do NaN test for equality etc
    }

    /** post-filter */
    abstract public float v(float x);

    /** allows NaN */
    private float _v(float x) {
        return x!=x ? Float.NaN : v(x);
    }

    private int _pri() {
        return (int) INT.getOpaque(this);
        //return (int) INT.get(this);
    }

    @Override
    public float pri() {
        return intBitsToFloat( _pri() );
    }
    public final int priComparable() {
        return _pri();
    }

    public boolean isDeleted() {
        return _pri() == iNaN;
    }



    /** set */
    @Override public float pri(float p) {
        INT.set(this, floatToIntBits(v(p)));
        return p;
    }

    /** update */
    @Override public final float pri(FloatSupplier update) {
        return PRI.updateAndGet(this, update);
    }

    /** update */
    @Override public final float pri(FloatToFloatFunction update) {
        return PRI.updateAndGet(this, update, this::_v);
    }

    /** update */
    @Override public final float pri(FloatFloatToFloatFunction update, float x) {
        return PRI.updateAndGet(this, x, update, this::_v);
    }

    @Override
    public final void priUpdate(FloatFloatToFloatFunction update, float x) {
        PRI.update(this, x, update, this::_v);
    }
}
