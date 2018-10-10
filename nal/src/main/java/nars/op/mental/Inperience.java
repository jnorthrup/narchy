package nars.op.mental;

import jcog.Util;
import jcog.data.list.FasterList;
import jcog.math.FloatRange;
import nars.*;
import nars.bag.leak.TaskLeakTransform;
import nars.concept.Concept;
import nars.task.signal.SignalTask;
import nars.term.Compound;
import nars.term.Functor;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.atom.Int;
import nars.term.util.Conj;
import nars.term.util.Image;
import nars.term.util.transform.Retemporalize;
import nars.term.util.transform.TermTransform;
import nars.time.Tense;
import nars.truth.PreciseTruth;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.factory.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.BooleanSupplier;

import static nars.Op.*;
import static nars.time.Tense.*;

/**
 * Internal Experience (NAL9)
 * To remember activity as internal action operations
 * <p>
 * https:
 * "Imperience": http:
 */
abstract public class Inperience extends TaskLeakTransform {
    /**
     * multiplier for he sensory task priority to determine inperienced task priority
     * should be < 1.0 to avoid feedback overload
     */
    public final FloatRange priFactor = new FloatRange(0.5f, 0, 2);

    private transient int volMaxPost, volMaxPre;

    public static final Logger logger = LoggerFactory.getLogger(Inperience.class);

    private static final Atomic believe = Atomic.the("believe");
    private static final Atomic want = Atomic.the("want");


    public static final Atomic wonder = Atomic.the("wonder");
    private static final Atomic evaluate = Atomic.the("plan");
    private static final Atomic reflect = Atomic.the("reflect");
    private static final ImmutableSet<Atomic> operators = Sets.immutable.of(
            believe, want, wonder, evaluate, reflect);



    /** semanticize, as much as possible, a term so it can enter higher order
     * TODO merge with NLPGen stuff
     * */
    final static TermTransform Described = new TermTransform.NegObliviousTermTransform() {

        private final Atomic and = Atomic.the("and");
        //private final Atomic so = Atomic.the("so"); //so, then, thus
        private final Atomic seq = Atomic.the("seq"); //so, then, thus
        private final Atomic dt = Atomic.the("dt");
        private final Atomic If = Atomic.the("if");
        private final Atomic inherits = Atomic.the("is");
        private final Atomic similar = Atomic.the("alike"); //similarity

        @Override
        protected Term transformNonNegCompound(Compound term) {
            int dt = term.dt();
            switch (term.op()) {

                case NEG: {
                    throw new UnsupportedOperationException("shouldnt be called due to NegObliviosity");
                }

                case INH: {
                    if (Functor.isFunc(term)) {
                        //preserve functor form
                        return INH.the(PROD.the(term.sub(0).subterms().transformSubs(this, PROD)), term.sub(1));
                    } else {
                        return $.func(inherits, transform(term.sub(0)), transform(term.sub(1)));
                    }
                }

                case SIM:
                    return $.func(similar, SETe.the(transform(term.sub(0)), transform(term.sub(1)) ));

                case IMPL:
                    //TODO insert 'dur' for dt()'s
                    if (dt == DTERNAL || dt == XTERNAL) {
                        return $.func(If, transform(term.sub(0)), transform(term.sub(1)));
                    } else {
                        return $.func(If, transform(term.sub(0)), transform(term.sub(1)), $.func(this.dt, Int.the(dt)));
                    }

                case CONJ:

                    if (Conj.concurrent(dt)) {
                        return $.func(and, SETe.the(term.subterms().transformSubs(this, SETe)));
                    } else {
                        List<Term> ss = new FasterList(3);
                        final long[] last = {0};
                        term.eventsWhile((when,what)->{
                            if (!ss.isEmpty()) {
                                long interval = when - last[0];
                                if (interval > 0)
                                    ss.add($.func(this.dt, $.the(interval)));
                            }
                            ss.add(transform(what));
                            last[0] = when;
                            return true;
                        },0, false, false, false,0);

                        return $.func(seq, ss);

                    }
            }
            return term;
        }
    };




    public static class Believe extends Inperience {

        public Believe(NAR n, int capacity) {
            this(n, BELIEF, capacity);
        }

        Believe(NAR n, byte punc, int capacity) {
            super(n, punc, capacity);
        }

        /**
         * max frequency difference from either 0.0 or 1.0 to be polarized enough.
         * use the < 0.5 value here, ex: 0.1 means that 0..0.1 and 0.9..1.0 will be accepted
         */
        public final FloatRange freqMax = new FloatRange(0.1f, 0f, 1f);



        /** whether a belief or goal is sufficiently polarized */
        boolean polarized(float f) {
            float fm = freqMax.floatValue();
            return f <= fm || f >= (1f - fm);
        }


        @Override
        public boolean acceptTask(Task t) {
            return polarized(t.freq());
        }

        @Override
        protected Term reify(Task t) {
            Term tt = t.term().eval(nar);

            if (!tt.op().conceptualizable)
                return Bool.Null;

            return reifyBeliefOrGoal(t, tt.negIf(t.isNegative()), nar);
        }

