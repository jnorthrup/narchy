package nars.control.op;

import jcog.WTF;
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
import nars.term.util.Image;
import nars.time.Tense;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

/**
 * conceptualize and attempt to insert/merge a task to belief table.
 * depending on the status of the insertion, activate links
 * in some proportion of the input task's priority.
 */
public class Remember extends AbstractTask {

    /**
     * root input
     */
    @Deprecated
    public Task input;

    private FasterList<ITask> remembered = null;

    public final boolean store, link, notify;
    public boolean done = false;

    @Nullable
    public static Remember the(Task x, NAR n) {
        return the(x, true, true, true, n);
    }

    @Nullable
    public static Remember the(Task x, boolean store, boolean link, boolean notify, NAR n) {

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
            if (d > 1) {
                if (Param.DEBUG_ENSURE_DITHERED_DT) {
                    Tense.assertDithered(xTerm, d);
                }
                if (Param.DEBUG_ENSURE_DITHERED_OCCURRENCE) {
                    Tense.assertDithered(x, d);
                }
            }
        }

        return new Remember(x, store, link, notify);
    }

    public Remember(Task input, boolean store, boolean link, boolean notify) {
        this.store = store;
        this.link = link;
        this.notify = notify;
        setInput(input);
    }


    /**
     * concept must correspond to the input task
     */
    public void setInput(Task input) {
        if (this.input != input) {
            this.input = input;
            this.done = false;
        }
    }

    @Override
    public String toString() {
        return Remember.class.getSimpleName() + '(' + input + ')';
    }

    @Override
    public ITask next(NAR n) {

        commit(input, store, n);

        return null;
    }

    private void commit(ITask input, NAR n) {
        if (input instanceof Task)
            commit((Task) input, false, n);
        else
            ITask.run(input, n); //inline
    }

    /** TODO check that image dont double link/activate for their product terms */
    private void commit(Task input, boolean store, NAR n) {

        TaskConcept c = null;

        Task rawInput = input;

        Term inputTerm = input.term();
        boolean the = (input == this.input);
        boolean commitProxyOrigin = false;
        if (store) {
            Term imgNormal = Image.imageNormalize(inputTerm);

            if (!inputTerm.equals(imgNormal)) {
                //transparently normalize image tasks
                c = (TaskConcept)
                        //n.conceptualizeDynamic(imgNormal);
                        n.conceptualize(imgNormal);
                        //n.concept(imgNormal);
                if (c == null)
                    return;

                if (input instanceof Image.ImageBeliefTable.ImageTermTask)
                    input = ((Image.ImageBeliefTable.ImageTermTask)input).task; //unwrap existing
                else {
                    input = new SpecialTermTask(imgNormal, input);
                    input.pri(0); //prevent the product task from being activated significantly, because the image task will be emitted at its priority also.

                    boolean cyclic = input.isCyclic();
                    if (cyclic)
                        input.setCyclic(true); //inherit cyclic
                }

                if (the) {
                    this.input = input;
                    commitProxyOrigin = true;
                }
            }
        }

        if (c == null) {
            Concept cc = n.conceptualize(input);
            if (!(cc instanceof TaskConcept)) {
                //may be an atomic functor term, not sure
                if (Param.DEBUG)
                    throw new WTF();
                return;
            }
            c = (TaskConcept) cc;
            if (c == null)
                return;
        }

        if (store) {
            insert(c, n);
        }
        if (!store || commitProxyOrigin) {
            link(rawInput, c, n);
        }
    }


    private void link(Task t, TaskConcept c, NAR n) {

        if (link)
            n.attn.link(t, c, n);

        if (notify)
            new TaskEvent(t).next(n);

    }


    /**
     * attempt to insert into the concept's belief table
     */
    private void insert(TaskConcept c, NAR n) {

        c.add(this, n);

        if (remembered != null && !remembered.isEmpty()) {
            remembered.forEachWith((ITask r, NAR nn) -> {
                if (r.equals(this.input)) //HACK
                    link((Task) r, c, nn); //root
                else
                    commit(r, nn); //sub
            }, n);
            remembered = null;
        }
    }


    public void forget(Task x) {
//        if (x == null)
//            throw new NullPointerException(); //TEMPORARY

        if (remembered != null && remembered.removeInstance(x)) {
            //throw new TODO();
            //TODO filter next tasks with any involving that task
        }

        x.delete();

        if (input == x) {
            input = null;
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

            if (prev!=null && next!=null) {
                dPri = next.priElseZero() - prev.priElseZero();

                if (prev instanceof NALTask)
                    ((NALTask) prev).priCauseMerge(next, CauseMerge.AppendUnique);
            } else {
                dPri = 0; //TODO?
            }

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
    protected static boolean rememberFilter(Task prev, Task next, Task remembered, float dPri, NAR n) {

        long dDurCycles = Math.max(0, next.creation() - prev.creation());
        float dCreationDurs = dDurCycles == 0 ? 0 : (dDurCycles / ((float) n.dur()));

        if (next == remembered && next.isInput())
            return true;

        if (dCreationDurs > Param.REMEMBER_REPEAT_THRESH_DURS) {
            prev.setCreation(next.creation());
            return true;
        }

        return dPri > Param.REMEMBER_REPEAT_PRI_THRESHOLD;

    }

    /**
     * returns which task, if any, to remember on merge
     */
    @Nullable
    protected static Task rememberMerged(Task prev, Task next) {

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


    public final boolean active() {
        //return input == null || (remembered != null && remembered.containsInstance(input));
        return !done;
    }

}
