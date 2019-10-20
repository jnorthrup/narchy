package jcog.pri;

import jcog.Util;
import jcog.data.atomic.AtomicFloatFieldUpdater;
import jcog.util.FloatFloatToFloatFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;

import static java.lang.Float.floatToIntBits;
import static jcog.data.atomic.AtomicFloatFieldUpdater.iNaN;
import static jcog.data.atomic.AtomicFloatFieldUpdater.iZero;

public abstract class AtomicPri implements ScalarValue {
    private static final AtomicFloatFieldUpdater<jcog.pri.AtomicPri> PRI =
            new AtomicFloatFieldUpdater(jcog.pri.AtomicPri.class, "pri");

//    private static final VarHandle INT;
//
//    static {
//        try {
//            INT = MethodHandles.lookup().in(jcog.pri.AtomicPri.class)
//                .findVarHandle(jcog.pri.AtomicPri.class,"pri",int.class);
//        } catch (Exception e) {
//            throw new WTF(e);
//        }
//    }



    /** initialized to zero */
    public volatile int pri = iZero;
    static final FloatToFloatFunction vNonZero = AtomicPri::_vNonZero;
    static final FloatToFloatFunction vUnit = AtomicPri::_vUnit;


    /** initialized to zero */
    public AtomicPri() {

    }

    public AtomicPri(float p) {
        pri(p);
//        if (p == p) {
//            if (p == 0f)
//                return; //HACK default is already zero
//            pri(p);
//        } else {
//            //start deleted
//            PRI.INT.lazySet(this, iNaN); //HACK
//        }
    }

    public AtomicPri(Prioritized x) {
        this(x.pri());
    }

    @Override
    public String toString() {
        return String.valueOf(pri());
    }

    @Override
    public final float priElse(float valueIfDeleted) {
        float f = pri();
        return f == f ? f : valueIfDeleted;
    }

    @Override
    public final float priGetAndSetZero() {
        return PRI.getAndSetZero(this);
    }


    @Override
    public final float priGetAndDelete() {
        return PRI.getAndSetNaN(this);
    }

    @Override
    public boolean delete() {
        return PRI.INT.getAndSet(this, iNaN) != iNaN;
        //return ((int)INT.getAndSet(this, iNaN)) != iNaN;
        //if the above doesnt work, try converting with intToFloatBits( then do NaN test for equality etc
    }


    /** allows NaN */
    private static float _vAny(float x) {
        return x!=x ? Float.NaN : x;
    }
    private static float _vNonZero(float x) {
        return x!=x ? Float.NaN : Math.max((float) 0, x);
    }
    /** allows NaN */
    private static float _vUnit(float x) {
        return x!=x ? Float.NaN : Util.unitize(x);
    }
    private static final FloatFloatToFloatFunction priAddUpdateFunctionUnit = AtomicPri.post(priAddUpdateFunction,AtomicPri::_vUnit);
    private static final FloatFloatToFloatFunction priAddUpdateFunctionNonZero = AtomicPri.post(priAddUpdateFunction,AtomicPri::_vNonZero);
    private static final FloatFloatToFloatFunction priAddUpdateFunctionAny = AtomicPri.post(priAddUpdateFunction,AtomicPri::_vAny);

    private static final FloatFloatToFloatFunction priMulUpdateFunctionUnit = AtomicPri.post(priMulUpdateFunction,AtomicPri::_vUnit);
    private static final FloatFloatToFloatFunction priMulUpdateFunctionNonZero = AtomicPri.post(priMulUpdateFunction,AtomicPri::_vNonZero);

    private int _pri() {
        return PRI.INT.getOpaque(this);
        //return (int) INT.getOpaque(this);
        //return (int) INT.get(this);
    }

    @Override
    public float pri() {
        return PRI.INT.getFloat(this);
    }
    public final int priComparable() {
        return _pri();
    }

    public final boolean isDeleted() {
        return _pri() == iNaN;
    }

    @Override
    public final float priGetAndSet(float p) {
        return (float) PRI.INT.getAndSet(this, floatToIntBits(post().valueOf(p)));
    }

    @Override public float priSetAndGet(float p) {
        float v = post().valueOf(p);
        PRI.INT.set(this, v);
        return v;
    }

    /** set */
    @Override public final <P extends ScalarValue> P pri(float p) {
        PRI.INT.set(this, post().valueOf(p));
        return (P) this;
    }

//    /** update */
//    @Override public final float pri(FloatSupplier update) {
//        return PRI.updateAndGet(this, update);
//    }

//    /** update */
//    @Override public final float pri(FloatToFloatFunction update) {
//        return PRI.updateAndGet(this, update, post());
//    }

    /** update */
    @Override public final float pri(FloatFloatToFloatFunction update, float x) {
        return PRI.updateAndGet(this, x, update, post());
    }

    @Override
    public final float priMult(float a) {
        return PRI.updateAndGet(this, unit() ? priMulUpdateFunctionUnit : priMulUpdateFunctionNonZero, a);
    }

    @Override
    public final void priAdd(float a) {
        PRI.update(this, unit() ? priAddUpdateFunctionUnit : priAddUpdateFunctionNonZero, a);
    }

//    public final float priUpdateAndGet(FloatToFloatFunction update) {
//        return PRI.updateAndGet(this, update, post());
//    }

//    public final float priUpdateAndGet(FloatFloatToFloatFunction update, float x) {
//        return PRI.updateAndGet(this, x, update, post());
//    }

    @Override
    public final void priUpdate(FloatFloatToFloatFunction update, float x) {
        PRI.updateAndGet(this, x, update, post());
    }

    private static FloatFloatToFloatFunction post(FloatFloatToFloatFunction update, FloatToFloatFunction post) {
        return (xx,yy)-> post.valueOf(update.apply(xx,yy));
    }

    private FloatToFloatFunction post() {
        return unit() ? vUnit : vNonZero;
    }

    /** override and return true if the implementation clamps values to 0..+1 (unit) */
    protected boolean unit() {
        return false;
    }


}
