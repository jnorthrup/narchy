package nars.control.op;

import jcog.data.list.FasterList;
import nars.NAR;
import nars.Op;
import nars.Param;
import nars.Task;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.control.CauseMerge;
import nars.op.stm.ConjClustering;
import nars.task.AbstractTask;
import nars.task.ITask;
import nars.task.NALTask;
import nars.task.TaskProxy;
import nars.task.proxy.SpecialTermTask;
import nars.task.signal.SignalTask;
import nars.task.util.TaskException;
import nars.term.Term;
import nars.time.Tense;
import nars.truth.Truth;
import nars.truth.dynamic.DynEvi;
import org.jetbrains.annotations.Nullable;

import static jcog.WTF.WTF;
import static nars.time.Tense.ETERNAL;

/**
 * conceptualize and attempt to insert/merge a task to belief table.
 * depending on the status of the insertion, activate links
 * in some proportion of the input task's priority.
 */
public class Remember extends AbstractTask {
    public Task input;

    public FasterList<ITask> remembered = null;

    public TaskConcept concept;


//    static final Logger logger = LoggerFactory.getLogger(Remember.class);

    @Nullable
    public static Remember the(Task input, NAR n) {

        assert (!input.isCommand());

        assert (input.op().taskable);

        boolean isInput = input.isInput();
        if (Param.VOLMAX_RESTRICTS || (Param.VOLMAX_RESTRICTS_INPUT && isInput)) {
            int termVol = input.term().volume();
            int maxVol = n.termVolumeMax.intValue();
            if (termVol > maxVol)
                throw new TaskException(input, "term exceeds volume maximum: " + termVol + " > " + maxVol);
        }

        if ((!isInput || input instanceof TaskProxy) && input.isBeliefOrGoal() && input.conf() < n.confMin.floatValue()) {
            if (Param.DEBUG)
                throw new TaskException(input, "insufficient evidence for non-input Task");
            else
                return null;
        }


        //verify dithering
        if (Param.DEBUG_ENSURE_DITHERED_TRUTH) {
            if (!isInput) {
                Truth t = input.truth();
                if (t != null) {
                    Truth d = t.dithered(n);
                    if (!t.equals(d))
                        throw WTF("not dithered");
                }
            }
        }
        if (Param.DEBUG_ENSURE_DITHERED_DT || Param.DEBUG_ENSURE_DITHERED_OCCURRENCE) {
            int d = n.timeResolution.intValue();
            if (d > 1) {
                if (Param.DEBUG_ENSURE_DITHERED_DT) {
                    Term x = input.term();
                    if (x.hasAny(Op.Temporal)) {
                        x.recurseTerms((Term z) -> z.hasAny(Op.Temporal), xx -> {
                            int zdt = xx.dt();
                            if (!Tense.dtSpecial(zdt)) {
                                if (zdt != Tense.dither(zdt, d))
                                    throw WTF(input + " contains non-dithered DT in subterm " + xx);
                            }
                            return true;
                        }, null);
                    }
                }
                if (Param.DEBUG_ENSURE_DITHERED_OCCURRENCE) {
                    long s = input.start();
                    if (s != ETERNAL) {
                        if (Tense.dither(s, d) != s)
                            throw WTF(input + " has non-dithered start occurrence");
                        long e = input.end();
                        if (e != s && Tense.dither(e, d) != e)
                            throw WTF(input + " has non-dithered end occurrence");
                    }
                }
            }
        }


        Concept c = n.conceptualize(input);
        if (c != null) {
            if (!(c instanceof TaskConcept)) {
                if (Param.DEBUG || isInput)
                    throw new TaskException(input, c + " is not a TaskConcept: " + c.getClass());
                else
                    return null;
            }

            return new Remember(input, (TaskConcept) c);
        } else {
            if (isInput)
                throw new TaskException(input, "not conceptualized");
            return null;
        }
    }

    public Remember(Task input, TaskConcept c) {
        setInput(input, c);
    }


    /**
     * concept must correspond to the input task
     */
    public void setInput(Task input, @Nullable TaskConcept c) {
        if (c != null) {
            this.input = input;
            this.concept = c;
        }
    }

    @Override
    public String toString() {
        return "Remember(" + input + ')';
    }

    @Override
    public ITask next(NAR n) {

        add(n);

        if (remembered != null && !remembered.isEmpty())
            commit(n);

        return null;
    }

    private void commit(NAR n) {
        Term conceptTerm = concept != null ? concept.term() : null;

        for (ITask r : remembered) {
            if (r instanceof Task) {
                commit(n, conceptTerm, (Task) r);
            } else {
                ITask.run(r, n);
            }
        }

        remembered = null;
    }

    private void commit(NAR n, Term conceptTerm, Task r) {
        Task rr = r;

        if (tasklink()) {
            Concept c = conceptTerm != null && conceptTerm.equals(rr.term().concept()) ? concept : null;
            TaskLinkTask t = tasklink(rr, c);
            if (t != null)
                t.next(n);
        }

        if (taskevent())
            new TaskEvent(rr).next(n);
    }


    protected TaskLinkTask tasklink(Task rr, Concept c) {
        return new TaskLinkTask(rr, c);
    }

    protected boolean tasklink() {
        return true;
    }

    protected boolean taskevent() {
        return true;
    }

    /**
     * attempt to insert into the concept's belief table
     */
    protected void add(NAR n) {
        concept.add(this, n);
    }


    //TODO: private static final class ListTask extends FasterList<ITask> extends NativeTask {

//    @Deprecated
//    private static final class Commit extends AbstractTask {
//
//        FasterList<Task> forgotten, remembered;
//
//        public Commit(FasterList<Task> forgotten, FasterList<Task> remembered) {
//            super();
//            this.forgotten = forgotten;
//            this.remembered = remembered;
//        }
//
//        @Override
//        public ITask next(NAR n) {
//
//        }
//    }


    public void forget(Task x) {
        if (remembered != null && remembered.removeInstance(x)) {
            //throw new TODO();
            //TODO filter next tasks with any involving that task
        }

        x.delete();

        if (input == x) {
            input = null;
            concept = null;
        }
    }

    public void remember(ITask x) {
        if (this.remembered == null) {
            remembered = new FasterList<>(2);
            remembered.add(x);
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
            long dDurCycles = Math.max(0, next.creation() - prev.creation());
            float dCreationDurs = dDurCycles == 0 ? 0 : (dDurCycles / ((float) n.dur()));
            r = rememberFilter(prev, next, r, dPri, dCreationDurs);

        }
        if (r != null)
            remember(r);

        if (!identity && r == null) {
            forget(next);
        } else {
            //just disable further input
            this.input = null;
        }

    }

    /**
     * heuristic for determining repeat suppression
     *
     * @param dCreationDurs (creation(next) - creation(prev))/durCycles
     */
    @Nullable
    protected Task rememberFilter(Task prev, Task next, Task remembered, float dPri, float dCreationDurs) {

        if (dCreationDurs > Param.REMEMBER_REPEAT_THRESH_DURS) {
            prev.setCreation(next.creation());
            return remembered;
        }

        if (dPri >= Param.REMEMBER_REPEAT_PRI_THRESHOLD)
            return remembered;

        return null;
    }

    /**
     * returns which task, if any, to remember on merge
     */
    @Nullable
    protected Task rememberMerged(Task prev, Task next) {

        if (next instanceof DynEvi.DynamicTruthTask)
            return null;
        if (next instanceof ConjClustering.STMClusterTask)
            return null;
        if (next instanceof SignalTask)
            return null; //TODO determine if this works

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
        return input == null || (remembered != null && remembered.containsInstance(input));
    }

}
