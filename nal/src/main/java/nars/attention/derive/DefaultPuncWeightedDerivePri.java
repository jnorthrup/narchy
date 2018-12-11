package nars.attention.derive;

import jcog.pri.ScalarValue;
import nars.NAR;
import nars.Task;
import nars.control.DurService;

import static nars.Op.*;

public class DefaultPuncWeightedDerivePri extends DefaultDerivePri {

    /** cache of punctuation priorities */
    transient private float beliefPri, goalPri, questionPri, questPri;

    public DefaultPuncWeightedDerivePri(NAR n) {
        DurService.on(n, this::update); //permanent
    }

    /** repurposes nar's default punctuation priorities (for input) as the derivation punctuation weighting */
    public void update(NAR nar) {

        float beliefPri = nar.beliefPriDefault.floatValue();
        float goalPri = nar.goalPriDefault.floatValue();
        float questionPri = nar.questionPriDefault.floatValue();
        float questPri = nar.questPriDefault.floatValue();

        //normalize to 1.0, for postAmp usage
        float max = Math.max(beliefPri, Math.max(goalPri, Math.max(questionPri, questPri)));
        if (max < ScalarValue.EPSILON) {
            //flat
            this.beliefPri = this.goalPri = this.questionPri = this.questPri = 1f;
        } else {
            this.beliefPri = beliefPri / max;
            this.goalPri = goalPri / max;
            this.questionPri = questionPri / max;
            this.questPri = questPri / max;
        }

    }

    @Override
    public float preAmp(byte conclusionPunc) {
        switch (conclusionPunc) {
            case BELIEF: return beliefPri;
            case GOAL: return goalPri;
            case QUESTION: return questionPri;
            case QUEST: return questPri;
            default:
                throw new UnsupportedOperationException();
        }
    }

    @Override
    protected float postAmp(Task t, float pri) {
        return preAmp(t.punc()) * pri;
        //return pri;
    }
}
