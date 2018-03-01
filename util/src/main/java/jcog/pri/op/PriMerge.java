package jcog.pri.op;

import jcog.Util;
import jcog.pri.Prioritized;
import jcog.pri.Priority;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;

import static jcog.pri.op.PriMerge.PriMergeOp.*;

/**
 * Budget merge function, with input scale factor
 */
@FunctionalInterface
public interface PriMerge extends BiConsumer<Priority, Prioritized> {






    /** merge 'incoming' budget (scaled by incomingScale) into 'existing'
     *  incomingScale is a factor (0 < s <= 1) by which the incoming budget's effect is multiplied,
     *  1.0 being complete merge and 0 being no effect at all.
     *
     * @return any resultng overflow priority which was not absorbed by the target, >=0
     * */
    float merge(Priority existing, Prioritized incoming);



    @Nullable
    @Override
    default void accept(Priority existing, Prioritized incoming) {
        merge(existing, incoming);
    }

    static void max(Priority existing, Prioritized incoming) {
        float p = incoming.priElseZero();
        if (p > 0)
            existing.priMax(p);
    }

    enum PriMergeOp {
        PLUS,
        AVG,
        OR,
        //AND, OR,
        MAX
    }

    /** srcScale only affects the amount of priority adjusted; for the other components, the 'score'
     * calculations are used to interpolate
     * @param exi existing budget
     * @param inc incoming budget
     *
     //TODO will this work for a possible negative pri value case?
     * */
    static float blend(Priority exi, Prioritized inc, PriMerge.PriMergeOp priMerge) {

        float ePriBefore = exi.priElseZero();
        float iPri = inc.priElseZero();

        float nextPri;
        switch (priMerge) {
            case PLUS:
                nextPri = ePriBefore + iPri;
                break;
            case OR:
                nextPri = Util.or(ePriBefore,iPri);
                break;
            case MAX:
                nextPri = Math.max(ePriBefore, iPri);
                break;
            case AVG:
                nextPri = (iPri+ePriBefore)/2f;
                break;
            //TODO
            //case AND:     .. = ePri * iPri;          break;
            //case OR:      .. = or(ePri,iPri);        break;
            default:
                throw new UnsupportedOperationException();
        }

        float ePriAfter = exi.priSet( nextPri );

        return iPri - (ePriAfter - ePriBefore);
    }

//    static float dqBlendByPri(@NotNull Budget tgt, @NotNull Budgeted src, float srcScale, boolean addOrAvgPri) {
//        float incomingPri = src.priIfFiniteElseZero() * srcScale;
//
//        float currentPri = tgt.priIfFiniteElseZero();
//
//        float sumPri = currentPri + incomingPri;
//
//        float cp = sumPri > 0 ? currentPri / sumPri : 0.5f; // current proportion
//
//        return dqBlend(tgt, src, addOrAvgPri ?
//                sumPri :
//                ((cp * currentPri) + ((1f-cp) * incomingPri)), cp);
//    }
//    static float dqBlendBySummary(@NotNull Budget tgt, @NotNull Budgeted src, float srcScale, boolean addOrAvgPri) {
//        float incomingPri = src.pri() * srcScale;
//        float incomingSummary = src.summary() * srcScale;
//
//        float currentPri = tgt.priIfFiniteElseZero();
//        float currentSummary = tgt.summary();
//
//        float sumSummary = currentSummary + incomingSummary;
//
//        float cp = currentSummary / sumSummary; // current proportion
//
//        return dqBlend(tgt, src, addOrAvgPri ?
//                currentPri + incomingPri :
//                ((cp * currentPri) + ((1f-cp) * incomingPri)), cp);
//    }
//
//    PriMerge errorMerge = (x, y, z) -> {
//        throw new UnsupportedOperationException();
//    };
//
//    PriMerge nullMerge = (x, y, z) -> {
//        //nothing
//        return 0f;
//    };

    /** sum priority, LERP other components in proportion to the priorities */
    PriMerge plus = (tgt, src) -> blend(tgt, src, PLUS);

    /** avg priority, LERP other components in proportion to the priorities */
    PriMerge avg = (tgt, src) -> blend(tgt, src, AVG);

//    /** or priority, LERP other components in proportion to the priorities */
    PriMerge or = (tgt, src) -> blend(tgt, src, OR);


    PriMerge max = (tgt, src) -> blend(tgt, src, MAX);

