package jcog.pri;

import com.google.common.base.Function;
import jcog.Texts;
import jcog.Util;

import static jcog.Util.lerp;

/**
 * Mutable Prioritized
 * implementations need only implement priSet() and pri()
 */
public interface Priority extends Prioritized {

    /**
     * Change priority value
     *
     * @param p The new priority
     * @return value after set
     */
    float priSet(float p);

    /**
     * the result of this should be that pri() is not finite (ex: NaN)
     * returns false if already deleted (allowing overriding subclasses to know if they shold also delete)
     */
    default boolean delete() {
        float p = pri();
        if (p==p) {
            this.priSet(Float.NaN);
            return true;
        }
        return false;
    }

    default void priSet(Prioritized p) {
        priSet(p.pri());
    }

    default void priMax(float max) {
        priSet(Math.max(priElseZero(), max));
    }

    default void priMin(float min) {
        priSet(Math.min(priElseZero(), min));
    }

    default float priAdd(float toAdd) {

        float e = pri();
        if (e != e) {
            if (toAdd <= 0) {
                return Float.NaN;
            } /*else {
                e = 0; 
            }*/
        } else {
            toAdd += e;
        }

        return priSet(toAdd);
    }

    default float priMult(float factor) {
        float p = pri();
        if (p == p)
            return priSet(p * /*notNaNOrNeg*/(factor));
        else
            return Float.NaN;
    }

    default float priMult(float factor, float min) {
        float p = pri();
        if (p == p)
            return priSet(Math.max(min, p * /*notNaNOrNeg*/(factor)));
        else
            return Float.NaN;
    }


    /**
     * assumes 1 max value (Plink not NLink)
     */
    default float priAddOverflow(float toAdd) {
        if (Math.abs(toAdd) <= EPSILON) {
            return 0;
        }

        float before = priElseZero();
        float next = priAdd(toAdd);
        float delta = next - before;

        return toAdd - delta;
    }

    default float take(Priority source, float p, boolean amountOrFraction, boolean copyOrMove) {
        float amount;
        if (!amountOrFraction) {
            if (p < Prioritized.EPSILON) return 0;
            amount = source.priElseZero() * p;
            if (amount < Prioritized.EPSILON) return 0;
        } else {
            amount = p;
        }

        float before = priElseZero();

        float after = priAdd(amount);

        float taken = after - before;

        if (!copyOrMove) {
            source.priSub(taken);
        }

        return taken;
    }


    default Priority setPriThen(float p) {
        priSet(p);
        return this;
    }

    default float priSub(float toSubtract) {
        assert (toSubtract >= 0) : "trying to subtract negative priority: " + toSubtract;

        return priAdd(-toSubtract);
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


    /**
     * normalizes the current value to within: min..(range+min), (range=max-min)
     */
    default void normalizePri(float min, float range, float lerp) {
        float p = priElseNeg1();
        if (p < 0) return;

        priLerp((p - min) / range, lerp);
    }

    default Priority priLerp(float target, float speed) {
        float pri = pri();
        if (pri == pri)
            priSet(lerp(speed, pri, target));
        return this;
    }

    static Prioritized fund(float maxPri, boolean copyOrTransfer, Priority... src) {
        return fund(maxPri, copyOrTransfer, (x -> x), src);
    }

    /**
     * X[] may contain nulls
     */
    static <X> UnitPri fund(float maxPri, boolean copyOrTransfer, Function<X, Priority> getPri, X... src) {

        assert (src.length > 0);

        float priSum = Util.sum((X s) -> {
            if (s == null) return 0;
            Priority p = getPri.apply(s);
            return p.priElseZero();
        }, src);

        float priTarget = Math.min(maxPri, priSum);

        UnitPri u = new UnitPri();

        if (priTarget > Prioritized.EPSILON) {
            float perSrc = priTarget / src.length;
            for (X t: src) {
                if (t != null)
                    u.take(getPri.apply(t), perSrc, true, copyOrTransfer);
            }
        }
        assert (u.priElseZero() <= maxPri + Prioritized.EPSILON);
        return u;
    }

}
