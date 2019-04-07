package nars.op.mental;

import jcog.data.list.FasterList;
import jcog.math.FloatRange;
import nars.$;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.attention.What;
import nars.bag.leak.TaskLeakTransform;
import nars.task.signal.SignalTask;
import nars.term.Compound;
import nars.term.Functor;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.atom.Int;
import nars.term.util.Image;
import nars.term.util.conj.Conj;
import nars.term.util.transform.AbstractTermTransform;
import nars.term.util.transform.Retemporalize;
import nars.term.util.transform.TermTransform;
import nars.term.util.transform.VariableTransform;
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

    public static final Atomic believe = Atomic.the("believe");
    public static final Atomic want = Atomic.the("want");


    public static final Atomic wonder = Atomic.the("wonder");
    public static final Atomic evaluate = Atomic.the("plan");
    private static final Atomic reflect = Atomic.the("reflect");
    private static final ImmutableSet<Atomic> operators = Sets.immutable.of(
            believe, want, wonder, evaluate, reflect);



    /** semanticize, as much as possible, a target so it can enter higher order
     * TODO merge with NLPGen stuff
     * */
    final static TermTransform Described = new AbstractTermTransform.NegObliviousTermTransform() {

        private final Atomic and = Atomic.the("and");
        //private final Atomic so = Atomic.the("so"); //so, then, thus
        private final Atomic seq = Atomic.the("seq"); //so, then, thus
        private final Atomic dt = Atomic.the("dt");
        private final Atomic If = Atomic.the("if");
        private final Atomic inherits = Atomic.the("is");
        private final Atomic similar = Atomic.the("alike"); //similarity

        @Override
        protected Term applyPosCompound(Compound term) {
            int dt = term.dt();
            switch (term.op()) {

                case BOOL:
                case NEG: {
                    throw new UnsupportedOperationException();
                }

                case INH: {
                    if (Functor.isFunc(term)) {
                        //preserve functor form
                        return INH.the(PROD.the(term.sub(0).subterms().transformSubs(this, PROD)), term.sub(1));
                    } else {
                        return $.func(inherits, apply(term.sub(0)), apply(term.sub(1)));
                    }
                }

                case SIM:
                    return $.func(similar, SETe.the(apply(term.sub(0)), apply(term.sub(1)) ));

                case IMPL:
                    //TODO insert 'dur' for dt()'s
                    if (dt == DTERNAL || dt == XTERNAL) {
                        return $.func(If, apply(term.sub(0)), apply(term.sub(1)));
                    } else {
                        return $.func(If, apply(term.sub(0)), apply(term.sub(1)),
                                $.func(this.dt, /*Tense.dither(*/Int.the(dt)));
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
                                    ss.add($.func(this.dt, /*Tense.dither(*/$.the(interval)));
                            }
                            ss.add(apply(what));
                            last[0] = when;
                            return true;
                        },0, false, false, false);

                        return $.func(seq, ss);

                    }
            }
            return term;
        }
    };

    public static Term reifyQuestion(Term x, byte punc, NAR nar) {
        x = x.temporalize(Retemporalize.retemporalizeXTERNALToDTERNAL);
        x = x.hasAny(VAR_QUERY) ? VariableTransform.queryToDepVar.apply(x) : x;
        if (x instanceof Bool) return Bool.Null;

        x = Image.imageNormalize(x);

        return $.funcImg(punc == QUESTION ? wonder : evaluate, nar.self(), Described.apply(x));
    }

    public static Term reifyBeliefOrGoal(Task t, NAR nar) {
        Term x = Image.imageNormalize(t.term());
//        Concept c = nar.conceptualizeDynamic(tt.unneg());
//        if (c == null)
//            return Bool.Null;

        Term self = nar.self();

        Atomic verb;
        Term y;
        if (t.punc() == BELIEF) {
//                Task belief =
//                        t;
//                        //c.table(BELIEF).answer(t.start(), t.end(), tt, null, nar);

            y = //belief != null ? Described.transform(belief.target().negIf(belief.isNegative())) :
                    Described.apply(x);
            verb = believe;
        } else {
//                Task goal = t;
//                        //c.table(GOAL).answer(t.start(), t.end(), tt.unneg(), null, nar);

            y = //goal!=null ? Described.transform(goal.target().negIf(goal.isNegative())) :
                    Described.apply(x);

            verb = want;
//                else {
//
//                    return CONJ.the(want, 0, $.func(believe, self, bb.negIf(belief.isNegative())));
//                }
        }
        return $.funcImg(verb, self, y.negIf(t.isNegative()));
    }


    public static class Believe extends Inperience {

        public Believe(int capacity, NAR n) {
            this(capacity, n, BELIEF);
        }

        Believe(int capacity, NAR n, byte punc) {
            super(capacity, punc, n);
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
            return !(t instanceof SignalTask) && polarized(t.freq());
        }

        @Override
        protected Term reify(Task t) {
            Term tt = t.term().eval(nar);

            if (!tt.op().taskable)
                return Bool.Null;

            return reifyBeliefOrGoal(t,  nar);
        }


    }
    public static class Want extends Believe {

        public Want(int capacity, NAR n) {
            super(capacity, n, GOAL);
        }

    }

    public static class Wonder extends Inperience {

        public Wonder(int capacity, NAR n) {
            this(capacity, n, QUESTION);
        }

        protected Wonder(int capacity, NAR n, byte punc) {
            super(capacity, punc, n);
        }

        @Override
        public boolean acceptTask(Task t) {
            return true;
        }

        @Override
        protected Term reify(Task t) {
            return reifyQuestion(
                    //t.target().eval(nar),
                    t.term(),
                    t.punc(), nar);
        }
    }

    public static class Plan extends Wonder {

        public Plan(int capacity, NAR n) {
            super(capacity, n, QUEST);
        }

    }

    protected Inperience(int capacity, byte punc, NAR n) {
        super(capacity, n, punc);
    }
