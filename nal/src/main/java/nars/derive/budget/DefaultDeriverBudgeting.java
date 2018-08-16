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
    public final FloatRange scale = new FloatRange(0.9f, 0f, 2f);

    /**
     * how important is it to retain conf (evidence).
     * leniency towards uncertain derivations
     */
    public final FloatRange confImportance = new FloatRange(0.9f, 0f, 1f);

    /** importance of frequency polarity in result (distance from freq=0.5) */
    public final FloatRange polarityImportance = new FloatRange(0.1f, 0f, 1f);

    public final FloatRange relGrowthExponent = new FloatRange(3f, 0f, 8f);

    @Override
    public float pri(Task t, Truth derivedTruth, Derivation d) {
        float factor = this.scale.floatValue();

        factor *= factorComplexity(t, d);

        if (derivedTruth != null) {
            //belief or goal:
            factor *= factorConfidence(derivedTruth, d);
            factor *= factorPolarity(derivedTruth);
        }

        return Util.clamp(d.pri * factor, ScalarValue.EPSILON, 1f);
    }

    float factorComplexity(Task t, Derivation d) {
        float pCompl = d.parentComplexitySum;
        float dCompl = t.voluplexity();
        float penalty = 1; //base penalty (relative to parent complexity)
        float relGrowthCostFactor =
                //pCompl / (pCompl + dCompl);
                1f / (1f + (penalty + Math.max(0, (dCompl - pCompl))) / pCompl);

        return (float) Math.pow(relGrowthCostFactor, relGrowthExponent.floatValue());
    }

    float factorPolarity(Truth derivedTruth) {
        float polarity = derivedTruth.polarity();
        return 1f - ((1f - polarity) * polarityImportance.floatValue());
    }

    float factorConfidence(Truth derivedTruth, Derivation d) {
        boolean single = d.concSingle;
        float pEvi = single ? d.taskEvi : Math.max(d.taskEvi, d.beliefEvi);
        if (pEvi > 0) {
            float pConf = w2cSafe(pEvi);
            float dConf = derivedTruth.conf();

            float confLossFactor = Util.unitize((pConf - dConf) / pConf);

            return Util.lerp(confImportance.floatValue(), 1, confLossFactor);
        }

        throw new RuntimeException("spontaneous belief/goal evidence generated from only question parent task");
        //return 1; //
    }
}