        @Deprecated protected Term reifyBeliefOrGoal(Task t, Term tt, NAR nar) {
            Concept c = nar.conceptualizeDynamic(tt.unneg());
            if (c == null)
                return Bool.Null;

            Term self = nar.self();

            if (t.punc() == BELIEF) {
                Task belief = c.table(BELIEF)
                        .answer(t.start(), t.end(), tt.unneg(), null, nar);

                Term bb = belief != null ? Described.transform(belief.term().negIf(belief.isNegative())) : Described.transform(tt);
                return $.funcImageLast(believe, self, bb);
            } else {
                Task goal = c.table(GOAL)
                        .answer(t.start(), t.end(), tt.unneg(), null, nar);

                Term gg = goal!=null ? Described.transform(goal.term().negIf(goal.isNegative())) : Described.transform(tt);
                return $.funcImageLast(want, self, gg);

//                else {
//
//                    return CONJ.the(want, 0, $.func(believe, self, bb.negIf(belief.isNegative())));
//                }
            }
        }



    }
    public static class Want extends Believe {

        public Want(NAR n, int capacity) {
            super(n, GOAL, capacity);
        }

    }

    public static class Wonder extends Inperience {

        public Wonder(NAR n, int capacity) {
            this(n, QUESTION, capacity);
        }

        protected Wonder(NAR n, byte punc, int capacity) {
            super(n, punc, capacity);
        }

        @Override
        public boolean acceptTask(Task t) {
            return true;
        }

        private Term reifyQuestion(Term x, byte punc, NAR nar) {
            x = x.temporalize(Retemporalize.retemporalizeXTERNALToDTERNAL);
            x = x.hasAny(VAR_QUERY) ? TermTransform.queryToDepVar.transform(x) : x;
            if (x instanceof Bool) return Bool.Null;

            return $.funcImageLast(punc == QUESTION ? wonder : evaluate, nar.self(), Described.transform(x));
        }

        @Override
        protected Term reify(Task t) {
            return reifyQuestion(
                    //t.term().eval(nar),
                    t.term(),
                    t.punc(), nar);
        }
    }

    public static class Plan extends Wonder {

        public Plan(NAR n, int capacity) {
            super(n, QUEST, capacity);
        }

    }

    protected Inperience(NAR n, byte punc, int capacity) {
        super(capacity, n, punc);
    }


    /** prefilter on task punc */
    abstract public boolean acceptTask(Task t);

    /**
     * compute current value of the truth for the task's time;
     * dont just regurgitate what the task says. the truth may differ
     * at some point after the task was created so we get a more
     * updated result.
     */
    abstract protected Term reify(Task t);

    /** can be overridden but default is belief */
    protected byte puncResult() { return BELIEF; }


    /** minimum expected reification overhead for filtering candidates by volume limit */
    final static int MIN_REIFICATION_OVERHEAD = 2 + 1 /* 1 extra to be safe */;

    @Override
    protected void next(NAR nar, BooleanSupplier kontinue) {
        volMaxPre = (volMaxPost = nar.termVolumeMax.intValue()) - MIN_REIFICATION_OVERHEAD;
        super.next(nar, kontinue);
    }

    @Override
    public boolean filter(Task next) {

        if (next.stamp().length == 0)
            return false;

        if (//next.isInput() ||
            !acceptTask(next)
            /*|| task instanceof InperienceTask*/)
            return false;


        Term nextTerm = next.term();
        if (nextTerm.volume() > volMaxPre)
            return false;

        nextTerm = Image.imageNormalize(nextTerm);
        if (nextTerm.op() == INH) {
            //prevent immediate cyclic feedback
            Term pred = nextTerm.sub(1);
            return pred.op() != ATOM || !operators.contains(pred);
        }

        return true;
    }


    @Override
    protected float leak(Task x) {

        Term c = reify(x).normalize();
        if (!c.op().conceptualizable)
            return 0;
        if (c.volume() > volMaxPost)
            return 0; //TODO try to prevent

        float polarity = x.isQuestionOrQuest() ? 0.5f : x.polarity();
        PreciseTruth t = $.t(1, Util.lerp(polarity, nar.confMin.floatValue()*2, nar.confDefault(Op.BELIEF))).dithered(nar);
        if (t == null)
            return 0;

        byte punc = puncResult();

        SignalTask y = Task.tryTask(c, punc, t, (tt, tr)->{
            long start = x.start();
            long end;
            if (start == ETERNAL) {
                //start = end = ETERNAL;
                start = nar.time();
                end = start + nar.dur();
            } else {
                start = Tense.dither(start, nar);
                end = Tense.dither(x.end(), nar);
            }
            return new SignalTask(tt, punc,
                    tr,
                    nar.time(), start, end, x.stamp()
            );
        });
        if (y!=null) {
            y.priCauseMerge(x);

            if (Param.DEBUG)
                y.log("Inperience");

            y.priSet(x.priElseZero() * priFactor.floatValue());

            //System.out.println(y);

            input(y);
            return 1;
        }

        return 0;
    }



}
