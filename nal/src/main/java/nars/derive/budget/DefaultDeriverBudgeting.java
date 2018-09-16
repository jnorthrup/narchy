package nars.derive.budget;

import jcog.Util;
import jcog.math.FloatRange;
import jcog.pri.ScalarValue;
import nars.NAR;
import nars.Task;
import nars.derive.Derivation;
import nars.derive.Deriver;
import nars.derive.DeriverBudgeting;
import nars.truth.Truth;

import static nars.Op.*;
import static nars.Op.QUEST;

/**
 * TODO parameterize, modularize, refactor etc
 */
public class DefaultDeriverBudgeting implements DeriverBudgeting {

    /**
     * global multiplier
     */
    public final FloatRange scale = new FloatRange(0.5f, 0f, 2f);

    /**
     * how important is it to retain conf (evidence).
     * leniency towards uncertain derivations
     */
    public final FloatRange confImportance = new FloatRange(0.95f, 0f, 1f);

    /** importance of frequency polarity in result (distance from freq=0.5) */
    public final FloatRange polarityImportance = new FloatRange(0.05f, 0f, 1f);

    public final FloatRange relGrowthExponent = new FloatRange(2f, 0f, 8f);

    @Override
    public float pri(Task t, float f, float e, Derivation d) {
        float factor = this.scale.floatValue();

        factor *= factorComplexity(t, d);

        if (f==f) {
            //belief or goal:
            factor *= factorConfidence(e, d);
            factor *= factorPolarity(f);
        }

        return Util.clamp(d.pri * factor, ScalarValue.EPSILON, 1f);
    }

    float factorComplexity(Task t, Derivation d) {
        float pCompl = d.parentComplexitySum;
        float dCompl = t.voluplexity();
        float relGrowthCostFactor =
                //pCompl / (pCompl + dCompl);
                //1f / (1f + Math.max(0, (dCompl - pCompl)) / pCompl);
                1f / (1f + Math.max(0, dCompl/(dCompl+pCompl)));
                //1f-Util.unitize((dCompl - pCompl) / pCompl );


        return (float) Math.pow(relGrowthCostFactor, relGrowthExponent.floatValue());
    }

    float factorPolarity(float freq) {
        float polarity = Truth.polarity(freq);
        return Util.lerp(polarity, 1f - polarityImportance.floatValue(), 1f);
    }

    float factorConfidence(float dEvi, Derivation d) {
        boolean single = d.concSingle;
        float pEvi = single ? d.taskEvi : Math.max(d.taskEvi, d.beliefEvi);
        if (pEvi > 0) {

            float confLossFactor = 1f-Util.unitize((pEvi - dEvi) / pEvi);

            return Util.lerp(confLossFactor, 1f-confImportance.floatValue(), 1);
        }

        throw new RuntimeException("spontaneous belief/goal evidence generated from only question parent task");
        //return 1; //
    }

    /** cache of punctuation priorities */
    transient private float beliefPri, goalPri, questionPri, questPri;

    /** repurposes nar's default punctuation priorities (for input) as the derivation punctuation weighting */
    @Override public void update(Deriver deriver, NAR nar) {

        beliefPri = nar.beliefPriDefault.floatValue();
        goalPri = nar.goalPriDefault.floatValue();
        questionPri = nar.questionPriDefault.floatValue();
        questPri = nar.questPriDefault.floatValue();
    }

    @Override
    public float puncFactor(byte conclusion) {
        switch (conclusion) {
            case BELIEF: return beliefPri;
            case GOAL: return goalPri;
            case QUESTION: return questionPri;
            case QUEST: return questPri;
            default:
                throw new UnsupportedOperationException();
        }
    }
}
