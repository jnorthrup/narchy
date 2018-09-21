package nars.derive.budget;

import nars.NAR;
import nars.derive.Deriver;

import static nars.Op.*;

public class DefaultPriWeightedDeriverBudgeting extends DefaultDeriverBudgeting {

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
