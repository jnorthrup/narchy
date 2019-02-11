package nars.link;

import jcog.TODO;
import jcog.Util;
import jcog.WTF;
import jcog.data.MutableFloat;
import jcog.decide.Roulette;
import jcog.pri.*;
import jcog.pri.bag.Bag;
import jcog.pri.op.PriMerge;
import jcog.signal.tensor.AtomicArrayTensor;
import jcog.util.FloatFloatToFloatFunction;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.Concept;
import nars.index.concept.AbstractConceptIndex;
import nars.term.Term;
import nars.term.atom.Atomic;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.function.Function;

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
public interface TaskLink extends UnitPrioritizable, Function<NAR, Task> {

    TaskLink[] EmptyTaskLinkArray = new TaskLink[0];

    /**
     * dont use .apply() directly; use this
     */
    static Task task(TaskLink x, NAR n) {
        Task y = x.apply(n);
        if (y == null)
            x.delete();
        return y;
    }


    /**
     * concept term (source) where the link originates
     */
    Term source();

    /**
     * task term (target) of the task linked
     */
    Term target();

    //byte punc();
    float punc(byte punc);

    /**
     * main tasklink constructor
     */
    static TaskLink tasklink(Term src, Task task, float pri) {

        //assert(task.target().volume() < n.termVolumeMax.intValue());


//        if (task instanceof SignalTask) {
//            return new DirectTaskLink(task, pri);
//        } else {

        return new GeneralTaskLink(src, task.term()).pri(task.punc(), pri);
        //}
    }


    static void link(TaskLink x, NAR nar) {
        link(x, nar, null);
    }

    static void link(TaskLink x, NAR nar, @Nullable OverflowDistributor<Bag> overflow) {

        Bag<TaskLink, TaskLink> b = ((AbstractConceptIndex) nar.concepts).active;

        if (overflow != null) {
            MutableFloat o = new MutableFloat();

            TaskLink yy = b.put(x, o);

            if (o.floatValue() > EPSILON) {
                overflow.overflow(b, o.floatValue(),
                        (yy != null) ?
                                1f - yy.priElseZero()
                                :
                                1 //assume it needs as much as it can get
                );
            }
        } else {
            b.putAsync(x);
        }
    }

    /**
     * sample punctuation by relative priority
     */
    byte punc(Random rng);

    default long when() {
        return ETERNAL;
    }

    default Task apply(NAR n) {

        Term t = target();

        //choose punc
        byte punc = punc(n.random());

        Concept c =
                //n.concept(t);
                punc == BELIEF || punc == GOAL ? n.conceptualizeDynamic(t) : n.concept(t);

        if (c != null) {

            long start, end;
            start = end = when();

            return c.table(punc).
                    sample
                    //match
                            (start, end, t, n);

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


        //TEMPORARY
//        if (task!=null && task.isInput() && !(task instanceof SignalTask)) {
//            link.priMax(task.priElseZero()); //boost
//        }

        return null;

    }

    float merge(TaskLink incoming, PriMerge merge);

    default byte puncMax() {
        switch (Util.maxIndex(punc(BELIEF), punc(GOAL), punc(QUESTION), punc(QUEST))) {
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

    default boolean isSelf() {
        return source().equals(target());
    }

    default Term other(Atomic x) {
        if (source().equals(x)) {
            return target();
        } else if (!Param.DEBUG || target().equals(x)) {
            return source();
        }
        throw new WTF();
    }

    /** returns the delta */
    float priMax(byte punc, float p);


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
        private final Term source;
        private final Term target;
        private final int hash;

        public AbstractTaskLink(Term source, Term target) {
            this.source = source.concept();
            this.target = target.concept();
            this.hash = Util.hashCombine(source, target);
        }

        @Override
        public final boolean equals(Object obj) {
            return this == obj || (
                    (hash == obj.hashCode())
                            && source.equals(((AbstractTaskLink) obj).source)
                            && target.equals(((AbstractTaskLink) obj).target)
            );
        }

        @Override
        public final int hashCode() {
            return hash;
        }

        @Override
        public final Term source() {
            return source;
        }

        @Override
        public final Term target() {
            return target;
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

        public GeneralTaskLink(Term source, Term target, long when, byte punc, float pri) {
            this(source, target);
            if (when != ETERNAL) throw new TODO("non-eternal tasklink not supported yet");
            if (pri > 0)
                pri(punc, pri);
        }

        private void assertAccurate() {
            if (Param.DEBUG) {
                if (!Util.equals(priElseZero(), punc.sumValues() / 4f, ScalarValue.EPSILON * 2))
                    throw new WTF();
            }
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
        public byte punc(Random rng) {
            return p(Roulette.selectRouletteCached(4, punc::getAt, rng));
        }

        public final GeneralTaskLink pri(byte punc, float pri) {
            return mergeComponent(punc, pri, Param.tasklinkMerge);
        }

        @Override
        public float priMax(byte punc, float max) {
            return mergeComponent(punc, max, PriMerge.max, false);
        }

        @Override
        public /* HACK */ float merge(TaskLink incoming, PriMerge merge) {
            if (incoming instanceof GeneralTaskLink) {
                float delta = 0;
                for (int i = 0; i < 4; i++) {
                    float p = ((GeneralTaskLink) incoming).punc.getAt(i);
                    if (Math.abs(p) > Float.MIN_NORMAL) {
                        delta += mergeComponent(p(i), p, merge, false);
                    }
                }
                return delta/4;
            } else {
                throw new TODO();
            }
        }

        public GeneralTaskLink mergeComponent(byte punc, float pri, PriMerge merge) {

            mergeComponent(punc, pri, merge, true);

            return this;
        }

        public float mergeComponent(byte punc, float pri, PriMerge merge, boolean valueOrDelta) {
            assertFinite(pri);

            //assertAccurate();

            float y = this.punc.update(pri, mergeComponent(merge), (prev, next) -> {
                float delta = (next - prev) / 4;
                if (Math.abs(delta) > Float.MIN_NORMAL)
                    super.priAdd(delta);
            }, i(punc), valueOrDelta);

            //assertAccurate();

            return y;
        }

        private static FloatFloatToFloatFunction mergeComponent(PriMerge merge) {
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
        public float punc(byte punc) {
            return this.punc.getAt(i(punc));
        }

        private static int i(byte p) {
            switch (p) {
                case BELIEF:
                    return 0;
                case QUESTION:
                    return 1;
                case GOAL:
                    return 2;
                case QUEST:
                    return 3;
                default:
                    return -1;
            }
        }

        private static byte p(int index) {
            switch (index) {
                case 0:
                    return BELIEF;
                case 1:
                    return QUESTION;
                case 2:
                    return GOAL;
                case 3:
                    return QUEST;
                default:
                    return -1;
            }
        }

        @Override
        public String toString() {
            return toBudgetString() + ' ' + target() + (punc) + ':' + source();
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
