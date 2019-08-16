package nars.control.op;

import jcog.pri.ScalarValue;
import nars.NAL;
import nars.NAR;
import nars.Task;
import nars.attention.What;
import nars.concept.TaskConcept;
import nars.control.MetaGoal;
import nars.task.AbstractTask;
import nars.task.util.TaskException;
import nars.term.Term;
import nars.time.Tense;
import nars.truth.Truth;

import static nars.Op.BELIEF;
import static nars.Op.GOAL;

/**
 * conceptualize and attempt to insert/merge a task to belief table.
 * depending on the status of the insertion, activate links
 * in some proportion of the input task's priority.
 */
public class Remember extends AbstractTask {

    /**
     * root input
     */
    public Task input;

//    public FasterList<Task> remembered = null;

    public boolean store;
    public boolean link;
    public boolean notify;
    public boolean done = false;

    public final NAR nar;

    public static Remember the(Task x, NAR n) {
        return the(x, true, true, true, n);
    }

    private static Remember the(Task x, boolean store, boolean link, boolean emit, NAR n) {

        Term xTerm = x.term();
//        assert (!x.isCommand());
//        assert (xTerm.op().taskable);

        boolean input = x.isInput();
        if ((NAL.VOLMAX_RESTRICTS) && (input || !NAL.VOLMAX_RESTRICTS_INPUT)) {
            int termVol = xTerm.volume();
            int maxVol = n.termVolMax.intValue();
            if (termVol > maxVol)
                throw new TaskException(x, "target exceeds volume maximum: " + termVol + " > " + maxVol);
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
                    Tense.assertDithered(xTerm, d);
                if (NAL.test.DEBUG_ENSURE_DITHERED_OCCURRENCE)
                    Tense.assertDithered(x, d);
            }
        }

        return new Remember(x, store, link, emit, n);
    }

    private Remember(Task input, boolean store, boolean link, boolean notify, NAR n) {
        this.store = store;
        this.link = link;
        this.notify = notify;
        this.nar = n;
        this.input = input;
    }

    @Override
    public String toString() {
        return Remember.class.getSimpleName() + '(' + input + ')';
    }

    @Override
    public Task next(Object w) {

        if (store) {
           TaskConcept cc = (TaskConcept) ((What)w).nar.conceptualize(input);
           if (cc==null)
               return null;

           cc.remember(this);
       } else {
           done = true;
       }

        if (done && !this.input.isDeleted()) {
            link(this.input, (What)w);
        }

        return null;
    }


    private void link(Task t, What w) {

        NAR n = w.nar;

        /* 1. resolve */
//        Termed cc = c == null ? t : c;
//        c = (TaskConcept) n.conceptualize(cc);

        n.emotion.perceive(t);

        byte punc = t.punc();
        if (punc == BELIEF || punc == GOAL) {
            (punc == BELIEF ? MetaGoal.Believe : MetaGoal.Desire)
                    .learn(t.priElseZero(), n.control.why, t.why());

//            if (t.isGoal()) {
//                MetaGoal.Action.learn(
//                        t.isEternal() ? t.conf() * n.dur()
//                                :
//                                (float) TruthIntegration.evi(t),
//                        n.control.why, t.why());
//            }
        }

        if (link)
            w.link(t);

        if (notify)
            w.emit(t); //notify regardless of whether it was conceptualized, linked, etc..

    }

    public void forget(Task x) {
        x.delete();
    }

    public void remember(Task x) {
        input = x;
        done = true;
    }


    /**
     * called by belief tables when the input task is matched by an existing equal (or identical) task
     */
    public void merge(Task prev) {

        Task next = this.input;
        boolean identity = prev == next;
        if (identity || prev.equals(next)) {

            if (filter(prev, next, this.nar))
                remember(prev); //if novel: relink, re-emit (but using existing or identical task)

            if (!identity) {
                Task.merge(prev, next);
                forget(next);
            }
        }

        done = true;
    }

    /**
     * heuristic for determining repeat suppression
     *
     */
    private static boolean filter(Task prev, Task next, NAR n) {

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
        long nextCreation = prev != next ? next.creation() : n.time();

        boolean novel;
        if (priChange) { //dont compare time if pri already detected changed
            novel = true;
        } else {
            long dCycles = Math.max(0, nextCreation - prevCreation);
            float dDithers = dCycles == 0 ? 0 : (dCycles / ((float) n.dtDither()));
            novel = dDithers > NAL.belief.REMEMBER_REPEAT_THRESH_DITHERS;
        }

        if (novel) {
            prev.setCreation(nextCreation); //renew creation
            return true;
        }

        return false;
    }


}
