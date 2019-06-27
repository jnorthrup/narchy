package nars.control.op;

import jcog.data.list.FasterList;
import jcog.pri.Prioritizable;
import nars.NAL;
import nars.NAR;
import nars.Task;
import nars.attention.TaskLinkWhat;
import nars.attention.What;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.control.MetaGoal;
import nars.op.stm.ConjClustering;
import nars.task.AbstractTask;
import nars.task.DynamicTruthTask;
import nars.task.NALTask;
import nars.task.ProxyTask;
import nars.task.proxy.ImageTask;
import nars.task.proxy.SpecialTermTask;
import nars.task.util.TaskException;
import nars.term.Term;
import nars.term.util.Image;
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
public class Remember extends AbstractTask {

    /**
     * root input
     */
    @Deprecated public Task input;

    private FasterList<Task> remembered = null;

    private final boolean store, link, notify;
    public boolean done = false;

    public final NAR nar;

    @Nullable
    public static Remember the(Task x, NAR n) {
        return the(x,
                !(x instanceof DynamicTruthTask) || NAL.belief.DYNAMIC_TRUTH_TASK_STORE,
                true, true, n);
    }

    @Nullable
    public static Remember the(Task x, boolean store, boolean link, boolean notify, NAR n) {

        assert (!x.isCommand());

        assert (x.op().taskable);

        boolean isInput = x.isInput();
        Term xTerm = x.term();

        if (NAL.VOLMAX_RESTRICTS || (NAL.VOLMAX_RESTRICTS_INPUT && isInput)) {
            int termVol = xTerm.volume();
            int maxVol = n.termVolMax.intValue();
            if (termVol > maxVol)
                throw new TaskException(x, "target exceeds volume maximum: " + termVol + " > " + maxVol);
        }

        if (x.isBeliefOrGoal()) {
            if (x.conf() < n.confMin.floatValue()) {
                if (!(x instanceof ProxyTask)) {
                    if (NAL.DEBUG)
                        throw new TaskException(x, "insufficient evidence for non-input Task");
                    else
                        return null;
                }
            }

            if (NAL.test.DEBUG_ENSURE_DITHERED_TRUTH) {
                Truth.assertDithered(x.truth(), n);
            }
        }


        if (NAL.test.DEBUG_ENSURE_DITHERED_DT || NAL.test.DEBUG_ENSURE_DITHERED_OCCURRENCE) {
            int d = n.dtDither();
            if (d > 1) {
                if (NAL.test.DEBUG_ENSURE_DITHERED_DT) {
                    Tense.assertDithered(xTerm, d);
                }
                if (NAL.test.DEBUG_ENSURE_DITHERED_OCCURRENCE) {
                    Tense.assertDithered(x, d);
                }
            }
        }

        return new Remember(x, store, link, notify, n);
    }

    public Remember(Task input, boolean store, boolean link, boolean notify, NAR n) {
        this.store = store;
        this.link = link;
        this.notify = notify;
        this.nar = n;
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
    public Task next(Object w) {
        commit(input, store, (What)w);
        return null;
    }

    /** TODO check that image dont double link/activate for their product terms */
    private void commit(Task input, boolean store, What w) {

        NAR n = w.nar;

        TaskConcept c = null;

        Task rawInput = input;

        Term inputTerm = input.term();
        boolean the = (input == this.input);
        boolean commitProxyOrigin = false;
        if (store) {
            Term imgNormal = Image.imageNormalize(inputTerm).normalize();

            if (!inputTerm.equals(imgNormal)) {
                //transparently normalize image tasks
                c = (TaskConcept)
                        //n.conceptualizeDynamic(imgNormal);
                        n.conceptualize(imgNormal);
                        //n.concept(imgNormal);
                if (c == null)
                    return;

                if (input instanceof ImageTask)
                    input = ((ImageTask)input).task; //unwrap existing
                else {
                    input = Task.withContent(input, imgNormal);
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
                //if (NAL.DEBUG)
                    //throw new WTF();
                return;
            }
            c = (TaskConcept) cc;
        }

        if (store) {
            insert(c, w);
        }
        if (!store || commitProxyOrigin) {
            link(rawInput, c, w);
        }
    }


    private void link(Task t, @Nullable TaskConcept c, What w) {

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
            ((TaskLinkWhat) w).links.link(t);


        if (notify)
            w.emit(t); //notify regardless of whether it was conceptualized, linked, etc..

    }


    /**
     * attempt to insert into the concept's belief table
     */
    private void insert(TaskConcept c, What w) {

        NAR n = w.nar;

        c.add(this, n);

        if (remembered != null && !remembered.isEmpty()) {
            remembered.forEachWith((Task r, NAR nn) -> {
                if (r.equals(this.input)) //HACK
                    link(r, c, w); //root
                else
                    commit(r, false, w); //sub
            }, n);
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

        /** pri(next) - pri(prev) */
        float dPri;

        if (!identity) {

            //assert (!input.isDeleted()); //dont delete just yet

            //TODO decide how much to re-activate
            //TODO consider forgetting rate

            if (prev!=null && next!=null) {
                dPri = next.priElseZero() - prev.priElseZero();

                if (prev instanceof NALTask)
                    Task.merge(prev, next);


            } else {
                dPri = 0; //TODO?
            }

        } else {
            dPri = 0;
        }
        if (next!=null) {

            @Nullable Task r = rememberMerged(prev, next);
            if (r != null) {
                if (rememberFilter(prev, next, r, dPri, this.nar))
                    remember(r);
                else
                    input = null;
            }

            if (!identity && r == null)
                forget(next);
        }

        done = true;
    }

    /**
     * heuristic for determining repeat suppression
     *
     * @param dCreationDurs (creation(next) - creation(prev))/durCycles
     */
    private static boolean rememberFilter(Task prev, Task next, Task remembered, float dPri, NAR n) {

        if (dPri >= NAL.belief.REMEMBER_REPEAT_PRI_THRESHOLD)
            return true;

        long dDurCycles = Math.max(0, next.creation() - prev.creation());
        float dCreationDurs = dDurCycles == 0 ? 0 : (dDurCycles / ((float) n.dtDither()));

        if (next == remembered) {
            return next.isInput();
        }

        if (dCreationDurs > NAL.belief.REMEMBER_REPEAT_THRESH_DITHERS) {
            prev.setCreation(next.creation());
            return true;
        }

        return false;

    }

    /**
     * returns which task, if any, to remember on merge
     */
    @Nullable
    private static Task rememberMerged(Task prev, Task next) {

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