    /** avg priority, LERP other components in proportion to the priorities */
    PriMerge replace = (tgt, src) -> src.priElse(tgt.priElseZero());

//
//
//    /** AND priority, LERP other components in proportion to the priorities */
//    BudgetMerge andBlend = (tgt, src, srcScale) -> blend(tgt, src, srcScale, AND);


//    @Deprecated BudgetMerge plusDQDominant = (tgt, src, srcScale) -> {
//        float nextPriority = src.priIfFiniteElseZero() * srcScale;
//
//        float currentPriority = tgt.priIfFiniteElseZero();
//
//        float sumPriority = currentPriority + nextPriority;
//        float overflow;
//        if (sumPriority > 1) {
//            overflow = sumPriority - 1f;
//            sumPriority = 1f;
//        } else {
//            overflow = 0;
//        }
//
//        boolean currentWins = currentPriority > nextPriority;
//
//        tgt.budget( sumPriority,
//                (currentWins ? tgt.dur() : src.dur()),
//                (currentWins ? tgt.qua() : src.qua()));
//
//        return overflow;
//    };

//    /** add priority, interpolate durability and quality according to the relative change in priority
//     *  WARNING untested
//     * */
//    BudgetMerge plusDQInterp = (tgt, src, srcScale) -> {
//        float dp = src.pri() * srcScale;
//
//        float currentPriority = tgt.priIfFiniteElseZero();
//
//        float nextPri = currentPriority + dp;
//        if (nextPri > 1) nextPri = 1f;
//
//        float currentNextPrioritySum = (currentPriority + nextPri);
//
//        /* current proportion */
//        final float cp = currentNextPrioritySum != 0 ? currentPriority / currentNextPrioritySum : 0.5f;
//
//        /* next proportion = 1 - cp */
//        float np = 1.0f - cp;
//
//
//        float nextDur = (cp * tgt.dur()) + (np * src.dur());
//        float nextQua = (cp * tgt.qua()) + (np * src.qua());
//
//        if (!Float.isFinite(nextDur))
//            throw new RuntimeException("NaN dur: " + src + ' ' + tgt.dur());
//        if (!Float.isFinite(nextQua))
//            throw new RuntimeException("NaN quality");
//
//        tgt.budget( nextPri, nextDur, nextQua );
//    };




//    /** the max priority, durability, and quality of two tasks */
//    default Budget mergeMax(Budget b) {
//        return budget(
//                Util.max(getPriority(), b.getPriority()),
//                Util.max(getDurability(), b.getDurability()),
//                Util.max(getQuality(), b.getQuality())
//        );
//    }


//    /**
//     * merges another budget into this one, averaging each component
//     */
//    default void mergeAverageLERP(Budget that) {

//    }

//    /* ----------------------- Concept ----------------------- */
//    /**
//     * Activate a concept by an incoming TaskLink
//     *
//     *
//     * @param factor linear interpolation factor; 1.0: values are applied fully,  0: values are not applied at all
//     * @param receiver The budget receiving the activation
//     * @param amount The budget for the new item
//     */
//    public static void activate(final Budget receiver, final Budget amount, final Activating mode, final float factor) {
//        switch (mode) {
//            /*case Max:
//                receiver.max(amount);
//                break;*/
//
//            case Accum:
//                receiver.accumulate(amount);
//                break;
//
//            case Classic:
//                float priority = or(receiver.getPriority(), amount.getPriority());
//                int durability = aveAri(receiver.getDurability(), amount.getDurability());
//                receiver.setPriority(priority);
//                receiver.setDurability(durability);
//                break;
//
//            case WTF:
//
//                final float currentPriority = receiver.getPriority();
//                final float targetPriority = amount.getPriority();
//                /*receiver.setPriority(
//                        lerp(or(currentPriority, targetPriority),
//                                currentPriority,
//                                factor) );*/
//                float op = or(currentPriority, targetPriority);
//                if (op > currentPriority) op = lerp(op, currentPriority, factor);
//                receiver.setPriority( op );
//
//                final float currentDurability = receiver.getDurability();
//                final float targetDurability = amount.getDurability();
//                receiver.setDurability(
//                        lerp(aveAri(currentDurability, targetDurability),
//                                currentDurability,
//                                factor) );
//
//                //doesnt really change it:
//                //receiver.setQuality( receiver.getQuality() );
//
//                break;
//        }
//
//    }
//

//    /**
//     * merges another budget into this one, averaging each component
//     */
//    public void mergeAverage(@NotNull Budget that) {
//        if (this == that) return;
//
//        budget(
//                mean(pri(), that.pri()),
//                mean(dur(), that.dur()),
//                mean(qua(), that.qua())
//        );
//    }

}
