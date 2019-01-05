package nars.control.op;

import jcog.data.list.FasterList;
import nars.NAR;
import nars.Op;
import nars.Param;
import nars.Task;
import nars.concept.Concept;
import nars.concept.TaskConcept;
import nars.control.CauseMerge;
import nars.task.AbstractTask;
import nars.task.ITask;
import nars.task.NALTask;
import nars.task.TaskProxy;
import nars.task.util.TaskException;
import nars.term.Term;
import nars.time.Tense;
import nars.truth.Truth;
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
    @Nullable
    public FasterList<Task> forgotten = null;
    public TaskConcept concept;


//    static final Logger logger = LoggerFactory.getLogger(Remember.class);

    @Nullable
    public static Remember the(Task input, NAR n) {

        assert (input.op().taskable);


        //verify dithering
        if (Param.DEBUG_ENSURE_DITHERED_TRUTH) {
            if (!input.isInput()) {
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
                    if (s!=ETERNAL) {
                        if (Tense.dither(s, d)!=s)
                            throw WTF(input + " has non-dithered start occurrence");
                        long e = input.end();
                        if (e!=s && Tense.dither(e, d)!=e)
                            throw WTF(input + " has non-dithered end occurrence");
                    }
                }
            }
        }

        if (!input.isCommand()) {

            if (Param.VOLMAX_RESTRICTS_INPUT) {
                int termVol = input.term().volume();
                int maxVol = n.termVolumeMax.intValue();
                if (termVol > maxVol)
                    throw new TaskException(input, "term exceeds volume maximum: " + termVol + " > " + maxVol);
            }

            if((!input.isInput() || input instanceof TaskProxy) && input.isBeliefOrGoal() && input.conf() < n.confMin.floatValue()) {
                if(Param.DEBUG)
                    throw new TaskException(input, "insufficient evidence for non-input Task");
                else
                    return null;
            }

            Concept c = n.conceptualize(input);
            if (c!=null) {
                if (!(c instanceof TaskConcept)) {
                    if (Param.DEBUG)
                        throw new TaskException(input, c + " is not a TaskConcept");
                    else
                        return null;
                }

                return new Remember(input, (TaskConcept) c);
            }
        }

        return null;
    }

    public Remember(Task input, TaskConcept c) {
        setInput(input, c);
    }


    /** concept must correspond to the input task */
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

        if (forgotten!=null)
            forgotten.forEach(Task::delete);

        if (remembered!=null && !remembered.isEmpty())
            commit(n);

        return null;
    }

    private void commit(NAR n) {
        Term conceptTerm = concept!=null ? concept.term() : null;

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
            if (t!=null)
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
        if (remembered!=null && remembered.removeInstance(x)) {
            //throw new TODO();
            //TODO filter next tasks with any involving that task
        }

        if (forgotten==null)
            forgotten = new FasterList(1);

        add(x, this.forgotten);
        if (input == x) {
            input = null; concept = null;
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


    public void merge(Task existing) {

        Task input = this.input;

        if (existing != input) {

            //assert (!input.isDeleted()); //dont delete just yet

            //TODO decide how much to re-activate
            //TODO consider forgetting rate

            if (existing instanceof NALTask)
                ((NALTask) existing).priCauseMerge(input, CauseMerge.AppendUnique);


            remember(existing);

////        if (input.isInput())
////            remember(existing); //link and emit
////        else {
////            //next(new TaskLinkTask(existing, concept)); //just link
////        }

//            //remember(new TaskLinkTask(existing, concept)); //just link TODO proportionally to pri difference?
//            //2. update tasklink
//            float pri;
//            if (Param.tasklinkMerge == PriMerge.max)
//                pri = Math.max(existing.priElseZero(), input.priElseZero());
//            else if (Param.tasklinkMerge == PriMerge.plus)
//                pri = input.priElseZero() - existing.priElseZero();
//            else
//                throw new TODO();
//            if (pri > ScalarValue.EPSILON) {
//                if (concept == null)
//                    throw new TODO();
//
//                TaskLink.link(
//                        TaskLink.the(existing, true, true,
//                                pri
//                                , null),
//                        concept);
//            }
//            remember(new TaskEvent(existing));

            forget(input);


        } else {
            input = null;
        }


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


    public boolean forgotten(Task input) {
        return forgotten!=null && forgotten.containsInstance(input);
    }

    public final boolean done() {
        return (remembered != null && remembered.containsInstance(input)) || input == null;
    }

}
