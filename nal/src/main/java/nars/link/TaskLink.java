package nars.link;

import jcog.TODO;
import jcog.Util;
import jcog.WTF;
import jcog.data.list.FasterList;
import jcog.decide.Roulette;
import jcog.pri.ScalarValue;
import jcog.pri.UnitPri;
import jcog.pri.UnitPrioritizable;
import jcog.pri.Weight;
import jcog.pri.bag.Bag;
import jcog.pri.op.PriMerge;
import jcog.signal.tensor.AtomicArrayTensor;
import jcog.util.FloatFloatToFloatFunction;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.concept.Concept;
import nars.concept.NodeConcept;
import nars.derive.Derivation;
import nars.index.concept.AbstractConceptIndex;
import nars.task.NALTask;
import nars.task.util.TaskException;
import nars.term.Compound;
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
     * concept term (source) where the link originates
     */
    Term source();

    /**
     * task term (target) of the task linked
     */
    Term target();

    //byte punc();
    float priPunc(byte punc);


    static TaskLink tasklink(Term src, Term tgt, byte punc, float pri) {
        return new GeneralTaskLink(src, tgt).priMerge(punc, pri);
    }


    static void link(TaskLink x, NAR nar) {
        Bag<TaskLink, TaskLink> b = ((AbstractConceptIndex) nar.concepts).active;
        b.putAsync(x);
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
     */
    byte priPunc(Random rng);

    default long when() {
        return ETERNAL;
    }

    default Task apply(NAR n) {
        return apply(n, priPunc(n.random()));
    }

    @Nullable
    default Task apply(NAR n, byte punc) {
        Term x = source();

        boolean beliefOrGoal = punc == BELIEF || punc == GOAL;
        Concept c =
                //n.concept(t);
                beliefOrGoal ? n.conceptualizeDynamic(x) : n.concept(x);

        if (c == null)
            delete();
        else {

            long start, end;
            start = end = when();

            Task y = c.table(punc).
                    sample
                    //match
                            (start, end, x, n);

            if (y == null) {
                if (!beliefOrGoal) {
                    //form question
                    y = new NALTask(x, punc, null, n.time(), start, end, new long[]{n.time.nextStamp()});
                    y.pri(priPunc(punc));
                } else {
                    delete(punc); //TODO try another punc?
                }
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


        //TEMPORARY
//        if (task!=null && task.isInput() && !(task instanceof SignalTask)) {
//            link.priMax(task.priElseZero()); //boost
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

    /**
     * returns the delta
     */
    float priMax(byte punc, float p);

    @Nullable
    default Term term(Task task, Derivation d) {

        Term t = target();


        NAR nar = d.nar;
        Random rng = d.random;

        if (t instanceof Compound) {

            Concept sc = nar.conceptualize(t);
            if (sc != null) {
                TermLinker linker = sc.linker();
                if ((linker instanceof FasterList)
                        //rng.nextInt(t.volume() + 1) != 0

                //&& rng.nextInt(((FasterList) linker).size() + 1) != 0
                    //rng.nextFloat() < 0.25f
                    //rng.nextInt(((FasterList) linker).size() + 1) == 0
                    //src.equals(tgt) || rng.nextBoolean()
                    //rng.nextBoolean()
                ) {

                    @Nullable Term subSrc = linker.sample(rng);
                    t = subSrc;
                    if (subSrc.op().conceptualizable) {
                        @Nullable Concept subSrcConcept;
                        if ((subSrcConcept = nar.conceptualize(subSrc)) != null) {
                            t = subSrcConcept.term();
                            TaskLink.link(
                                    TaskLink.tasklink(source(), t, task.punc(),
                                            //priPunc(task.punc())
                                            task.priElseZero()
                                    ),
                                    nar);
                        }
                    }
                }


            }
        } else if (t.op().conceptualizable) {
            //scan active tasklinks for a tangent match to the atom
            @Nullable Concept cc = nar.concept(t);
            if (cc != null)
                t = ((AbstractConceptIndex) nar.concepts).active.atomTangent((NodeConcept) cc, this, d.time, d.ditherDT, d.random);
        }

        return t;

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
        private final Term source;
        private final Term target;
        private final int hash;

        protected AbstractTaskLink(Term self) {
            this(self, self);
        }

        protected AbstractTaskLink(Term source, Term target) {
            if (!source.op().taskable)
                throw new TaskException(source, "source term not taskable");
            this.source = source;
            this.target = target;
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
            super(source.concept(), target.concept());
        }

        public GeneralTaskLink(Term self) {
            super(self.concept());
        }

        public GeneralTaskLink(Term source, Term target, long when, byte punc, float pri) {
            this(source, target);
            if (when != ETERNAL) throw new TODO("non-eternal tasklink not supported yet");
            if (pri > 0)
                priMerge(punc, pri);
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
            return Task.p(Roulette.selectRouletteCached(4, punc::getAt, rng));
        }

        public final GeneralTaskLink priMerge(byte punc, float pri) {
            return mergeComponent(punc, pri, Param.tasklinkMerge);
        }

        /**
         * returns delta
         */
        public float priSet(byte punc, float pri) {
            return mergeComponent(punc, pri, PriMerge.replace, false);
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
                        delta += mergeComponent(Task.p(i), p, merge, false);
                    }
                }
                return delta / 4;
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
            }, Task.i(punc), valueOrDelta);

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
        public float priPunc(byte punc) {
            return this.punc.getAt(Task.i(punc));
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
