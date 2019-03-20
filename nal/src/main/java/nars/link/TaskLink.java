package nars.link;

import jcog.TODO;
import jcog.Util;
import jcog.WTF;
import jcog.decide.Roulette;
import jcog.pri.ScalarValue;
import jcog.pri.UnitPri;
import jcog.pri.UnitPrioritizable;
import jcog.pri.Weight;
import jcog.pri.op.PriMerge;
import jcog.signal.tensor.AtomicArrayTensor;
import jcog.util.FloatFloatToFloatFunction;
import nars.*;
import nars.concept.Concept;
import nars.subterm.Subterms;
import nars.table.TaskTable;
import nars.task.NALTask;
import nars.task.util.TaskException;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.time.When;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.function.Predicate;

import static jcog.Util.assertFinite;
import static nars.Op.*;
import static nars.time.Tense.ETERNAL;

/**
 * the function of a tasklink is to be a prioritizable strategy for resolving a Task in a NAR.
 * this does not mean that it must reference a specific Task but certain properties of it
 * that can be used ot dynamically match a Task on demand.
 * <p>
 * note: seems to be important for Tasklink to NOT implement Termed when use with common Map's with Termlinks
 */
public interface TaskLink extends UnitPrioritizable {

    TaskLink[] EmptyTaskLinkArray = new TaskLink[0];


    /**
     * concept term (source) where the link originates
     */
    Term source();

    /**
     * task term (target) of the task linked
     */
    Term target();

    //byte punc();
    float priPunc(byte punc);

    default float pri(byte punc) {
        throw new WTF("use priPunc");
    }

    float getAndSetPriPunc(byte punc, float next);


    static TaskLink tasklink(Term src, Term tgt, byte punc, float pri) {
        return new GeneralTaskLink(src, tgt).priMerge(punc, pri);
    }


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
    byte priPunc(Random rng);

    default byte punc(Random rng) {
        return priPunc(rng);
    }

    @Nullable default /* final */Task get(When when) {
        return get(punc(when.nar.random()), when, null);
    }

    @Nullable default Task get(byte punc, When when) {
        return get(punc, when, null);
    }

