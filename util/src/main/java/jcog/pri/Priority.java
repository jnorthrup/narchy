package jcog.pri;

import jcog.Texts;
import org.jetbrains.annotations.Nullable;

import static jcog.Util.lerp;
import static jcog.Util.notNaN;

/**
 * Mutable Prioritized
 */
public interface Priority extends Prioritized {
    /**
     * Change priority value
     *
     * @param p The new priority
     * @return value after set
     */
    float priSet(float p);

    default void priSet(/*@NotNull*/ Prioritized p) {
        priSet(p.pri());
    }

    /**
     * returns null if already deleted
     */
    @Nullable Priority clonePri();


    default float priMax(float max) {
        priSet(Math.max(priElseZero(), max)); return pri();
    }

    default void priMin(float min) {
        priSet(Math.min(priElseZero(), min));
    }

    default float priAdd(float toAdd) {
        //notNaN(toAdd);
        float e = pri();
        if (e != e) {
            if (toAdd <= 0) {
                return Float.NaN; //subtracting from deleted has no effect
            } /*else {
                e = 0; //adding to deleted resurrects it to pri=0 before adding
            }*/
        } else {
            toAdd += e;
        }

        return priSet(toAdd);
    }

//    default float priAddAndGetDelta(float toAdd) {
//        float before = priElseZero();
//        return priSet(before + notNaN(toAdd)) - before;
//    }

    default float priSub(float toSubtract) {
        //setPri(priElseZero() - toSubtract);
        return priAdd(-toSubtract);
    }

    default void priSub(float maxToSubtract, float minFractionRetained) {
        float p = priElseZero();
        if (p > 0) {
            float pMin = minFractionRetained * p;
            float pNext = Math.max((p - maxToSubtract), pMin);
            priSet(pNext);
        }
    }

    /** the result of this should be that pri() is not finite (ex: NaN)
     * returns false if already deleted (allowing overriding subclasses to know if they shold also delete) */
    boolean delete();


//    default void priAvg(float pOther, float rate) {
//        float cu = priElseZero();
//        setPriority(Util.lerp(rate, (cu + pOther)/2f, cu));
//    }

//    default float priAddOverflow(float toAdd) {
//        return priAddOverflow(toAdd, null);
//    }

    default float priAddOverflow(float toAdd, @Nullable float[] pressurized) {
        if (Math.abs(toAdd) <= EPSILON) {
            return 0; //no change
        }

        float before = priElseZero();
        float next = priAdd(toAdd);
        float delta = next - before;
        float excess = toAdd - delta;

        if (pressurized != null)
            pressurized[0] += delta;

        return excess;
    }



    default float priMult(float factor) {
        float p = pri();
        if (p == p)
            return priSet(p * /*notNaNOrNeg*/(factor));
        return Float.NaN;
    }



    default float priAddOverflow(float toAdd) {
        if (Math.abs(toAdd) <= EPSILON) {
            return 0; //no change
        }

        float before = priElseZero();
        float next = priAdd(toAdd);
        float delta = next - before;

        return toAdd - delta;
    }

    default float take(Priority source, float p, boolean amountOrFraction, boolean copyOrMove) {
        float amount;
        if (!amountOrFraction) {
            if (p < Pri.EPSILON) return 0;
            amount = source.priElseZero() * p;
            if (amount < Pri.EPSILON) return 0;
        } else {
            amount = p;
        }

        float before = priElseZero();

        float after = priAdd(amount);

        float taken = after - before;

        if (!copyOrMove) {
            //TRANSFER

            //cap at 1, and only transfer what is necessary to reach it
            if (taken > Pri.EPSILON) {
                //subtract first to ensure the funds are available
                source.priSub(taken);
            }
        }

        return taken;
    }

//    /** returns the delta */
//    default float priLerpMult(float factor, float speed) {
//
////        if (Util.equals(factor, 1f, Param.BUDGET_EPSILON))
////            return 0; //no change
//
//        float p = pri();
//        float target = unitize(p * factor);
//        float delta = target - p;
//        setPriority(lerp(speed, target, p));
//        return delta;
//
//    }

//    default void absorb(@Nullable MutableFloat overflow) {
//        if (overflow!=null) {
//            float taken = Math.min(overflow.floatValue(), 1f - priElseZero());
//            if (taken > EPSILON_DEFAULT) {
//                overflow.subtract(taken);
//                priAdd(taken);
//            }
//        }
//    }



    /**
     * Briefly display the BudgetValue
     *
     * @return String representation of the value with 2-digit accuracy
     */
    @Override
    default Appendable toBudgetStringExternal() {
        return toBudgetStringExternal(null);
    }

    @Override
    default StringBuilder toBudgetStringExternal(StringBuilder sb) {
        return Prioritized.toStringBuilder(sb, Texts.n2(pri()));
    }

    @Override
    default String toBudgetString() {
        return toBudgetStringExternal().toString();
    }

    @Override
    default String getBudgetString() {
        return Prioritized.toString(this);
    }



    /**
     * normalizes the current value to within: min..(range+min), (range=max-min)
     */
    default void normalizePri(float min, float range, float lerp) {
        float p = priElseNeg1();
        if (p < 0) return; //dont normalize if deleted

        priLerp((p - min) / range, lerp);
    }

     default Priority priLerp(float target, float speed) {
        float pri = pri();
        if (pri == pri)
            priSet(lerp(speed, pri, target));
        return this;
    }

//    void orPriority(float v);
//
//    void orPriority(float x, float y);

}
