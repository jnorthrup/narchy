package nars.attention.derive;

import jcog.Util;
import jcog.math.FloatRange;
import jcog.pri.ScalarValue;
import nars.Task;
import nars.attention.DerivePri;
import nars.derive.Derivation;
import nars.truth.Truth;
import nars.truth.polation.TruthIntegration;

import static nars.time.Tense.ETERNAL;

/**
 * TODO parameterize, modularize, refactor etc
 */
public class DefaultDerivePri implements DerivePri {

    /**
     * master derivation gain factor
     */
    public final FloatRange gain = new FloatRange(1f, 0f, 2f);

    /**
     * how important is it to retain conf (evidence).
     * leniency towards uncertain derivations
     */
    public final FloatRange eviImportance = new FloatRange(1f, 0f, 1f);

    /** occam's razor - increase this discriminate more heavily against more complex derivations */
    public final FloatRange simplicityImportance = new FloatRange(0.9f, 0f, 8f);

    /** importance of frequency polarity in result (distance from freq=0.5) */
    public final FloatRange polarityImportance = new FloatRange(0.1f, 0f, 1f);

    @Override
    public float pri(Task t, Derivation d) {
        float factor = this.gain.floatValue();

        factor *= factorComplexityAbsolute(t, d);
        //factor *= factorComplexityRelative(t, d);
        //factor *= factorComplexityRelative2(t, d);

        if (t.isBeliefOrGoal()) {
            //belief or goal:
            factor *= factorEvi(t, d);
            factor *= factorPolarity(t.freq());
        } else {
            factor *= factor; //re-apply for single-premise case
        }

        float parent = d.parentPri();
        float y = postAmp(t, parent * factor);
        return Util.clampSafe(y, ScalarValue.EPSILON, parent);
    }

    /** default impl: pass-thru */
    protected float postAmp(Task t, float pri) {
        return pri;
    }

    float factorComplexityAbsolute(Task t, Derivation d) {
        int max = d.termVolMax + 1;

        float weight = Math.min(1, t.voluplexity() / max);
        //float parentWeight = Math.min(1, ((d.parentVoluplexitySum / 2)/*avg*/) / max);
        //float f = (1f - Util.lerp(parentWeight,weight,parentWeight * weight));
        //return Util.lerp(simplicityImportance.floatValue(), 1f, f);
        return Util.lerp(simplicityImportance.floatValue(), 1f, 1-weight);
    }

    float factorComplexityRelative2(Task t, Derivation d) {
        float inc = (t.voluplexity() - d.parentVoluplexitySum /2 /* avg */);
        if (inc <= 0) return 1f;
        float f = 1f / (1f + inc);
        return Util.lerp(simplicityImportance.floatValue(), 1f, f);
    }

    float factorComplexityRelative(Task t, Derivation d) {
        float pCompl = d.parentVoluplexitySum;
        float dCompl = t.voluplexity();
        float f =
                1f / (1f + Math.max(0, dCompl/(dCompl+pCompl)));
                //pCompl / (pCompl + dCompl);
                //1f / (1f + Math.max(0, (dCompl - pCompl)) / pCompl);
                //1f-Util.unitize((dCompl - pCompl) / pCompl );


        return Util.lerp(simplicityImportance.floatValue(), 1f, f);
    }

    float factorPolarity(float freq) {
        float polarity = Truth.polarity(freq);
        return Util.lerp(polarity, 1f - polarityImportance.floatValue(), 1f);
    }

    float factorEvi(Task t, Derivation d) {

        float ep, et;
        if (t.isEternal()) {
            et = t.evi();
            assert(d.taskStart==ETERNAL);
            ep = d._task.isBeliefOrGoal() ? d._task.evi() : 0;
            if (!d.concSingle) {
                assert(d.beliefStart==ETERNAL);
                ep += d._belief.evi();
            }
        } else {
            int dur = d.dur;

            et = TruthIntegration.evi(t);

            long ts = t.start(), te = t.end();
            ep = d._task.isBeliefOrGoal() ?
                    (d._task.isEternal() ? TruthIntegration.evi(d._task, ts, te, dur) : TruthIntegration.evi(d._task))
                        : 0;
            if (!d.concSingle) {
                ep += d._belief.isEternal() ? TruthIntegration.evi(d._belief, ts, te, dur) : TruthIntegration.evi(d._belief);
            }

        }

//        if (ep < et) {
//            throw new WTF("spontaneous belief inflation"); //not actually
//        }
        if (ep < et)
            return 1;
        else {
            float lossFactor = 1 - ((ep - et) / ep);
            return Util.lerp(eviImportance.floatValue(), 1f, lossFactor);
        }
    }


}
