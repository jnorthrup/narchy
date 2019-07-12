package nars.op.mental;

import jcog.bloom.StableBloomFilter;
import jcog.math.FloatRange;
import nars.$;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.attention.TaskLinkWhat;
import nars.attention.What;
import nars.control.How;
import nars.control.channel.CauseChannel;
import nars.control.op.Remember;
import nars.link.TaskLink;
import nars.table.dynamic.SeriesBeliefTable;
import nars.task.proxy.SpecialPuncTermAndTruthTask;
import nars.term.Term;
import nars.term.Terms;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.util.Image;
import nars.term.util.transform.Retemporalize;
import nars.term.util.transform.VariableTransform;
import nars.time.When;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

import static nars.Op.*;

/**
 *
 *
 * Internal Experience (NAL9)
 * To remember activity as internal action operations
 * <p>
 * https:
 * "Imperience": http:
 *
 * snapshots of belief table aggregates, rather than individual tasks
 */
public class Inperience extends How {

    public static final Atomic believe = Atomic.the("believe");
    public static final Atomic want = Atomic.the("want");
    public static final Atomic wonder = Atomic.the("wonder");
    public static final Atomic evaluate = Atomic.the("plan");
    public final FloatRange priFactor = new FloatRange(0.1f, 0, 2f);

    private final CauseChannel<Task> in;

    public Inperience(NAR n) {
        super();
        this.in = n.newChannel(id);
        n.start(this);
    }

    public static Term reifyQuestion(Term x, byte punc, NAR nar) {
        return $.funcImg(punc == QUESTION ? wonder : evaluate, nar.self(), describe(x));
    }

    static Term reifyBeliefOrGoal(Task t, NAR nar) {

        Term self = nar.self();

        Term y = describe(t.term());

        Atomic verb;

        if (t.punc() == BELIEF) {
            verb = believe;
        } else {
            verb = want;
        }
        //return $.funcImg(verb, self, y.negIf(t.isNegative()));
        return $.func(verb, self, y.negIf(t.isNegative()));
    }

    private static Term describe(Term x) {

        x = x.temporalize(Retemporalize.retemporalizeXTERNALToDTERNAL);
        x = x.hasAny(VAR_QUERY) ? VariableTransform.queryToDepVar.apply(x) : x;
        if (x instanceof Bool) return Bool.Null;
        Term y = Image.imageNormalize(x);

        //return Described.apply(y);
        return y;
    }

    @Override
    public void next(What w, BooleanSupplier kontinue) {
        NAR n = w.nar;
        long now = w.time();
        float dur = w.dur();
        double window = 1.0;

        //int dither = n.dtDither();
        long start = /*Tense.dither*/((long)Math.floor(now - window * dur/2));//, dither);
        long end = /*Tense.dither*/((long)Math.ceil(now + window * dur/2));//, dither);
        When when = new When(start, end, dur, nar);

        int volMax = n.termVolMax.intValue();
        int volMaxPre = (int) Math.max(1, Math.ceil(volMax * 0.5f));
        float beliefConf = n.confDefault(BELIEF);
        Random rng = w.random();

        Predicate<Task> taskFilter = t -> accept(volMaxPre, t);

        StableBloomFilter<Task> filter = Terms.newTaskBloomFilter(rng, ((TaskLinkWhat) w).links.links.size());

        w.sampleUnique(rng, (TaskLink tl) -> {

            Task t = tl.get(when, taskFilter);
            if (t != null && filter.addIfMissing(t)) {

                Task u = null;
                if (t.isBeliefOrGoal()) {
                    Term r = reifyBeliefOrGoal(t, n);

                    if ((r = validReification(r, volMax))!=null) {
                        u = new InperienceTask(r, $.t(1,
                                beliefConf * (t.isNegative()?
                                        t.truth().expectationNeg() : t.truth().expectation())), t);
                    }
                } else {
                    Term r = reifyQuestion(t.term(), t.punc(), n);
                    if ((r = validReification(r, volMax))!=null)
                        u = new InperienceTask(r, $.t(1, beliefConf * 0.5f), t);
                }

                if (u != null) {
                    u.pri(0); //HACK
                    Task.fund(u, t, priFactor.floatValue(), true);
                    w.accept(u);
                }
            }

            return kontinue.getAsBoolean();
        });

    }

    private boolean accept(int volMax, Task t) {
        if (t instanceof SeriesBeliefTable.SeriesTask)
            return false;

        Term tt = t.term();
        if (tt.volume() > volMax)
            return false;

//        if (tt.hasAny(Op.CONJ))
//            return false; //HACK temporary

        return true;
    }

    @Nullable private Term validReification(Term r, int volMax) {
        if (r.op().taskable && r.volume() <= volMax) {
             r = r.normalize();
             if (Task.validTaskTerm(r, BELIEF, true))
                return r;
        }
        return null;
    }

