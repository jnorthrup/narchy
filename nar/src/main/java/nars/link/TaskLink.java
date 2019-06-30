package nars.link;

import jcog.TODO;
import jcog.Util;
import jcog.data.graph.path.FromTo;
import jcog.decide.Roulette;
import jcog.pri.UnitPrioritizable;
import jcog.pri.op.PriMerge;
import jcog.pri.op.PriReturn;
import nars.NAL;
import nars.NAR;
import nars.Task;
import nars.derive.model.Derivation;
import nars.table.TaskTable;
import nars.task.NALTask;
import nars.term.Term;
import nars.time.When;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.function.Predicate;

import static nars.Op.*;
import static nars.Task.p;

/**
 * the function of a tasklink is to be a prioritizable strategy for resolving a Task in a NAR.
 * this does not mean that it must reference a specific Task but certain properties of it
 * that can be used ot dynamically match a Task on demand.
 * <p>
 * note: seems to be important for Tasklink to NOT implement Termed when use with common Map's with Termlinks
 */
public interface TaskLink extends UnitPrioritizable, FromTo<Term, TaskLink> {

    TaskLink[] EmptyTaskLinkArray = new TaskLink[0];

    default /* final */ float priPunc(byte punc) { return priIndex(Task.i(punc)); }

    /** index will be either 0, 1, 2, or 3 */
    float priIndex(byte index);


    float getAndSetPriPunc(byte punc, float next);


//    static void link(TaskLink x, NAR nar, @Nullable OverflowDistributor<Bag> overflow) {
//
//        Bag<TaskLink, TaskLink> b = ((AbstractConceptIndex) nar.concepts).active;
//
//        if (overflow != null) {
//            MutableFloat o = new MutableFloat();
//
//            TaskLink yy = b.put(x, o);
//
//            if (o.floatValue() > EPSILON) {
//                overflow.overflow(b, o.floatValue(),
//                        (yy != null) ?
//                                1f - yy.priElseZero()
//                                :
//                                1 //assume it needs as much as it can get
//                );
//            }
//        } else {
//            b.putAsync(x);
//        }
//    }

    /**
     * sample punctuation by relative priority
     * returns 0 for none
     */
    default byte priPunc(Random rng) {
        int i = Roulette.selectRouletteCached(4, (j) -> priPunc(p(j)), rng);
        if (i != -1)
            return p(i);
        else
            return 0;
    }

    default byte punc(Random rng) {
        return priPunc(rng);
    }

    @Nullable default /* final */Task get(When<NAR> when) {
        return get(punc(when.x.random()), when, null);
    }

    @Nullable default Task get(byte punc, When<NAR> when, Predicate<Task> filter) {
        if (punc == 0)
            return null; //flat-lined tasklink

        Term x = from();

        NAR n = when.x;

        boolean beliefOrGoal = punc == BELIEF || punc == GOAL;
        TaskTable table =
                //n.concept(t);
                //n.conceptualizeDynamic(x);
                //beliefOrGoal ? n.conceptualizeDynamic(x) : n.beliefDynamic(x);
                n.tableDynamic(x, punc);

        if (table == null || table.isEmpty()) {

            return null;
        } else {


            Task y;
            if (beliefOrGoal) {

                y = table.match(when, null, filter, false);
                if (y == null)
                    y = table.sample(when, null, filter);

            } else {
                y = table.sample(when, null, filter);
            }

            if (y == null) {
                if (!beliefOrGoal) {
                    //form question?
                    float qpri = NAL.TASKLINK_GENERATED_QUESTION_PRI_RATE;
                    if (qpri > Float.MIN_NORMAL && Task.validTaskTerm(x, punc, true)) {
                        y = NALTask.the(x, punc, null, when);
                        y.pri(priPunc(punc) * qpri);
                    }
                }

//                if (y == null)
//                    delete(punc); //TODO try another punc?
            }

            return y;

//            if (task!=null) {
//                    byte punc = task.punc();
//                    //dynamic question answering
//                    Term taskTerm = task.target();
//                    if ((punc==QUESTION || punc == QUEST) && !taskTerm.hasAny(Op.VAR_QUERY /* ineligible to be present in actual belief/goal */)) {
//
//                        BeliefTables aa = (BeliefTables) c.tableAnswering(punc);
//                        /*@Nullable DynamicTaskTable aa = answers.tableFirst(DynamicTaskTable.class);
//                        if (aa!=null)*/ {
//
//                            //match an answer emulating a virtual self-termlink being matched during premise formation
//                            Task q = task;
//                            Task a = aa.answer(q.start(), q.end(), taskTerm, null, n);
//                            if (a != null) {
//
//
//
//                                //decrease tasklink too?
//
//                                q.onAnswered(a, n);
//                                n.input(a);
//                            }
//                        }
//                    }
//            }
        }
//        } else {
//            //TODO if target supports dynamic truth, then possibly conceptualize and then match as above?
//
//            //form a question/quest task for the missing concept
//            byte punc;
//            switch (this.punc) {
//                case BELIEF:
//                    punc = QUESTION;
//                    break;
//                case GOAL:
//                    punc = QUEST;
//                    break;
//                case QUESTION:
//                case QUEST:
//                    punc = this.punc;
//                    break;
//                default:
//                    throw new UnsupportedOperationException();
//            }
//
//            task = new UnevaluatedTask(target, punc, null, n.time(), se[0], se[1], n.evidence());
//            if (Param.DEBUG)
//                task.log("Tasklinked");
//            task.pri(link.priElseZero());

//        }
        //return null;
    }

