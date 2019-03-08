package nars.control.op;

import jcog.data.list.FasterList;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.control.CauseMerge;
import nars.op.stm.ConjClustering;
import nars.task.*;
import nars.task.proxy.SpecialTermTask;
import nars.task.util.TaskException;
import nars.term.Term;
import nars.time.Tense;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

/**
 * conceptualize and attempt to insert/merge a task to belief table.
 * depending on the status of the insertion, activate links
 * in some proportion of the input task's priority.
 */
public class Remember extends AbstractTask {

    public Task input;
    /** TODO HACK */
    @Deprecated private transient TaskConcept inputConcept;


    private FasterList<ITask> remembered = null;


    public final boolean store, link, notify;
    public boolean done = false;


    @Nullable public static Remember the(Task x, NAR n) {
        return the(x, true, true, true, n);
    }

    @Nullable public static Remember the(Task x, boolean store, boolean link, boolean notify, NAR n) {

        assert (!x.isCommand());

        assert (x.op().taskable);

        boolean isInput = x.isInput();
        Term xTerm = x.term();

        if (Param.VOLMAX_RESTRICTS || (Param.VOLMAX_RESTRICTS_INPUT && isInput)) {
            int termVol = xTerm.volume();
            int maxVol = n.termVolumeMax.intValue();
            if (termVol > maxVol)
                throw new TaskException(x, "target exceeds volume maximum: " + termVol + " > " + maxVol);
        }

        if (x.isBeliefOrGoal() && x.conf() < n.confMin.floatValue()) {
            if (!(x instanceof TaskProxy)) {
                if (Param.DEBUG)
                    throw new TaskException(x, "insufficient evidence for non-input Task");
                else
                    return null;
            }
        }


        if (Param.DEBUG_ENSURE_DITHERED_TRUTH) {
            Truth.assertDithered(x.truth(), n);
        }

        if (Param.DEBUG_ENSURE_DITHERED_DT || Param.DEBUG_ENSURE_DITHERED_OCCURRENCE) {
            int d = n.dtDither();
            if(d > 1) {
                if (Param.DEBUG_ENSURE_DITHERED_DT) {
                    Tense.assertDithered(xTerm, d);
                }
                if (Param.DEBUG_ENSURE_DITHERED_OCCURRENCE) {
                    Tense.assertDithered(x, d);
                }
            }
        }

        Concept c = n.conceptualize(x);
        if (c != null) {
            if (!(c instanceof TaskConcept)) {
                if (isInput || Param.DEBUG)
                    throw new TaskException(x, c + " is not a TaskConcept: " + c.getClass());
                else
                    return null;
            }

            return new Remember(x, store, link, notify, (TaskConcept) c);
        } else {
            if (isInput) {
                //if (Param.DEBUG) {
                    throw new TaskException(x, "input not conceptualized");
                //}
            }
            return null;
        }
    }

    public Remember(Task input, boolean store, boolean link, boolean notify, TaskConcept c) {
        this.store = store;
        this.link = link;
        this.notify = notify;
        setInput(input, c);
    }


    /**
     * concept must correspond to the input task
     */
    public void setInput(Task input, @Nullable TaskConcept c) {
        if (this.input!=input) {
            this.input = input;
            this.inputConcept = c;
            this.done = false;
        }
    }

    @Override
    public String toString() {
        return "Remember(" + input + ')';
    }

    @Override
    public ITask next(NAR n) {

        if (store) {
            tryAddAndCommit(n);
        } else {
            commit(input, n);
        }

        return null;
    }



    private void commit(ITask t, NAR n) {
        if (t instanceof Task) {
            commit((Task) t, n);
        } else {
            ITask.run(t, n); //inline
        }
    }

    private void commit(Task t, NAR n) {


        if (link) {
            Concept c = (this.inputConcept!=null) &&
                    ((this.input==t) || (inputConcept.term().equals(t.term().concept()))) ? inputConcept : null;
            new TaskLinkTask(t, c).next(n);
        }

        if (notify)
            new TaskEvent(t).next(n);
    }


    /**
     * attempt to insert into the concept's belief table
     */
    private void tryAddAndCommit(NAR n) {
        inputConcept.add(this, n);
        if (remembered != null && !remembered.isEmpty()) {
            remembered.forEachWith(this::commit, n);
            remembered = null;
        }
    }


    public void forget(Task x) {
        if (remembered != null && remembered.removeInstance(x)) {
            //throw new TODO();
            //TODO filter next tasks with any involving that task
        }

        x.delete();

        if (input == x) {
            input = null;
            inputConcept = null;
            done = true;
        }
    }

    public void remember(ITask x) {
        if (x == input)
            done = true;

        if (this.remembered == null) {
            remembered = new FasterList<>(2);
            remembered.addWithoutResizeTest(x);
        } else {
            add(x, this.remembered);
        }
    }


    /**
     * called by belief tables when the input task is matched by an existing equal (or identical) task
     */
    public void merge(Task prev, NAR n) {

        Task next = this.input;

        boolean identity = prev == next;

        /** pri(next) - pri(prev) */
        float dPri;

        if (!identity) {

            //assert (!input.isDeleted()); //dont delete just yet

            //TODO decide how much to re-activate
            //TODO consider forgetting rate

            dPri = next.priElseZero() - prev.priElseZero();

            if (prev instanceof NALTask)
                ((NALTask) prev).priCauseMerge(next, CauseMerge.AppendUnique);


        } else {
            dPri = 0;
        }

        @Nullable Task r = rememberMerged(prev, next);
        if (r != null) {
            if (rememberFilter(prev, next, r, dPri, n))
                remember(r);
            else
                input = null;
        }

        if (!identity && r == null)
            forget(next);

        done = true;
    }

    /**
     * heuristic for determining repeat suppression
     *
     * @param dCreationDurs (creation(next) - creation(prev))/durCycles
     */
    @Nullable
    protected boolean rememberFilter(Task prev, Task next, Task remembered, float dPri, NAR n) {

        long dDurCycles = Math.max(0, next.creation() - prev.creation());
        float dCreationDurs = dDurCycles == 0 ? 0 : (dDurCycles / ((float) n.dur()));

        if (next==remembered && next.isInput())
            return true;

        if (dCreationDurs > Param.REMEMBER_REPEAT_THRESH_DURS) {
            prev.setCreation(next.creation());
            return true;
        }

        if (dPri > Param.REMEMBER_REPEAT_PRI_THRESHOLD)
            return true;

        return false;
    }

    /**
     * returns which task, if any, to remember on merge
     */
    @Nullable
    protected Task rememberMerged(Task prev, Task next) {

        if (next instanceof DynamicTruthTask)
            return null;
        if (next instanceof ConjClustering.STMClusterTask)
            return null;
//        if (next instanceof SignalTask)
//            return null; //TODO determine if this works

        if (next.isInput())
            return prev;

        if (next instanceof SpecialTermTask) //Image belief table
            return ((SpecialTermTask) next).task;

        return prev;
    }


    public final void reject() {
        forget(input);
    }


    private static boolean add(ITask x, FasterList f) {
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


    public final boolean done() {
        //return input == null || (remembered != null && remembered.containsInstance(input));
        return done;
    }

}
