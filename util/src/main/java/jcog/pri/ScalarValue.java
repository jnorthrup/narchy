package jcog.pri;

import jcog.Util;
import jcog.WTF;
import jcog.data.atomic.AtomicFloatFieldUpdater;
import jcog.util.FloatFloatToFloatFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import static java.lang.Float.floatToIntBits;
import static java.lang.Float.intBitsToFloat;

/**
 * general purpose value.  consumes and supplies 32-bit float numbers
 * supports certain numeric operations on it
 * various storage implementations are possible
 * as well as the operation implementations. */
public interface ScalarValue {

    /**
     * global minimum difference necessary to indicate a significant modification in budget float number components
     */
    float EPSILON =             0.00001f;

    /** setter
     *  @return value after set
     * */
    float pri(float p);

    /** getter.  returns NaN to indicate deletion */
    float pri();


    default float pri(FloatToFloatFunction update) {
        return pri(update.valueOf(pri()));
    }
    default float pri(FloatFloatToFloatFunction update, float x) {
        return pri(update.apply(pri(), x));
    }

    /**
     * the result of this should be that pri() is not finite (ex: NaN)
     * returns false if already deleted (allowing overriding subclasses to know if they shold also delete)
     */
    default boolean delete() {
        float p = pri();
        if (p==p) {
            this.pri(Float.NaN);
            return true;
        }
        return false;
    }

//    @Deprecated private static float priElseZero(ScalarValue x) {
//        float p = x.pri();
//        return (p==p) ? p : 0;
//    }

    default float priMax(float _max) {
        //pri(Math.max(priElseZero(this), max));
        return pri((x, max) -> Math.max(max, (x!=x) ? 0 : x), _max);
    }

    default float priMin(float _min) {
        return pri((x, min) -> Math.min(min, (x!=x) ? 0 : x), _min);
    }

    default float priAdd(float _y) {
        return pri((x,y)->{
            if (x != x) {
                //if deleted..
                if (y <= 0)
                    return Float.NaN; //remains deleted by negative addend

                x = 0; //undeleted by positive addend
            }

            return x + y;
        }, _y);
    }

    default float priSub(float toSubtract) {
        assert (toSubtract >= 0) : "trying to subtract negative priority: " + toSubtract;

        return priAdd(-toSubtract);
    }

    default float priMult(float _y) {
        return pri((x,y)-> (x == x) ? (x * y) : Float.NaN, _y);
    }

    /** y should be in domain (0...1) - for decaying result */
    default float priMult(float _y, float applyIfGreaterThan) {
        return pri((x,y)-> (x == x) ?
                ( x > applyIfGreaterThan ? Math.max(applyIfGreaterThan, (x * y)) : x)
                :
                Float.NaN,
        _y);
    }



    /**
     * assumes 1 max value (Plink not NLink)
     */
    default float priAddOverflow(float inc /* float upperLimit=1 */) {

        if (Math.abs(inc) <= EPSILON) {
            return 0;
        }

        final float[] before = new float[1];
        float after = pri((x,y)->{

            if (x!=x)
                x = 0;
            before[0] = x;

            return Util.min(1f, x + y);

        }, inc);

        float delta = after - before[0];

        return inc - delta;
    }


    class PlainScalarValue implements ScalarValue {
        private float pri;

        @Override
        public float pri(float p) {
            return this.pri = p;
        }

        @Override
        public final float pri() {
            return pri;
        }
    }

    class AtomicScalarValue implements ScalarValue {
        protected static final AtomicFloatFieldUpdater<AtomicScalarValue> FLOAT =
                new AtomicFloatFieldUpdater(AtomicIntegerFieldUpdater.newUpdater(AtomicScalarValue.class, "pri"));

        private static final VarHandle INT;

        static {
            try {
                INT = MethodHandles.lookup().in(AtomicScalarValue.class)
                    .findVarHandle(AtomicScalarValue.class,"pri",int.class);
            } catch (Exception e) {
                throw new WTF(e);
            }
        }


        final static int NaN = floatToIntBits(Float.NaN);

        private volatile int pri;


        public final float priElseZero() {
            int i = _pri();
            return i == NaN ? 0 : intBitsToFloat(i);
        }

        @Override
        public boolean delete() {
            return ((int)INT.getAndSet(this, NaN)) != NaN;
            //if the above doesnt work, try converting with intToFloatBits( then do NaN test for equality etc
        }

        /** post-filter */
        public float v(float x) {
            return x;
        }

        private int _pri() {

            return (int) INT.getOpaque(this);
            //return (int) INT.get(this);
        }

        @Override
        public final float pri() {
            return intBitsToFloat( _pri() );
        }

        public boolean isDeleted() {
            return _pri() == NaN;
        }



        /** set */
        @Override public float pri(float p) {
            INT.set(this, floatToIntBits(v(p)));
            return p;
        }

        /** update */
        @Override public final float pri(FloatToFloatFunction update) {
            return FLOAT.updateAndGet(this, (x)-> v(update.valueOf(x)) );
        }

        /** update */
        @Override public final float pri(FloatFloatToFloatFunction update, float x) {
            return FLOAT.updateAndGet(this, (xx,yy)->v(update.apply(xx,yy)), x);
        }
    }
}
