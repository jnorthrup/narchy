package nars.control.op;

import jcog.pri.ScalarValue;
import nars.NAL;
import nars.NAR;
import nars.Task;
import nars.attention.What;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.control.MetaGoal;
import nars.task.util.TaskException;
import nars.time.Tense;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

import static nars.Op.BELIEF;
import static nars.Op.GOAL;

/**
 * conceptualize and attempt to insert/merge a task to belief table.
 * depending on the status of the insertion, activate links
 * in some proportion of the input task's priority.
 */
public class Remember {

    /**
     * root input
     */
    public Task input;
    public @Nullable Task result = null;

    public final boolean store;
    public boolean link;
    public boolean notify;

    public final What what;

    public static @Nullable Remember the(Task x, What w) {
        return the(x, true, true, true, w);
    }


    public static @Nullable Remember the(Task x, boolean store, boolean link, boolean emit, What w) {

        verify(x, w.nar);

        return new Remember(x, store, link, emit, w);
    }

    /** misc verification tests which are usually disabled */
    protected static void verify(Task x, NAR n) {

        if ((NAL.VOLMAX_RESTRICTS) && (x.isInput() || !NAL.VOLMAX_RESTRICTS_INPUT)) {
            int termVol = x.term().volume();
            int maxVol = n.termVolMax.intValue();
            if (termVol > maxVol)
                throw new TaskException("target exceeds volume maximum: " + termVol + " > " + maxVol, x);
        }


        if (NAL.test.DEBUG_ENSURE_DITHERED_TRUTH && x.isBeliefOrGoal()) {
//            if (x.conf() < n.confMin.floatValue()) {
//                if (!(x instanceof ProxyTask)) {
//                    if (NAL.DEBUG)
//                        throw new TaskException(x, "insufficient evidence for non-input Task");
//                    else
//                        return null;
//                }
//            }

            Truth.assertDithered(x.truth(), n);
        }


        if (NAL.test.DEBUG_ENSURE_DITHERED_DT || NAL.test.DEBUG_ENSURE_DITHERED_OCCURRENCE) {
            int d = n.dtDither();
            if (d > 1) {
                if (NAL.test.DEBUG_ENSURE_DITHERED_DT)
                    Tense.assertDithered(x.term(), d);
                if (NAL.test.DEBUG_ENSURE_DITHERED_OCCURRENCE)
                    Tense.assertDithered(x, d);
            }
        }
    }

    Remember(Task input, boolean store, boolean link, boolean notify, What w) {
        this.store = store;
        this.link = link;
        this.notify = notify;
        this.what = w;
        this.input = input;

        if (store) {
            if (!store())
                return;
        } else {
            result = input;
        }

        if (complete() && !this.result.isDeleted()) {
            link();
        }

        if (result!=input)
            input.delete();

    }

    @Override
    public String toString() {
        return Remember.class.getSimpleName() + '(' + input + ')';
    }


    public boolean store() {
        Concept cc = concept();
        if (!(cc instanceof TaskConcept))
            return false;
        else {
            ((TaskConcept)cc).remember(this);
            return true;
        }
    }

    protected Concept concept() {
        boolean conceptualize = true;
        return what.nar.concept(input,conceptualize);
    }

    public final boolean complete() {
        return result!=null;
    }

    protected void link() {

        Task t = this.result;
        NAR n = nar();

        /* 1. resolve */
//        Termed cc = c == null ? t : c;
//        c = (TaskConcept) n.conceptualize(cc);

        n.emotion.perceive(t);

        byte punc = t.punc();
        if ((int) punc == (int) BELIEF || (int) punc == (int) GOAL) {
            float value = NAL.valueBeliefOrGoal(t);
            ((int) punc == (int) BELIEF ? MetaGoal.Believe : MetaGoal.Desire)
                    .learn(t, value, n);

//            if (t.isGoal()) {
//                MetaGoal.Action.learn(
//                        t.isEternal() ? t.conf() * n.dur()
//                                :
//                                (float) TruthIntegration.evi(t),
//                        n.control.why, t.why());
//            }
        }

        if (link) {
            //emit the result, but using the input pri (which may be different on merge)
            what.link(t, input.priElseZero());
        }

        if (notify)
            what.emit(t); //notify regardless of whether it was conceptualized, linked, etc..

    }

    public void forget(Task x) {
        x.delete();
        if (x == input)
            result = x; //complete
    }

    public void remember(Task x) {
        result = x;
    }


    /**
     * called by belief tables when the input task is matched by an existing equal (or identical) task
     */
    public void merge(Task y) {

        boolean identity = y == input;
        if (identity || y.equals(input)) {

            if (filter(y))
                remember(y); //if novel: relink, re-emit (but using existing or identical task)
            else {
                link = false;
                notify = false;
            }

            if (!identity) {
                Task.merge(y, input);
            }
        }

        //result = y;
    }

    /**
     * heuristic for determining repeat suppression
     *
     */
    private boolean filter(Task prev) {

        Task next = input;
        boolean priChange = false;
        if (next!=prev) {
            float np = next.priElseZero();
            float pp = prev.priElseZero();
            float dPriPct = (np - pp) / Math.max(ScalarValue.EPSILON, Math.max(np, pp));

            //priority change significant enough
            if (dPriPct >= NAL.belief.REMEMBER_REPEAT_PRI_PCT_THRESHOLD) {
                priChange = true;
            }
        }

        long prevCreation = prev.creation();
        long nextCreation = prev != next ? next.creation() : what.time();

        boolean novel;
        if (priChange) { //dont compare time if pri already detected changed
            novel = true;
        } else {
            long dCycles = Math.max(0L, nextCreation - prevCreation);
            float dur = what.dur();
            float dDurs = dCycles == 0L ? (float) 0 : ((float) dCycles / dur); //maybe what.dur()
            novel = dDurs >= (float) NAL.belief.REMEMBER_REPEAT_THRESH_DURS;
        }

        if (novel) {
            prev.setCreation(nextCreation); //renew creation
            return true;
        }

        return false;
    }


    /** current time */
    public final long time() {
        return what.time();
    }

    public final NAR nar() {
        return what.nar;
    }


}
