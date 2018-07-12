package jcog.pri;

import jcog.Util;
import jcog.util.AtomicFloatFieldUpdater;
import jcog.util.FloatFloatToFloatFunction;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

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

    @Deprecated private static float priElseZero(ScalarValue x) {
        float p = x.pri();
        return (p==p) ? p : 0;
    }

    default void priMax(float max) {
        pri(Math.max(priElseZero(this), max));
    }

    default void priMin(float min) {
        pri(Math.min(priElseZero(this), min));
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


    class VolatileScalarValue implements ScalarValue {
        private volatile float pri;

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
        protected static final AtomicFloatFieldUpdater<AtomicScalarValue> PRI =
                new AtomicFloatFieldUpdater(AtomicIntegerFieldUpdater.newUpdater(AtomicScalarValue.class, "pri"));

        private volatile int pri;

        @Override
        public float pri(float p) {
            PRI.set(this, p);
            return p;
        }

        @Override
        public final float pri() {
            return PRI.get(this);
        }

        @Override
        public final float pri(FloatToFloatFunction update) {
            return PRI.updateAndGet(this, update);
        }

        @Override
        public final float pri(FloatFloatToFloatFunction update, float x) {
            return PRI.updateAndGet(this, update, x);
        }
    }
}
