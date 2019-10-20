package nars.derive.pri;

import jcog.Util;
import jcog.math.FloatRange;
import nars.Task;
import nars.derive.Derivation;
import nars.truth.Truth;

import static jcog.math.LongInterval.TIMELESS;
import static nars.truth.func.TruthFunctions.w2cSafeDouble;

/**
 * TODO parameterize, modularize, refactor etc
 * TODO belief decomposition gets less priority than a question activated decomposition
 */
public class DefaultDerivePri implements DerivePri {

    public final FloatRange questionGain = new FloatRange(
            1.0F
        //Util.PHI_min_1f
        , 0f, 2f);


    /**
     * how important is it to retain conf (evidence).
     * leniency towards uncertain derivations
     */
    public final FloatRange eviImportance = new FloatRange(0.5f, 0f, 1f);

    /** occam's razor - increase this discriminate more heavily against more complex derivations */
    public final FloatRange simplicityImportance = new FloatRange(1f, 0f, 8f);

    public final FloatRange simplicityExponent = new FloatRange(1.5f, 0f, 4f);

    /** importance of frequency polarity in result */
    public final FloatRange polarityImportance = new FloatRange(0.01f, 0f, 1f);

    @Override
    public float pri(final Task t, final Derivation d) {
        final float factorCmpl =
                    factorComplexityRelative(t, d, simplicityExponent.floatValue());
                    //= factorComplexityAbsolute(t, d);
                    //= factorComplexityRelative2(t, d);

        float factor = factorCmpl;

		factor *= t.isBeliefOrGoal() ?
            factorPolarity(t.freq()) :
            questionGain.floatValue() * factor; /* ^2 */

        //factorEviAbsolute(t,d);
        factor = (float) ((double) factor * factorMaintainRangeAndAvgEvi(t, d));

        final float y = postAmp(t, d.parentPri(), factor);
        return y;
    }

    /** default impl: pass-thru */
    protected float postAmp(final Task t, final float derivePri, final float factor) {
        return derivePri * factor;
    }

    float factorComplexityAbsolute(final Task t, final Derivation d) {
        final int max = d.termVolMax + 1;

        final float weight = Math.min(1.0F, t.voluplexity() / (float) max);
        //float parentWeight = Math.min(1, ((d.parentVoluplexitySum / 2)/*avg*/) / max);
        //float f = (1f - Util.lerp(parentWeight,weight,parentWeight * weight));
        //return Util.lerp(simplicityImportance.floatValue(), 1f, f);
        return Util.lerp(simplicityImportance.floatValue(), 1f, 1.0F -weight);
    }
//
//    float factorComplexityRelative2(Task t, Derivation d) {
//        float inc = (t.voluplexity() - d.parentVoluplexitySum /2 /* avg */);
//        if (inc <= 0) return 1f;
//        float f = 1f / (1f + inc);
//        return Util.lerp(simplicityImportance.floatValue(), 1f, f);
//    }

    float factorComplexityRelative(final Task t, final Derivation d, final float simplicityExponent) {

        final float pCompl =
                d.single ?
                        (float) d.taskTerm.volume()
                    :
                    ((float) (d.taskTerm.volume() + d.beliefTerm.volume())) / 2.0F; //average

        final int dCompl = t.volume();

        final float basePenalty = 0.5f; //if derivation is simpler, this is the maximum complexity increase seen
        float f = 1.0F - (basePenalty + Math.max((float) 0, (float) dCompl - pCompl)) / (basePenalty + (float) dCompl);
        f = (float) Math.pow((double) f, (double) simplicityExponent);

//        float f =
//                //pCompl / (pCompl + dCompl);
//                //1 - (dCompl - pCompl) / (pCompl+dCompl);
//                pCompl / (pCompl + dCompl);
//                //1f / (1f + Math.max(0, dCompl/(dCompl+pCompl)));
//                //1f / (1f + Math.max(0, (dCompl - pCompl)) / pCompl);
//                //1f-Util.unitize((dCompl - pCompl) / pCompl );

        return Util.lerp(simplicityImportance.floatValue(), 1f, f);
    }