    @Nullable default Task get(byte punc, When when, Predicate<Task> filter) {
        if (punc == 0)
            return null; //flat-lined tasklink

        Term x = source();

        NAR n = when.nar;

        boolean beliefOrGoal = punc == BELIEF || punc == GOAL;
        Concept c =
                //n.concept(t);
                beliefOrGoal ? n.conceptualizeDynamic(x) : n.concept(x);

        if (c == null)
            delete();
        else {

            TaskTable table = c.table(punc);
            Task y;
            if (beliefOrGoal) {
                y = table.match(when, null, filter);
            } else {
                y = table.sample(when, null, filter);
            }

            if (y == null) {
                if (!beliefOrGoal) {
                    //form question
                    float qpri = Param.TASKLINK_GENERATED_QUESTION_PRI_RATE;
                    if (qpri > Float.MIN_NORMAL) {
                        if (Task.validTaskTerm(x, punc, true)) {
                            y = NALTask.the(x, punc, null, when);
                            y.pri(priPunc(punc) * qpri);
                        }
                    }
                }

                if (y == null)
                    delete(punc); //TODO try another punc?
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



        return null;
    }

    void delete(byte punc);

    float merge(TaskLink incoming, PriMerge merge);

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

    float take(byte punc, float howMuch);

    default boolean isSelf() {
        return source().equals(target());
    }

    default Term other(Atomic x) {
        if (target().equals(x)) {
            return source();
        } else if (!Param.DEBUG || source().equals(x)) {
            return target();
        }
        throw new WTF();
    }

//    /**
//     * returns the delta
//     */
//    float priMax(byte punc, float p);


    /** snapshots a 4-tuple: beliefs, goals, questions, quests */
    default float[] priPuncArray() {
        return new float[] { priPunc(BELIEF), priPunc(GOAL), priPunc(QUESTION), priPunc(QUEST) };
    }

    @Nullable default Term other(Term x, boolean reverse) {
        if (reverse) {
            if (x.equals(target()))
                return source();
        } else {
            if (x.equals(source()))
                return target();
        }
        return null;
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
//        @Override
//        public TaskLink clone(float pri) {
//            Task t = get();
//            return t!=null ? new DirectTaskLink(t, pri) : null;
//        }
//    }

    abstract class AbstractTaskLink extends UnitPri implements TaskLink {
        /** source,target as a 2-ary subterm */
        protected final Subterms sourceTarget;

        protected AbstractTaskLink(Term self) {
            this(self.concept(), null);
        }

        protected AbstractTaskLink(Term source, Term target) {

            source = source.concept();
            target = target == null ? source : target.concept();

            Op so = source.op();
            if (!so.taskable)
                throw new TaskException(source, "source term not taskable");
            if (!so.conceptualizable)
                throw new TaskException(source, "source term not conceptualizable");
            //if (Param.DEBUG) {
            if (!source.isNormalized())
                throw new TaskException(source, "source term not normalized");
            //}

            this.sourceTarget = Op.terms.subterms(source, target);
        }

        @Override
        public final boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj instanceof GeneralTaskLink) {
                return sourceTarget.equals(((GeneralTaskLink)obj).sourceTarget);
            }
            else if (obj instanceof TaskLink) {
                if (hashCode() == obj.hashCode()) if (source().equals(((TaskLink) obj).source()))
                    if (target().equals(((TaskLink) obj).target())) return true;
            }
            return false;
        }

        @Override
        public final int hashCode() {
            return sourceTarget.hashCodeSubterms();
        }

        @Override
        public final Term source() {
            return sourceTarget.sub(0);
        }

        @Override
        public final Term target() {
            return sourceTarget.sub(1);
        }
    }

    /**
     * dynamically resolves a task.
     * serializable and doesnt maintain a direct reference to a task.
     * may delete itself if the target concept is not conceptualized.
     */
    class GeneralTaskLink extends AbstractTaskLink {

        final AtomicArrayTensor punc = new AtomicArrayTensor(4);

        public GeneralTaskLink(Term source, Term target) {
            super(source, target);
        }

        public GeneralTaskLink(Term self) {
            super(self);
        }

        public GeneralTaskLink(Term source, Term target, long when, byte punc, float pri) {
            this(source, target);
            if (when != ETERNAL) throw new TODO("non-eternal tasklink not supported yet");
            if (pri > 0)
                priMerge(punc, pri);
        }


        @Override
        public float take(byte punc, float howMuch) {
            return Math.max(ScalarValue.EPSILON, -priMergeGetDelta(punc, howMuch, PriMerge.minus));
        }

        private void assertAccurate() {
            if (Param.DEBUG) {
                if (!Util.equals(priElseZero(), punc.sumValues() / 4f, ScalarValue.EPSILON * 2))
                    throw new WTF();
            }
        }

        @Override
        public void delete(byte punc) {
            priSet(punc, 0);
        }

        @Override
        public boolean delete() {
            if (super.delete()) {
                punc.fill(0);
                return true;
            }
            return false;
        }

        @Override
        public byte priPunc(Random rng) {
            int i = Roulette.selectRouletteCached(4, punc::getAt, rng);
            if (i!=-1)
                return Task.p(i);
            else
                return 0;
        }

        public final GeneralTaskLink priMerge(byte punc, float pri) {
            mergeComponent(punc, pri, Param.tasklinkMerge, true);
            return this;
        }

        public final TaskLink priMerge(byte punc, float pri, PriMerge merge) {
            priMergeGetValue(punc, pri, merge);
            return this;
        }

        public final float priMergeGetValue(byte punc, float pri, PriMerge merge) {
            return mergeComponent(punc, pri, merge, true);
        }

        public final float priMergeGetDelta(byte punc, float pri, PriMerge merge) {
            return mergeComponent(punc, pri, merge, false);
        }

        /**
         * returns delta
         */
        public void priSet(byte punc, float pri) {
            priMergeGetDelta(punc, pri, PriMerge.replace);
        }



        @Override
        public /* HACK */ float merge(TaskLink incoming, PriMerge merge) {
            if (incoming instanceof GeneralTaskLink) {
                float delta = 0;
                for (int i = 0; i < 4; i++) {
                    float p = ((GeneralTaskLink) incoming).punc.getAt(i);
                    if (Math.abs(p) > Float.MIN_NORMAL) {
                        delta += priMergeGetDelta(Task.p(i), p, merge);
                    }
                }
                return delta / 4;
            } else {
                throw new TODO();
            }
        }

        private float mergeComponent(byte punc, float pri, PriMerge merge, boolean valueOrDelta) {
            assertFinite(pri);

            //assertAccurate();

            float y = this.punc.update(pri, mergeComponent(merge), (prev, next) -> {
                float delta = (next - prev) / 4;
                if (Math.abs(delta) > Float.MIN_NORMAL)
                    super.priAdd(delta);
            }, Task.i(punc), valueOrDelta);

            //assertAccurate();

            return y;
        }

        private static FloatFloatToFloatFunction mergeComponent(PriMerge merge) {
            if (merge == PriMerge.replace) {
                //optimized
                return (existing, incoming) -> incoming;
            }
            if (merge == PriMerge.max) {
                //optimized
                return (existing, incoming) -> {
                    if (existing != existing) return incoming;
                    float next = Math.max(existing, incoming);
                    if (next == next) {
                        if (next > 1) next = 1;
                        else if (next < 0) next = 0;
                        return next;
                    } else
                        return 0;
                };
            } else if (merge == PriMerge.plus) {
                //optimized
                return (existing, incoming) -> {
                    if (existing != existing) return incoming;
                    float next = (existing + incoming);
                    if (next == next) {
                        if (next > 1) next = 1;
                        else if (next < 0) next = 0;
                        return next;
                    } else
                        return 0;
                };
            } else {
                //generic
                return (existing, incoming) -> {
                    Weight tmp = new Weight(existing);
                    merge.merge(tmp, incoming);
                    float next = tmp.pri;
                    if (next == next) {
                        if (next > 1) next = 1;
                        else if (next < 0) next = 0;
                        return next;
                    } else
                        return 0;
                };
            }

        }

        @Override
        public float pri(float p) {
            throw new TODO();
        }

        @Override
        public void priAdd(float a) {
            throw new TODO();
        }

        @Override
        public float priMult(float a) {
            assertFinite(a);
            if (Util.equals(a, 1, Float.MIN_NORMAL))
                return pri();

            //assertAccurate();

            assert (a < 1);

            float y = priUpdateAndGet((p, aa) -> {
                if (p != p) {
                    punc.fill(0);
                    return p; //stay deleted
                } else {
                    float newSum = 0;
                    for (int i = 0; i < 4; i++)
                        newSum += punc.update(aa, (x, aaa) -> Util.unitize/*Safe*/(x * aaa), i);
                    return newSum / 4;
                }
            }, a);

            //assertAccurate();

            return y;
        }

        @Override
        public float pri(FloatFloatToFloatFunction update, float _x) {
            float y = super.pri((prev, x) -> {
                float next = update.apply(prev, x);
                if (next == next) {
                    if (prev != prev) {
                        punc.fill(next / 4);
                    } else {
                        float delta = (next - prev) / 4;
                        if (Math.abs(delta) > Float.MIN_NORMAL) {
                            float newSum = 0;
                            for (int i = 0; i < 4; i++)
                                newSum += punc.addAt(delta, i);
                            return newSum / 4;
                        }
                    }
                } else {
                    punc.fill(0);
                }
                return next;
            }, _x);

            assertAccurate();

            return y;
        }

        @Override
        public float priPunc(byte punc) {
            return this.punc.getAt(Task.i(punc));
        }

        @Override
        public float getAndSetPriPunc(byte punc, float next) {
            return mergeComponent(punc, next, PriMerge.replace, true);
        }

        @Override
        public String toString() {
            return toBudgetString() + ' ' + source() + (punc) + ':' + target();
        }


    }

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
