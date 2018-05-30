package nars.derive.budget;

import jcog.Util;
import jcog.math.FloatRange;
import nars.Task;
import nars.derive.Derivation;
import nars.derive.DeriverBudgeting;
import nars.truth.Truth;

import static nars.truth.TruthFunctions.w2cSafe;

/** TODO parameterize, modularize, refactor etc */
public class DefaultDeriverBudgeting implements DeriverBudgeting {

    /** global multiplier */
    public final FloatRange scale = new FloatRange(1f, 0f, 1f);

    /** how important is it to retain evidence.
     * leniency towards uncertain derivations */
    public final FloatRange evidenceImportance = new FloatRange(1f, 0f, 1f);

    @Override
    public float pri(Task t, Derivation d) {
        float factor = this.scale.floatValue();

        //t.volume();


            //relative growth compared to parent complexity
            float pCompl = d.parentComplexitySum;
            float dCompl = t.voluplexity();
            float relGrowthCost =
                    (pCompl / (pCompl + dCompl));

            //curve
            relGrowthCost = Util.sqr(relGrowthCost);

            factor *= relGrowthCost;


        {
            //absolute size relative to limit
            //float p = 1f / (1f + ((float)t.complexity())/termVolumeMax.floatValue());
        }

        //float simplicity = 1 - d.nar.deep.floatValue();

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


        if (/*BELIEF OR GOAL*/derivedTruth != null) {

            //loss of relative evidence: prefer stronger evidence results, relative to the premise which formed it
            boolean single = d.single;
            float pEvi = single ? d.premiseEviSingle : d.premiseEviDouble;
            if (pEvi > 0) {
                float pConf = w2cSafe(pEvi);

                //float dEvi = derivedTruth.evi();
                float dConf = derivedTruth.conf();
                if (single)
                    dConf /=2; //count the derived confidence twice to be fair to comparison against the combined evidence of 2 parents

                float eviFactor = dConf / pConf; //allow > 1
                //float eviFactor = dEvi / pEvi; //allow > 1
                factor *= Util.lerp(evidenceImportance.floatValue(), 1, eviFactor);
            }

            //opinionation: preference for polarized beliefs/goals
//            float polarizationPreference = 0.5f;
//            discount *= Util.lerp(polarizationPreference, 1, (2 * Math.abs(derivedTruth.freq() - 0.5f)));
        } else {
            //QUESTIONS and QUESTS: apply the rel growth cost factor again
            //since there is no truth to heuristically discount
            //factor *= relGrowthCost;
            factor *= Util.lerp(evidenceImportance.floatValue(), 1, relGrowthCost); //discount balanced with the truth version
        }

        return factor * d.pri;

        //return Util.lerp(1f-t.originality(),discount, 1) * d.premisePri; //more lenient derivation budgeting priority reduction in proportion to lack of originality

    }
}
