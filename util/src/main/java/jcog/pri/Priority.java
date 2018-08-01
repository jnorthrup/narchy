package jcog.pri;

import com.google.common.base.Function;
import jcog.Texts;
import jcog.Util;

/**
 * Mutable Prioritized
 * implementations need only implement priSet() and pri()
 */
public interface Priority extends Prioritized, ScalarValue {

    default Priority pri(Prioritized p) {
        if (this != p)
            pri(p.pri());
        return this;
    }

    default float take(Priority source, float p, boolean amountOrFraction, boolean copyOrMove) {
        float amount;
        if (!amountOrFraction) {
            amount = source.priElseZero() * p;
        } else {
            amount = p;
        }
        if (amount < ScalarValue.EPSILON) return 0;

        final float[] before = new float[1];

        float after = pri((x,a)->{
            if (x!=x)
                x = 0;
            before[0] = x;
            return x + a;
        }, amount);

        float b = before[0];
        if (b!=b)
            b = 0;

        float taken = after - b;

        if (!copyOrMove) {
            source.priSub(taken);
        }

        return taken;
    }


    @Deprecated default Priority setPriThen(float p) {
        pri(p);
        return this;
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

    default String getBudgetString() {
        return Prioritized.toString(this);
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

    static Prioritized fund(float maxPri, boolean copyOrTransfer, Priority... src) {
        return fund(maxPri, copyOrTransfer, (x -> x), src);
    }

    /**
     * X[] may contain nulls
     */
    static <X> UnitPri fund(float maxPri, boolean copyOrTransfer, Function<X, Priority> getPri, X... src) {

        assert (src.length > 0);

        float priTarget = Math.min(maxPri, Util.sum((X s) -> {
            if (s == null) return 0;
            return getPri.apply(s).priElseZero();
        }, src));

        UnitPri u = new UnitPri();

        if (priTarget > ScalarValue.EPSILON) {
            float perSrc = priTarget / src.length;
            for (X t: src) {
                if (t != null) {
                    float v = u.take(getPri.apply(t), perSrc, true, copyOrTransfer);
                    if (Util.equals(v, 1f, EPSILON))
                        break; //done
                }
            }
        }
        //assert (u.priElseZero() <= maxPri + ScalarValue.EPSILON): "not: " + u.priElseZero() + " <= " + maxPri + EPSILON;
        return u;
    }


}
