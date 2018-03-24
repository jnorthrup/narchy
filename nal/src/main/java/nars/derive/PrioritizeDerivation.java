package nars.derive;

import jcog.Util;
import nars.Task;
import nars.truth.Truth;

import static jcog.Util.unitize;

/** stateless, storing any state information in the Derivation instance */
interface PrioritizeDerivation {

    /** called on new premise.  useful if an implementation wants to cache values that are common to all derivations of a premise  */
    default void premise(Derivation d) {

    }

    /**
     *
     * just returns the priority.  should not set the priority of the task
     *
     * @param t the derived task
     * @param d the derivation context
     * @return priority, or NaN to filter (cancel) this derived result
     */
    float pri(Task t, Derivation d);


    /** TODO modularize, refactor etc */
    @Deprecated class DefaultPrioritizeDerivation implements PrioritizeDerivation {

        @Override
        public float pri(Task t, Derivation d) {
            float discount = 1f;

            //t.volume();

            {
                //relative growth compared to parent complexity
                float pCompl = d.parentComplexityMax;
                float dCompl = t.voluplexity();
                float relGrowth =
                        unitize(pCompl / (pCompl + dCompl));
                discount *= (relGrowth);
            }

            {
                //absolute size relative to limit
                //float p = 1f / (1f + ((float)t.complexity())/termVolumeMax.floatValue());
            }

            //float simplicity = 1 - d.nar.deep.floatValue();
            float truthFactor = 0.5f;

            Truth derivedTruth = t.truth();
//        {
//
//            float dCompl = t.voluplexity();
//
////            if (simplicity > Float.MIN_NORMAL) {
//
////                //float increase = (dCompl-d.parentComplexityMax);
////                //if (increase > Pri.EPSILON) {
////                int penalty = 1;
////                float change = penalty + Math.abs(dCompl - d.parentComplexityMax); //absolute change: penalize drastic complexification or simplification, relative to parent task(s) complexity
////
////                //relative increase in complexity
////                //calculate the increases proportion to the "headroom" remaining for term expansion
////                //ie. as the complexity progressively grows toward the limit, the discount accelerates
////                float complexityHeadroom = Math.max(1, d.termVolMax - d.parentComplexityMax);
////                float headroomConsumed = Util.unitize(change /* increase */ / complexityHeadroom);
////                float headroomRemain = 1f - headroomConsumed * simplicity;
////
////                //note: applies more severe discount for questions/quest since the truth deduction can not apply
////                discount *= (derivedTruth != null) ? headroomRemain : Util.sqr(headroomRemain);
////            }
//
//            //absolute
//            int max = d.termVolMax;
//            float headroomRemain = Util.unitize(1f - (dCompl / max) * simplicity);
//            discount *= (derivedTruth != null) ? headroomRemain : Util.sqr(headroomRemain);
//        {
//
//            float dCompl = t.voluplexity();
//
////            if (simplicity > Float.MIN_NORMAL) {
//
////                //float increase = (dCompl-d.parentComplexityMax);
////                //if (increase > Pri.EPSILON) {
////                int penalty = 1;
////                float change = penalty + Math.abs(dCompl - d.parentComplexityMax); //absolute change: penalize drastic complexification or simplification, relative to parent task(s) complexity
////
////                //relative increase in complexity
////                //calculate the increases proportion to the "headroom" remaining for term expansion
////                //ie. as the complexity progressively grows toward the limit, the discount accelerates
////                float complexityHeadroom = Math.max(1, d.termVolMax - d.parentComplexityMax);
////                float headroomConsumed = Util.unitize(change /* increase */ / complexityHeadroom);
////                float headroomRemain = 1f - headroomConsumed * simplicity;
////
////                //note: applies more severe discount for questions/quest since the truth deduction can not apply
////                discount *= (derivedTruth != null) ? headroomRemain : Util.sqr(headroomRemain);
////            }
//
//            //absolute
//            int max = d.termVolMax;
//            float headroomRemain = Util.unitize(1f - (dCompl / max) * simplicity);
//            discount *= (derivedTruth != null) ? headroomRemain : Util.sqr(headroomRemain);
//        }


            if (/* belief or goal */ derivedTruth != null) {

                //loss of relative confidence: prefer confidence, relative to the premise which formed it
                float parentEvi = d.single ? d.premiseEviSingle : d.premiseEviDouble;
                if (parentEvi > 0) {
                    discount *= Util.lerp(1f - truthFactor, unitize(
                            derivedTruth.evi() / parentEvi
                            //derivedTruth.conf() / w2cSafe(parentEvi)
                    ), 1f);
                }

                //opinionation: preference for polarized beliefs/goals
//            float polarizationPreference = 0.5f;
//            discount *= Util.lerp(polarizationPreference, 1, (2 * Math.abs(derivedTruth.freq() - 0.5f)));
            }

            return discount * d.pri;

            //return Util.lerp(1f-t.originality(),discount, 1) * d.premisePri; //more lenient derivation budgeting priority reduction in proportion to lack of originality

        }
    }

}
