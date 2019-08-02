package nars.control.op;

import jcog.WTF;
import jcog.data.list.FasterList;
import jcog.pri.Prioritizable;
import jcog.pri.ScalarValue;
import nars.NAL;
import nars.NAR;
import nars.Task;
import nars.attention.What;
import nars.concept.Concept;
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
    @Deprecated public Task input;

    public FasterList<Task> remembered = null;

    public boolean store;
    public boolean link;
    public boolean notify;
    public boolean done = false;

    public final NAR nar;

    public static Remember the(Task x, NAR n) {
        return the(x, true, true, true, n);
    }

    public static Remember the(Task x, boolean store, boolean link, boolean emit, NAR n) {

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

    public Remember(Task input, boolean store, boolean link, boolean notify, NAR n) {
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
        commit(input, store, (What)w);
        return null;
    }

    /** TODO check that image dont double link/activate for their product terms */
    private void commit(Task input, boolean store, What w) {

        NAR n = w.nar;

        boolean the = (input == this.input);

        if (!store)
            link(input, w);
        else {
            Concept cc = n.conceptualize(input);
            if (!(cc instanceof TaskConcept)) {
//                //may be an atomic functor term, not sure
//                if (NAL.DEBUG)
                    throw new WTF();
//                return;
            }


            insert((TaskConcept) cc, w);
        }

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


    /**
     * attempt to insert into the concept's belief table
     */
    private void insert(TaskConcept c, What w) {


        c.add(this);

        if (remembered != null && !remembered.isEmpty()) {
            remembered.forEachWith((Task r, What ww) -> {
                if (r.equals(this.input)) //HACK
                    link(r, ww); //root
                else
                    commit(r, false, ww); //sub
            }, w);
            remembered = null;
        }
    }


    public final void forget(Task x) {
        forget(x, true);
    }

    public void forget(Task x, boolean delete) {

        if (remembered != null && remembered.removeInstance(x)) {
            //throw new TODO();
            //TODO filter next tasks with any involving that task
        }

        if (delete)
            x.delete();

        if (input == x) {
            input = null;
            done = true;
        }
    }

    public void remember(Task x) {
        if (x == input)
            done = true;

        if (this.remembered == null) {
            remembered = new FasterList<>(2);
            remembered.addFast(x);
        } else {
            add(x, this.remembered);
        }
    }


    /**
     * called by belief tables when the input task is matched by an existing equal (or identical) task
     */
    public void merge(Task prev) {

        Task next = this.input;

        boolean identity = prev == next;

        if (next!=null) {

//            @Nullable Task r = rememberMerged(prev, next);
//            if (r != null) {
            if (rememberFilter(prev, next, this.nar)) {
                remember(prev); //if novel: relink, re-emit (but using existing or identical task)
            }

            if (!identity) {

                //assert (!input.isDeleted()); //dont delete just yet

                //TODO decide how much to re-activate
                //TODO consider forgetting rate

                Task.merge(prev, next);
            }

            if (!identity)
                forget(next, true);
        }

        done = true;
    }

    /**
     * heuristic for determining repeat suppression
     *
     */
    private static boolean rememberFilter(Task prev, Task next, NAR n) {

        boolean priChange = false;
        if (next!=prev) {
            float np = next.priElseNeg1();
            float pp = prev.priElseNeg1();
            float dPriPct = (np - pp) / Math.max(ScalarValue.EPSILON, Math.max(np, pp));

            //priority change significant enough
            if (dPriPct >= NAL.belief.REMEMBER_REPEAT_PRI_PCT_THRESHOLD) {
                priChange = true;
            }
        }


        long prevCreation = prev.creation();
        long nextCreation = prev != next ? next.creation() : n.time();

        boolean novel = false;
        if (!priChange) { //dont compare time if pri already detected changed
            long dCycles = Math.max(0, nextCreation - prevCreation);
            float dDithers = dCycles == 0 ? 0 : (dCycles / ((float) n.dtDither()));
            novel = dDithers > NAL.belief.REMEMBER_REPEAT_THRESH_DITHERS;
        }

        if (priChange || novel) {
            prev.setCreation(nextCreation); //renew creation
            return true;
        }

        return false;
    }


    private static boolean add(Prioritizable x, FasterList f) {
        if (x != null) {
            if (!f.containsInstance(x)) {
                f.add(x);
                return true;
            } else {
                return false;
            }
        }
        return false;
    }


    public final boolean active() {
        //return input == null || (remembered != null && remembered.containsInstance(input));
        return !done;
    }

}