    float factorPolarity(final float freq) {
        final float polarity = Truth.polarity(freq);
        return Util.lerp(polarityImportance.floatValue(), 1f, polarity);
    }

    float factorEviAbsolute(final Task t, final Derivation d) {
        final double rangeRatio = rangeRatio(t, d);

        //conf integrated
        final double y = t.isBeliefOrGoal() ? t.truth().confDouble() * rangeRatio : rangeRatio * rangeRatio;
        return (float) Util.lerp((double) eviImportance.floatValue(), 1, y);
    }

    private static double rangeRatio(final Task t, final Derivation d) {
        //eternal=1 dur
        final long taskRange = d._task.rangeIfNotEternalElse(TIMELESS);
        final long beliefRange = d.single ? taskRange : (d._belief.rangeIfNotEternalElse(TIMELESS));
        final long taskBeliefRange;
        if (taskRange == TIMELESS && beliefRange != TIMELESS) {
            taskBeliefRange = beliefRange;
        } else if (taskRange != TIMELESS && beliefRange == TIMELESS) {
            taskBeliefRange = taskRange;
        } else if (taskRange!=TIMELESS /*&& beliefRange!=TIMELESS*/) {
            taskBeliefRange = Math.min(taskRange, beliefRange);
        } else {
            taskBeliefRange = TIMELESS;
        }

        return Util.unitize((double) t.rangeIfNotEternalElse(taskBeliefRange) / ((double)taskBeliefRange) );
    }


    double factorMaintainRangeAndAvgEvi(final Task t, final Derivation d) {
        final double rangeRatio = rangeRatio(t, d);

        if (t.isQuestionOrQuest())
            return rangeRatio;

        final double eParent = d.evi();
        final double eDerived = t.evi();
        if (eParent <= eDerived)
//            throw new WTF("spontaneous belief inflation"); //not actually
            return rangeRatio;
        else {
            final double cDerived = w2cSafeDouble(eDerived);
            final double cParent = w2cSafeDouble(eParent);
            final float eRatio = (float) (1.0 - ((cParent - cDerived) / cParent));
            //double f = (float) (1 - ((eParent - eDerived) / eParent));

            Util.assertUnitized(eRatio);
            return Util.lerp((double) eviImportance.floatValue(), 1, (double) eRatio * rangeRatio);
        }
    }


    @Override public float prePri(final Derivation d) {

        return 1.0F;

//        if (d.isBeliefOrGoal()) {
//            //TODO include time range as factor since it's average evi
//            double te = d.truth.evi();
//            double de = d.evi();
//            double maintained =
//                    te / (te + de) //as weight
//                    //TruthFunctions.w2cSafe(te)/(TruthFunctions.w2cSafe(te)+TruthFunctions.w2cSafe(de))
//            ;
//            return (float) (1 + (Math.min(1, maintained)));
//            //return (float) (1 + Math.min(1, maintained));
//            //return (float) (1 + sqrt(w2cSafe(te)));
//        } else {
//            //question
//            return 1;
//        }
    }
}
/*
//        double eParentTask, eParentBelief, eDerived;
//        if (t.isEternal()) {
//            eDerived = t.evi();
//            assert(d.taskStart==ETERNAL);
//            eParentTask = d._task.isBeliefOrGoal() ? d._task.evi() : 0;
//
//            if (!d.concSingle)
//                eParentBelief = d._belief.evi();
//            else
//                eParentBelief = 0;
//
//        } else {
//
//            eDerived = TruthIntegration.evi(t);
//
//            long ts = t.start(), te = t.end();
//            eParentTask = d._task.isBeliefOrGoal() ?
//                    (d._task.isEternal() ? TruthIntegration.evi(d._task, ts, te, 0) : TruthIntegration.evi(d._task))
//                        : 0;
//
//            if (!d.concSingle)
//                eParentBelief =
//                    d._belief.isEternal() ? TruthIntegration.evi(d._belief, ts, te, 0) : TruthIntegration.evi(d._belief);
//            else
//                eParentBelief = 0;
//
//        }
//
//        double eParent =
//                Math.max(eParentTask, eParentBelief);
//                //Util.mean(eParentTask, eParentBelief);
//                //eParentTask + eParentBelief;

 */