//    protected Inperience(byte punc, NAR n) {
//        super(n, punc);
//    }


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
    public void next(What w, BooleanSupplier kontinue) {
        volMaxPre = (volMaxPost = w.nar.termVolumeMax.intValue()) - MIN_REIFICATION_OVERHEAD;
        super.next(w, kontinue);
    }

    @Override
    protected boolean filter(Term nextTerm) {
        return nextTerm.volume() <= volMaxPre;
    }

    @Override
    public boolean filter(Task next) {

        if (next.stamp().length == 0)
            return false;

        if (!acceptTask(next))
            return false;



        Term nextTerm = Image.imageNormalize(next.term());
        if (nextTerm.op() == INH) {
            //prevent immediate cyclic feedback
            Term pred = nextTerm.sub(1);
            return pred.op() != ATOM || !operators.contains(pred);
        }

        return true;
    }


    @Override
    protected float leak(Task x, What what) {

        Term c;
        try {
            c = reify(x).normalize();
        } catch (Throwable t) {
            if (Param.DEBUG)
                logger.error("{} failed Task reification: {} : {}", this, x, t);
            return 0;
        }

        if (!c.op().conceptualizable)
            return 0;
        if (c.volume() > volMaxPost)
            return 0; //TODO try to prevent

        int freq = x.isQuestionOrQuest() || x.isPositive() ? 1 : 0;
        PreciseTruth t = $.t(
            freq, nar.confDefault(BELIEF)
            //freq, Util.lerp(x.isQuestionOrQuest() ? 0.5f : x.polarity(), nar.confMin.floatValue()*2, nar.confDefault(Op.BELIEF))
        ).dither(nar);
        if (t == null)
            return 0;

        byte punc = puncResult();

        SignalTask y = Task.tryTask(c, punc, t, (tt, tr)->{
            long start = x.start();
            long end;
            long now = nar.time();
            if (start == ETERNAL) {
                //start = end = ETERNAL;
                int dur = nar.dur();
                start = now - dur/2;
                end   = now + dur/2;
            } else {
                end = x.end();
            }
            int dt = nar.dtDither();
            start = Tense.dither(start, dt);
            end = Tense.dither(end, dt);
            return new SignalTask(tt, punc,
                    tr,
                    now, start, end, x.stamp()
            );
        });
        if (y!=null) {

            Task.merge(y, x);

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
