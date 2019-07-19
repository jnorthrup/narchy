package nars.derive.pri;

import jcog.Util;
import jcog.math.FloatRange;
import jcog.pri.ScalarValue;
import nars.Task;
import nars.derive.model.Derivation;
import nars.truth.Truth;
import nars.truth.proj.TruthIntegration;

import static nars.time.Tense.ETERNAL;

/**
 * TODO parameterize, modularize, refactor etc
 * TODO belief decomposition gets less priority than a question activated decomposition
 */
public class DefaultDerivePri implements DerivePri {

    /**
     * master derivation gain factor
     */
    public final FloatRange gain = new FloatRange(1f, 0f, 2f);

    public final FloatRange questionGain = new FloatRange(0.5f, 0f, 2f);


    /**
     * how important is it to retain conf (evidence).
     * leniency towards uncertain derivations
     */
    public final FloatRange eviImportance = new FloatRange(1f, 0f, 1f);



    /** occam's razor - increase this discriminate more heavily against more complex derivations */
    public final FloatRange simplicityImportance = new FloatRange(1f, 0f, 8f);


    public final FloatRange simplicityExponent = new FloatRange(1f, 0f, 4f);


    /** importance of frequency polarity in result (distance from freq=0.5) */
    public final FloatRange polarityImportance = new FloatRange(0f, 0f, 1f);

    @Override
    public float pri(Task t, Derivation d) {
        float factorCmpl =
                    factorComplexityRelative(t, d, simplicityExponent.floatValue());
                    //= factorComplexityAbsolute(t, d);
                    //= factorComplexityRelative2(t, d);

        float factor;
        if (t.isBeliefOrGoal()) {
            factor = factorCmpl * factorEviRelative(t, d) * factorPolarity(t.freq());
            //factor = factorCmpl * factorEviAbsolute(t, d) * factorPolarity(t.freq());
        } else {
            factor = questionGain.floatValue() * factorCmpl * factorCmpl;
        }

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
                d.concSingle ?
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
        float f;
        if (t.isBeliefOrGoal())
            f = t.conf();
        else
            f = 1;
        return Util.lerp(eviImportance.floatValue(), 1f, f);
    }

    float factorEviRelative(Task t, Derivation d) {

        double eParentTask, eParentBelief, eDerived;
        if (t.isEternal()) {
            eDerived = t.evi();
            assert(d.taskStart==ETERNAL);
            eParentTask = d._task.isBeliefOrGoal() ? d._task.evi() : 0;

            if (!d.concSingle)
                eParentBelief = d._belief.evi();
            else
                eParentBelief = 0;

        } else {

            eDerived = TruthIntegration.evi(t);

            long ts = t.start(), te = t.end();
            eParentTask = d._task.isBeliefOrGoal() ?
                    (d._task.isEternal() ? TruthIntegration.evi(d._task, ts, te, 0) : TruthIntegration.evi(d._task))
                        : 0;

            if (!d.concSingle)
                eParentBelief =
                    d._belief.isEternal() ? TruthIntegration.evi(d._belief, ts, te, 0) : TruthIntegration.evi(d._belief);
            else
                eParentBelief = 0;

        }

        double eParent =
                //Math.max(eParentTask, eParentBelief);
                eParentTask + eParentBelief;
        if (eParent <= eDerived)
//            throw new WTF("spontaneous belief inflation"); //not actually
            return 1;
        else {
//            double cDerived = w2cSafeDouble(eDerived);
//            double cParent = w2cSafeDouble(eParent);
//            double lossFactor = 1 - ((cParent - cDerived) / cParent);

            float f = (float)(1 - ((eParent - eDerived) / eParent));

            Util.assertUnitized(f);
            return Util.lerp(eviImportance.floatValue(), 1f, f);

        }
    }


}
