package nars.derive.pri;

import nars.Op;
import nars.Task;

import java.util.Arrays;

import static nars.Op.*;

/** TODO strength parameter
 *  TODO toggle for preamp and postamp - both dont need applied necessarily
 * */
public class DefaultPuncWeightedDerivePri extends DefaultDerivePri {

    /** cache of punctuation priorities */
    public transient float beliefPri;
    public transient float goalPri;
    public transient float questionPri;
    public transient float questPri;

    public float[] opPri = new float[Op.values().length];

    public DefaultPuncWeightedDerivePri() {
        Arrays.fill(opPri, 1f); //TODO only taskable ops else Float.NaN
        beliefPri = goalPri = questionPri = questPri = 1;
    }

//    long lastUpdate = ETERNAL;
//    static final float updateDurs = 1;
//    @Override
//    public void reset(Derivation d) {
//        if (lastUpdate == ETERNAL || d.time - lastUpdate > updateDurs * d.dur) {
//            cache(d.nar);
//            lastUpdate = d.time;
//        }
//    }

//    /** repurposes nar's default punctuation priorities (for input) as the derivation punctuation weighting */
//    private void cache(NAR nar) {
//
//        float beliefPri = nar.beliefPriDefault.pri();
//        float goalPri = nar.goalPriDefault.pri();
//        float questionPri = nar.questionPriDefault.pri();
//        float questPri = nar.questPriDefault.pri();
//
//        //normalize to 1.0, for postAmp usage
//        float sum = Util.sum(beliefPri, goalPri, questionPri, questPri);
//        if (sum < ScalarValue.EPSILON) {
//            //flat
//            this.beliefPri = this.goalPri = this.questionPri = this.questPri = 1f;
//        } else {
//            this.beliefPri = beliefPri / sum;
//            this.goalPri = goalPri / sum;
//            this.questionPri = questionPri / sum;
//            this.questPri = questPri / sum;
//        }
//
//    }



    public float puncPri(byte conclusionPunc) {
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
        return derivePri * opPri[t.op().id] * puncPri(t.punc()) * factor;
    }
}
