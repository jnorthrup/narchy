package jcog.pri;

import jcog.Texts;
import jcog.Util;
import jcog.pri.op.PriMerge;
import jcog.pri.op.PriReturn;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.function.Function;

/**
 * Mutable Prioritized
 * implementations need only implement priSet() and pri()
 */
public interface Prioritizable extends Prioritized, ScalarValue {

    default Prioritizable pri(Prioritized p) {
        if (this != p)
            pri(p.pri());
        return this;
    }

    /** set priority at-least this value */
    default void priMax(float p) {
        priMax(p, PriReturn.Post);
    }

    default void priMin(float p) {
        priMin(p, PriReturn.Post);
    }

    default float priMax(float p, PriReturn mode) {
        return PriMerge.max.merge(this, p, mode);
    }

    default float priMin(float p, PriReturn mode) {
        return PriMerge.min.merge(this, p, mode);
    }

    default float take(Prioritizable source, float p, boolean amountOrFraction, boolean copyOrMove) {

        if (p!=p || p < ScalarValue.EPSILON)
            return 0; //amount is insignificant

        assert(this!=source);

        float amount;
        if (amountOrFraction) {
            float s = source.pri();
            if (s!=s || s < ScalarValue.EPSILON)
                return 0; //source is depleted

            amount = Math.min(s, p);
        } else {
            assert(p <= 1f);
            amount = source.priElseZero() * p;
            if (amount < ScalarValue.EPSILON)
                return 0; //request*source is insignificant
        }

        final float[] before = new float[1];

        float after = pri((x,a)->{
            float x1 = x;
            if (x1 != x1)
                x1 = 0;
            before[0] = x1;
            return x1 + a;
        }, amount);

        float b = before[0];
        if (b!=b)
            b = 0;

        float taken = after - b;

        if (!copyOrMove) {
//            float taken = source.priDelta((exist,subtracting)->{
//
//            }, amount);
            //TODO verify source actually had it.  this would involve somehow combining the 2 atomic ops
            source.priSub(taken);
        }

        return taken;
    }


    default <P extends Prioritizable> P withPri(float p) {
        pri(p);
        return (P) this;
    }


    /**
     * Briefly display the BudgetValue
     *
     * @return String representation of the value with 2-digit accuracy
     */
    default Appendable toBudgetStringExternal() {
        return toBudgetStringExternal(null);
    }

    default StringBuilder toBudgetStringExternal(StringBuilder sb) {
        return Prioritized.toStringBuilder(sb, Texts.n2(pri()));
    }

    default String toBudgetString() {
        return toBudgetStringExternal().toString();
    }



//    /**
//     * normalizes the current value to within: min..(range+min), (range=max-min)
//     */
//    default void normalizePri(float min, float range, float lerp) {
//        float p = priElseNeg1();
//        if (p < 0) return;
//
//        priLerp((p - min) / range, lerp);
//    }

//    default Priority priLerp(float target, float speed) {
//        float pri = pri();
//        if (pri == pri)
//            pri(lerp(speed, pri, target));
//        return this;
//    }

    static float fund(float maxPri, boolean copyOrTransfer, Prioritizable... src) {
        return fund(maxPri, copyOrTransfer, (x -> x), src);
    }

    /**
     * X[] may contain nulls
     */
    @SafeVarargs
    static <X> float fund(float maxPri, boolean copyOrTransfer, Function<X, @Nullable Prioritizable> getPri, X... src) {

        assert (src.length > 0);

        double priTarget = Math.min(maxPri, Util.sumDouble(s -> {
            if (s == null) return 0;
            return getPri.apply(s).priElseZero();
        }, src));

        UnitPri u = new UnitPri();

        if (priTarget > ScalarValue.EPSILON) {
            float perSrc = (float) (priTarget / src.length);
            //TODO random visit order if not copying (transferring)
            for (X t: src) {
                if (t != null) {
                    float v = u.take(getPri.apply(t), perSrc, true, copyOrTransfer);
                    if (Util.equals(v, 1f, EPSILON))
                        break; //done
                }
            }
        }
        //assert (u.priElseZero() <= maxPri + ScalarValue.EPSILON): "not: " + u.priElseZero() + " <= " + maxPri + EPSILON;
        return u.pri();
    }

//    default FloatRange priAsFloatRange() {
//        return new FloatRange(pri(), 0, 1) {
//            @Override
//            public void set(float value) {
//                super.set(value);
//                Prioritizable.this.pri(value);
//            }
//
//            @Override
//            public float getAndSet(float r) {
//                throw new UnsupportedOperationException();
//            }
//        };
//    }


}
