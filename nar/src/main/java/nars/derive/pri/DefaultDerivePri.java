package nars.derive.pri;

import jcog.Util;
import jcog.math.FloatRange;
import jcog.pri.ScalarValue;
import nars.Task;
import nars.derive.Derivation;
import nars.truth.Truth;
import nars.truth.func.TruthFunctions;

/**
 * TODO parameterize, modularize, refactor etc
 * TODO belief decomposition gets less priority than a question activated decomposition
 */
public class DefaultDerivePri implements DerivePri {

    /**
     * master derivation gain factor
     */
    public final FloatRange gain = new FloatRange(1f, 0f, 2f);

    public final FloatRange questionGain = new FloatRange(1f, 0f, 2f);


    /**
     * how important is it to retain conf (evidence).
     * leniency towards uncertain derivations
     */
    public final FloatRange eviImportance = new FloatRange(1f, 0f, 1f);



    /** occam's razor - increase this discriminate more heavily against more complex derivations */
    public final FloatRange simplicityImportance = new FloatRange(1f, 0f, 8f);


    public final FloatRange simplicityExponent = new FloatRange(1f, 0f, 4f);


    /** importance of frequency polarity in result (distance from freq=0.5) */
    public final FloatRange polarityImportance = new FloatRange(0.01f, 0f, 1f);

    @Override
    public float pri(Task t, Derivation d) {
        float factorCmpl =
                    factorComplexityRelative(t, d, simplicityExponent.floatValue());
                    //= factorComplexityAbsolute(t, d);
                    //= factorComplexityRelative2(t, d);

        float factor;
        if (t.isBeliefOrGoal()) {
            factor = factorCmpl * factorPolarity(t.freq()); //<-- absolute is much better
            //factor = factorCmpl * factorEviRelative(t, d) * factorPolarity(t.freq());
        } else {
            factor = questionGain.floatValue() * factorCmpl * factorCmpl;
        }

        factor *= factorEviAbsolute(t,d);

        float y = this.gain.floatValue() * postAmp(t, d.parentPri(), factor);
        return Util.clamp(y, ScalarValue.EPSILON, 1);
    }

    /** default impl: pass-thru */
    protected float postAmp(Task t, float derivePri, float factor) {
        return derivePri * factor;
    }

    float factorComplexityAbsolute(Task t, Derivation d) {
        int max = d.termVolMax + 1;

        float weight = Math.min(1, t.voluplexity() / max);
        //float parentWeight = Math.min(1, ((d.parentVoluplexitySum / 2)/*avg*/) / max);
        //float f = (1f - Util.lerp(parentWeight,weight,parentWeight * weight));
        //return Util.lerp(simplicityImportance.floatValue(), 1f, f);
        return Util.lerp(simplicityImportance.floatValue(), 1f, 1-weight);
    }
//
//    float factorComplexityRelative2(Task t, Derivation d) {
//        float inc = (t.voluplexity() - d.parentVoluplexitySum /2 /* avg */);
//        if (inc <= 0) return 1f;
//        float f = 1f / (1f + inc);
//        return Util.lerp(simplicityImportance.floatValue(), 1f, f);
//    }

    float factorComplexityRelative(Task t, Derivation d, float simplicityExponent) {

        float pCompl =
                d.single ?
                    d.taskTerm.volume()
                    :
                    ((float) (d.taskTerm.volume() + d.beliefTerm.volume())) / 2; //average

        int dCompl = t.volume();

        float basePenalty = 0.5f; //if derivation is simpler, this is the maximum complexity increase seen
        float f = 1 - (basePenalty + Math.max(0, dCompl - pCompl)) / (basePenalty + dCompl);
        f = (float) Math.pow(f, simplicityExponent);

//        float f =
//                //pCompl / (pCompl + dCompl);
//                //1 - (dCompl - pCompl) / (pCompl+dCompl);
//                pCompl / (pCompl + dCompl);
//                //1f / (1f + Math.max(0, dCompl/(dCompl+pCompl)));
//                //1f / (1f + Math.max(0, (dCompl - pCompl)) / pCompl);
//                //1f-Util.unitize((dCompl - pCompl) / pCompl );

        return Util.lerp(simplicityImportance.floatValue(), 1f, f);
    }

    float factorPolarity(float freq) {
        float polarity = Truth.polarity(freq);
        return Util.lerp(polarityImportance.floatValue(), 1f, polarity);
    }

    float factorEviAbsolute(Task t, Derivation d) {
        //eternal=1 dur
        long taskRange = d._task.rangeIfNotEternalElse(1);
        long beliefRange = d.single ? taskRange : (d._belief.rangeIfNotEternalElse(1));
        double taskBeliefRange = Math.min(taskRange, beliefRange);

        double rangeRatio = t.rangeIfNotEternalElse(1) / taskBeliefRange;

        double y;
        if (t.isBeliefOrGoal())
            y = t.truth().confDouble() * rangeRatio; //conf integrated
        else
            y = rangeRatio * rangeRatio;
        return (float) Util.lerp(eviImportance.floatValue(), 1f, y);
    }


    float factorMaintainAverageEvidence(Task t, Derivation d) {

        double eParent = d.evi();
        double eDerived = t.evi();
        if (eParent <= eDerived)
//            throw new WTF("spontaneous belief inflation"); //not actually
            return 1;
        else {
//            double cDerived = w2cSafeDouble(eDerived);
//            double cParent = w2cSafeDouble(eParent);
//            float f = (float) (1 - ((cParent - cDerived) / cParent));
            float f = (float) (1 - ((eParent - eDerived) / eParent));

            Util.assertUnitized(f);
            return Util.lerp(eviImportance.floatValue(), 1f, f);

        }
    }


    @Override public float prePri(Derivation d) {

        if (d.isBeliefOrGoal()) {
            //belief or goal conf boost
            //TODO include time range as factor since it's average evi
            double te = d.truth.evi(), de = d.evi();
            double maintained =
                    //te / (te + de) //as weight
                    TruthFunctions.w2cSafe(te)/(TruthFunctions.w2cSafe(te)+TruthFunctions.w2cSafe(de)) //as conf
            ;

            return (float) (1 + Math.min(1, maintained));
        } else {
            //question
            return 1;
        }
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