    void delete(byte punc);

    default /* final */ void merge(TaskLink incoming, PriMerge merge) {
        merge(incoming, merge, PriReturn.Void);
    }

    float merge(TaskLink incoming, PriMerge merge, PriReturn returning);

    float take(byte punc, float howMuch);

    default boolean isSelf() {
        return from().equals(to());
    }

    @Nullable
    default Term other(Term x, boolean reverse) {
        return x.equals(reverse ? to() : from()) ? (reverse ? from() : to()) : null;
    }
//    /**
//     * returns the delta
//     */
//    float priMax(byte punc, float p);

    /**
     * snapshots a 4-tuple: beliefs, goals, questions, quests
     */
    default float[] priGet() {
        return new float[]{priPunc(BELIEF), priPunc(GOAL), priPunc(QUESTION), priPunc(QUEST)};
    }

    /** which component is strongest */
    default byte puncMax() {
        switch (Util.maxIndex(priPunc(BELIEF), priPunc(GOAL), priPunc(QUESTION), priPunc(QUEST))) {
            case 0:
                return BELIEF;
            case 1:
                return GOAL;
            case 2:
                return QUESTION;
            case 3:
                return QUEST;
        }
        return -1;
    }


//    /** special tasklink for signals which can stretch and so their target time would not correspond well while changing */
//    class DirectTaskLink extends PLinkUntilDeleted<Task> implements TaskLink {
//
//        public DirectTaskLink(Task id, float p) {
//            super(id, p);
//        }
//
//        @Override
//        public Term term() {
//            return id.term();
//        }
//
//        @Override
//        public byte punc() {
//            return id.punc();
//        }
//
//        @Override
//        public Task apply(NAR n) {
//            return get();
//        }
//
        //@Override
        default TaskLink clone(float pri) {
//            Task t = get();
//            return t!=null ? new DirectTaskLink(t, pri) : null;
            throw new TODO();
        }

    @Nullable default Term reverseMatch(Term term) {
        if (!isSelf()) {
            if (to().equals(term)){
                return from();
            }
        }
        return null;
    }

    /** determines forward growth target. null to disable
     *  override to provide a custom termlink supplier */
    @Nullable default Term forward(Term target, TaskLink link, Task task, Derivation d) {
        if (target.op().conceptualizable) {
            TermLinker linker = d.deriver.linker(target); //TODO custom tasklink-provided termlink strategy
            if (linker != null) {
                Term forward = linker.sample(target, d.random);
                if (!forward.equals(target))
                    return forward;
            }

        }
        return null;
    }

    /** the termlink which is finally resolved by the tasklink.  defaults to to()
     *  but can be overridden for dynamic / virtual termlinks.
     */
    default Term target(Task task, Derivation d) {
        return to();
    }

//    }

    //    public static class CompactTaskLink extends AtomicQuad16Vector implements TaskLink {
//        /**
//         * source,target as a 2-ary subterm
//         */
//        final Term from, to;
//        private final int hash;
//
//        protected CompactTaskLink(Term self) {
//            this(self.concept(), null);
//        }
//
//        protected CompactTaskLink(Term source, Term target) {
//
//            source = source.concept();
//            target = target == null ? source : target.concept();
//
//            Op so = source.op();
//            if (!so.taskable)
//                throw new TaskException(source, "source term not taskable");
//            if (!so.conceptualizable)
//                throw new TaskException(source, "source term not conceptualizable");
//            if (Param.DEBUG) {
//                if (!source.isNormalized())
//                    throw new TaskException(source, "source term not normalized");
//            }
//
//            this.from = source;
//            this.to = target;
//            this.hash = Util.hashCombine(from, to);
//        }
//
//        @Override
//        final public TaskLink id() {
//            return this;
//        }
//
//        @Override
//        public final boolean equals(Object obj) {
//            if (this == obj) return true;
//            if (obj instanceof TaskLink) {
//                if (hashCode() == obj.hashCode()) if (from().equals(((TaskLink) obj).from()))
//                    if (to().equals(((TaskLink) obj).to())) return true;
//            }
//            return false;
//        }
//
//        @Override
//        public final int hashCode() {
//            return hash;
//        }
//
//        @Override
//        public final Term from() {
//            return from;
//        }
//
//        @Override
//        public final Term to() {
//            return to;
//        }
//
//        @Override
//        public float pri(float p) {
//            return 0;
//        }
//
//        @Override
//        public float merge(TaskLink incoming, PriMerge merge) {
//            return 0;
//        }
//
//        @Override
//        public float take(byte punc, float howMuch) {
//            return 0;
//        }
//
//        @Override
//        public float pri() {
//            //TODO add in integers convert to float after
//            return sumValues() / 4;
//        }
//
//        @Override
//        public float priPunc(byte punc) {
//            return getAt(i(punc));
//        }
//
//        @Override
//        public float getAndSetPriPunc(byte punc, float next) {
//            //TODO make atomic
//            int ii = i(punc);
//            float before = getAt(ii);
//            setAt(next, ii);
//            return before;
//        }
//
//        @Override
//        public void delete(byte punc) {
//            fill(0);
//        }
//
//
//    }


//    class DirectTaskLink extends PLinkUntilDeleted<Task> implements TaskLink {
//
//        public DirectTaskLink(Task id, float p) {
//            super(id, p);
//        }
//
//        @Override
//        public Term target() {
//            Task t = id;
//            if (t == null)
//                return null;
//            return t.target();
//        }
//
//        @Override
//        public Task get(NAR n) {
//
//            return id;
//        }
//
//
//    }

}
