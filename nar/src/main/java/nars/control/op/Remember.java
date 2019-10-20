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
            var termVol = x.term().volume();
            var maxVol = n.termVolMax.intValue();
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
            var d = n.dtDither();
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
        var cc = concept();
        if (!(cc instanceof TaskConcept))
            return false;
        else {
            ((TaskConcept)cc).remember(this);
            return true;
        }
    }

    protected Concept concept() {
        var conceptualize = true;
        return what.nar.concept(input,conceptualize);
    }

    public final boolean complete() {
        return result!=null;
    }

    protected void link() {

        var t = this.result;
        var n = nar();

        /* 1. resolve */
//        Termed cc = c == null ? t : c;
//        c = (TaskConcept) n.conceptualize(cc);

        n.emotion.perceive(t);

        var punc = t.punc();
        if (punc == BELIEF || punc == GOAL) {
            var value = NAL.valueBeliefOrGoal(t);
            (punc == BELIEF ? MetaGoal.Believe : MetaGoal.Desire)
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

        var identity = y == input;
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

        var next = input;
        var priChange = false;
        if (next!=prev) {
            var np = next.priElseZero();
            var pp = prev.priElseZero();
            var dPriPct = (np - pp) / Math.max(ScalarValue.EPSILON, Math.max(np, pp));

            //priority change significant enough
            if (dPriPct >= NAL.belief.REMEMBER_REPEAT_PRI_PCT_THRESHOLD) {
                priChange = true;
            }
        }

        var prevCreation = prev.creation();
        var nextCreation = prev != next ? next.creation() : what.time();

        boolean novel;
        if (priChange) { //dont compare time if pri already detected changed
            novel = true;
        } else {
            var dCycles = Math.max(0, nextCreation - prevCreation);
            var dur = what.dur();
            var dDurs = dCycles == 0 ? 0 : (dCycles / dur); //maybe what.dur()
            novel = dDurs >= NAL.belief.REMEMBER_REPEAT_THRESH_DURS;
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
