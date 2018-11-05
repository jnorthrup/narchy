package nars.derive.budget;

import jcog.Util;
import jcog.math.FloatRange;
import jcog.pri.ScalarValue;
import nars.Task;
import nars.derive.Derivation;
import nars.derive.DeriverBudgeting;
import nars.truth.Truth;

import static nars.truth.TruthFunctions.w2cSafe;

/**
 * TODO parameterize, modularize, refactor etc
 */
public class DefaultDeriverBudgeting implements DeriverBudgeting {

    /**
     * global multiplier
     */
    public final FloatRange scale = new FloatRange(1f, 0f, 2f);

    /**
     * how important is it to retain conf (evidence).
     * leniency towards uncertain derivations
     */
    public final FloatRange eviRetention = new FloatRange(1f, 0f, 1f);

    /** importance of frequency polarity in result (distance from freq=0.5) */
    public final FloatRange polarityImportance = new FloatRange(0f, 0f, 1f);

    /** increase this discriminate more heavily against more complex derivations */
    public final FloatRange relGrowthExponent = new FloatRange(2f, 0f, 8f);

    @Override
    public float pri(Task t, float f, float e, Derivation d) {
        float factor = this.scale.floatValue();

        factor *=
                //factorComplexityAbsolute(t, d);
                factorComplexityRelative(t, d);

        if (f==f) {
            //belief or goal:
            factor *= factorEvi(e, d);
            factor *= factorPolarity(f);
        } else {
            factor *= factor; //re-apply for single-premise case
        }

        return Util.clamp( d.parentPri() * factor, ScalarValue.EPSILON, 1f);
    }

    float factorComplexityAbsolute(Task t, Derivation d) {
        float f = (1f - (t.voluplexity() / (d.termVolMax+1)));
        return (float) Math.pow(f, relGrowthExponent.floatValue());
    }

    float factorComplexityRelative(Task t, Derivation d) {
        float pCompl = d.parentComplexitySum;
        float dCompl = t.voluplexity();
        float f =
                1f / (1f + Math.max(0, dCompl/(dCompl+pCompl)));
                //pCompl / (pCompl + dCompl);
                //1f / (1f + Math.max(0, (dCompl - pCompl)) / pCompl);
                //1f-Util.unitize((dCompl - pCompl) / pCompl );


        return (float) Math.pow(f, relGrowthExponent.floatValue());
    }

    float factorPolarity(float freq) {
        float polarity = Truth.polarity(freq);
        return Util.lerp(polarity, 1f - polarityImportance.floatValue(), 1f);
    }

    float factorEvi(float dEvi, Derivation d) {

        float pEvi = d.parentEvi();

        if (pEvi > dEvi) {

            float pConf = w2cSafe(pEvi);
            float dConf = w2cSafe(dEvi);
            float eviLossFactor = 1f-Util.unitize((pConf - dConf) / pConf);

            return Util.lerp(eviLossFactor, 1f- eviRetention.floatValue(), 1);
        }

        //throw new RuntimeException("spontaneous belief/goal evidence generated from only question parent task");
        return 1; //
    }


}
