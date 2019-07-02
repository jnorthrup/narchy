package nars.derive.pri;

import jcog.Util;
import jcog.pri.ScalarValue;
import nars.NAR;
import nars.Task;
import nars.derive.model.Derivation;

import static nars.Op.*;
import static nars.time.Tense.ETERNAL;

/** TODO strength parameter
 *  TODO toggle for preamp and postamp - both dont need applied necessarily
 * */
public class DefaultPuncWeightedDerivePri extends DefaultDerivePri {

    long lastUpdate = ETERNAL;
    final static float updateDurs = 1;

    /** cache of punctuation priorities */
    transient private float beliefPri, goalPri, questionPri, questPri;

    public DefaultPuncWeightedDerivePri() {
    }

    @Override
    public void premise(Derivation d) {
        if (lastUpdate == ETERNAL || d.time() - lastUpdate > updateDurs * d.dur()) {
            cache(d.nar());
            lastUpdate = d.time();
        }
    }

    /** repurposes nar's default punctuation priorities (for input) as the derivation punctuation weighting */
    private void cache(NAR nar) {

        float beliefPri = nar.beliefPriDefault.amp.floatValue();
        float goalPri = nar.goalPriDefault.amp.floatValue();
        float questionPri = nar.questionPriDefault.amp.floatValue();
        float questPri = nar.questPriDefault.amp.floatValue();

        //normalize to 1.0, for postAmp usage
        float sum = Util.sum(beliefPri, goalPri, questionPri, questPri);
        if (sum < ScalarValue.EPSILON) {
            //flat
            this.beliefPri = this.goalPri = this.questionPri = this.questPri = 1f;
        } else {
            this.beliefPri = beliefPri / sum;
            this.goalPri = goalPri / sum;
            this.questionPri = questionPri / sum;
            this.questPri = questPri / sum;
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
    protected float postAmp(Task t, float derivePri, float factor) {
        return derivePri * preAmp(t.punc()) * factor;
    }
}