    @Override
    public float value() {
        return in.value();
    }

    private static class InperienceTask extends SpecialPuncTermAndTruthTask {
        InperienceTask(Term r, @Nullable Truth tr, Task t) {
            super(r, Op.BELIEF, tr, t);
        }

        @Override
        public boolean isInput() {
            return false;
        }

        @Override
        public Task next(Object n) {
            return Remember.the(this, (NAR)n); //copied from UnevaluatedTask
        }
    }
}
//abstract public class Inperience extends TaskLeakTransform {
//    /**
//     * multiplier for he sensory task priority to determine inperienced task priority
//     * should be < 1.0 to avoid feedback overload
//     */
//    public final FloatRange priFactor = new FloatRange(0.5f, 0, 2);
//
//    private transient int volMaxPost, volMaxPre;
//
//    public static final Logger logger = LoggerFactory.getLogger(Inperience.class);
//
//
//    private static final ImmutableSet<Atomic> operators = Sets.immutable.of(
//            Inperience2.believe, Inperience2.want, Inperience2.wonder, Inperience2.evaluate, Inperience2.reflect);
//
//
//    /** semanticize, as much as possible, a target so it can enter higher order
//     * TODO merge with NLPGen stuff
//     * */
//    @Deprecated final static TermTransform Described = new AbstractTermTransform.NegObliviousTermTransform() {
//
//        private final Atomic and = Atomic.the("and");
//        //private final Atomic so = Atomic.the("so"); //so, then, thus
//        private final Atomic seq = Atomic.the("seq"); //so, then, thus
//        private final Atomic dt = Atomic.the("dt");
//        private final Atomic If = Atomic.the("if");
//        private final Atomic inherits = Atomic.the("is");
//        private final Atomic similar = Atomic.the("sim"); //similarity
//
//        @Override
//        protected Term applyPosCompound(Compound term) {
//            int dt = term.dt();
//            switch (term.op()) {
//
//                case BOOL:
//                case NEG: {
//                    throw new UnsupportedOperationException();
//                }
//
//                case INH: {
//                    if (Functor.isFunc(term)) {
//                        //preserve functor form
//                        return INH.the(PROD.the(term.sub(0).subterms().transformSubs(this, PROD)), term.sub(1));
//                    } else {
//                        return $.func(inherits, apply(term.sub(0)), apply(term.sub(1)));
//                    }
//                }
//
//                case SIM:
//                    return $.func(similar, SETe.the(apply(term.sub(0)), apply(term.sub(1)) ));
//
//                case IMPL:
//                    //TODO insert 'dur' for dt()'s
//                    if (dt == DTERNAL || dt == XTERNAL) {
//                        return $.func(If, apply(term.sub(0)), apply(term.sub(1)));
//                    } else {
//                        return $.func(If, apply(term.sub(0)), apply(term.sub(1)),
//                                $.func(this.dt, /*Tense.dither(*/Int.the(dt)));
//                    }
//
////                case CONJ:
////
////                    if (Conj.concurrent(dt)) {
////                        return $.func(and, SETe.the(term.subterms().transformSubs(this, SETe)));
////                    } else {
////                        List<Term> ss = new FasterList(3);
////                        final long[] last = {0};
////                        term.eventsWhile((when,what)->{
////                            if (!ss.isEmpty()) {
////                                long interval = when - last[0];
////                                if (interval > 0)
////                                    ss.add($.func(this.dt, /*Tense.dither(*/$.the(interval)));
////                            }
////                            ss.add(apply(what));
////                            last[0] = when;
////                            return true;
////                        },0, false, false, false);
////
////                        return $.func(seq, ss);
////
////                    }
//            }
//            return term;
//        }
//    };
//
//
//    public static class Believe extends Inperience {
//
//        public Believe(int capacity, NAR n) {
//            this(capacity, n, BELIEF);
//        }
//
//        Believe(int capacity, NAR n, byte punc) {
//            super(capacity, punc, n);
//        }
//
//        /**
//         * max frequency difference from either 0.0 or 1.0 to be polarized enough.
//         * use the < 0.5 value here, ex: 0.1 means that 0..0.1 and 0.9..1.0 will be accepted
//         */
//        public final FloatRange freqMax = new FloatRange(0.1f, 0f, 1f);
//
//
//
//        /** whether a belief or goal is sufficiently polarized */
//        boolean polarized(float f) {
//            float fm = freqMax.floatValue();
//            return f <= fm || f >= (1f - fm);
//        }
//
//
//        @Override
//        public boolean acceptTask(Task t) {
//            return !(t instanceof SignalTask) && polarized(t.freq());
//        }
//
//        @Override
//        protected Term reify(Task t) {
//            return Inperience2.reifyBeliefOrGoal(t,  nar);
//        }
//
//
//    }
//    public static class Want extends Believe {
//
//        public Want(int capacity, NAR n) {
//            super(capacity, n, GOAL);
//        }
//
//    }
//
//    public static class Wonder extends Inperience {
//
//        public Wonder(int capacity, NAR n) {
//            this(capacity, n, QUESTION);
//        }
//
//        protected Wonder(int capacity, NAR n, byte punc) {
//            super(capacity, punc, n);
//        }
//
//        @Override
//        public boolean acceptTask(Task t) {
//            return true;
//        }
//
//        @Override
//        protected Term reify(Task t) {
//            return Inperience2.reifyQuestion(
//                    //t.target().eval(nar),
//                    t.term(),
//                    t.punc(), nar);
//        }
//    }
//
//    public static class Plan extends Wonder {
//
//        public Plan(int capacity, NAR n) {
//            super(capacity, n, QUEST);
//        }
//
//    }
//
//    protected Inperience(int capacity, byte punc, NAR n) {
//        super(capacity, n, punc);
//    }
////    protected Inperience(byte punc, NAR n) {
////        super(n, punc);
////    }
//
//
//    /** prefilter on task punc */
//    abstract public boolean acceptTask(Task t);
//
//    /**
//     * compute current value of the truth for the task's time;
//     * dont just regurgitate what the task says. the truth may differ
//     * at some point after the task was created so we get a more
//     * updated result.
//     */
//    abstract protected Term reify(Task t);
//
//    /** can be overridden but default is belief */
//    protected byte puncResult() { return BELIEF; }
//
//
//    /** minimum expected reification overhead for filtering candidates by volume limit */
//    final static int MIN_REIFICATION_OVERHEAD = 2 + 1 /* 1 extra to be safe */;
//
//    @Override
//    public void next(What w, BooleanSupplier kontinue) {
//        volMaxPre = (volMaxPost = w.nar.termVolMax.intValue()) - MIN_REIFICATION_OVERHEAD;
//        super.next(w, kontinue);
//    }
//
//    @Override
//    protected boolean filter(Term nextTerm) {
//        return nextTerm.volume() <= volMaxPre;
//    }
//
//    @Override
//    public boolean filter(Task next) {
//
//        if (next.stamp().length == 0)
//            return false;
//
//        if (!acceptTask(next))
//            return false;
//
//        //HACK temporal filter
//        //this shouldnt name a concept with the temporal data:
//        //( &&+- ,believe("lÆåÍÕ7ÝSç",(((--,(g-->forget)) &&+20 x(g,-1)) &&+350 (--,x(g,-1)))),(g-->dex),(g-->amplify))
//        //until fixed, dont include any possibility of temporal data
//        if (next.term().hasAny(Temporal))
//            return false;
//
//        Term nextTerm = Image.imageNormalize(next.term());
//        if (nextTerm.op() == INH) {
//            //prevent immediate cyclic feedback
//            Term pred = nextTerm.sub(1);
//            return pred.op() != ATOM || !operators.contains(pred);
//        }
//
//        return true;
//    }
//
//
//    @Override
//    protected float leak(Task x, What what) {
//
//        Term c;
//        try {
//            c = reify(x).normalize();
//        } catch (Throwable t) {
//            if (NAL.DEBUG)
//                logger.error("{} failed Task reification: {} : {}", this, x, t);
//            return 0;
//        }
//
//        if (!c.op().conceptualizable)
//            return 0;
//        if (c.volume() > volMaxPost)
//            return 0; //TODO try to prevent
//
//        int freq = x.isQuestionOrQuest() || x.isPositive() ? 1 : 0;
//        PreciseTruth t = $.t(
//                freq, nar.confDefault(BELIEF)
//                //freq, Util.lerp(x.isQuestionOrQuest() ? 0.5f : x.polarity(), nar.confMin.floatValue()*2, nar.confDefault(Op.BELIEF))
//        ).dither(nar);
//        if (t == null)
//            return 0;
//
//        byte punc = puncResult();
//
//        SignalTask y = Task.tryTask(c, punc, t, (tt, tr)->{
//            long start = x.start();
//            long end;
//            long now = nar.time();
//            if (start == ETERNAL) {
//                //start = end = ETERNAL;
//                float  dur = what.dur();
//                start = Math.round(now - dur/2);
//                end   = Math.round(now + dur/2);
//            } else {
//                end = x.end();
//            }
//            int dt = nar.dtDither();
//            start = Tense.dither(start, dt, -1);
//            end = Tense.dither(end, dt, +1);
//            return new SignalTask(tt, punc,
//                    tr,
//                    now, start, end, x.stamp()
//            );
//        });
//        if (y!=null) {
//
//            Task.merge(y, x);
//
//            if (NAL.DEBUG) {
//            }
//
//            y.priSet(x.priElseZero() * priFactor.floatValue());
//
//            //System.out.println(y);
//
//            in.accept(y, what);
//            return 1;
//        }
//
//        return 0;
//    }
//
//
//
//}
