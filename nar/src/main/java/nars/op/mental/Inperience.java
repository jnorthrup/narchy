package nars.op.mental;

import jcog.math.FloatRange;
import nars.$;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.attention.What;
import nars.derive.Derivation;
import nars.derive.action.TaskTransformAction;
import nars.task.TemporalTask;
import nars.task.UnevaluatedTask;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.atom.IdempotentBool;
import nars.term.util.Image;
import nars.term.util.transform.Retemporalize;
import nars.term.util.transform.VariableTransform;
import nars.time.When;
import nars.truth.Truth;
import org.jetbrains.annotations.Nullable;

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
public class Inperience extends TaskTransformAction {


    public static final Atomic believe = Atomic.the("believe");
    public static final Atomic want = Atomic.the("want");
    public static final Atomic wonder = Atomic.the("wonder");

    /** note: NARS calls this 'evaluate' */
    public static final Atomic plan = Atomic.the("plan");

    private static final int VOL_SAFETY_MARGIN = 4;
    static final boolean PROPAGATE_ETERNAL = false;

    public final FloatRange priFactor = new FloatRange(0.1f, (float) 0, 2f);


    public Inperience() {
        super();
        taskAndBeliefEqual(); //all but command
    }

    public static Term reifyQuestion(Term x, byte punc, Term self) {
        return $.INSTANCE.funcImg(verb(punc), self, describe(x));
    }

    static Term reifyBeliefOrGoal(Task t, Term self) {

        Term y = describe(t.term());

        Atomic verb = verb(t.punc());

        //return $.funcImg(verb, self, y.negIf(t.isNegative()));
        return $.INSTANCE.func(verb, self, y.negIf(t.isNegative()));
    }

    private static Atomic verb(byte punc) {
        switch (punc) {
            case BELIEF: return believe;
            case GOAL: return want;
            case QUESTION: return wonder;
            case QUEST: return plan;
            default: throw new UnsupportedOperationException();
        }
    }

    private static Term describe(Term x) {

		x = Retemporalize.retemporalizeXTERNALToDTERNAL.apply(x);
        x = x.hasAny(VAR_QUERY) ? VariableTransform.queryToDepVar.apply(x) : x;
        if (x instanceof IdempotentBool) return IdempotentBool.Null;
        return Image.imageNormalize(x);
    }

    @Override
    protected Task transform(Task x, Derivation d) {

        NAR n = d.nar;

        int volMax = n.termVolMax.intValue();
//        int volMaxPre = volMax-VOL_SAFETY_MARGIN;
//        Random rng = d.random;

        Term self = n.self();

        //Predicate<Task> taskFilter = t -> accept(volMaxPre, t);
        //StableBloomFilter<Task> filter = Terms.newTaskBloomFilter(rng, ((TaskLinkWhat) w).links.links.size());
        //StableBloomFilter<Term> filter = Terms.newTermBloomFilter(rng, ((TaskLinkWhat) w).links.links.size());

//        When when = WhenTimeIs.now(d.what);
//
//        w.sampleUnique(rng, (TaskLink l) -> {
//
//            Term x = l.from();
//            if (x.volume() <= volMaxPre/* && filter.addIfMissing(x)*/) {
//
//                Task t = l.get(when, (z) -> !(z instanceof TemporalTask.Unevaluated));
//                if (t != null) {

                    if (isRecursive(x, self)) {
                        //avoid cyclic
                        //TODO refine
                        //Util.nop();
                    } else {

                        return reflect(volMax,  self, x, d);
                    }
//                }
//            }

//            return kontinue.getAsBoolean();
//        });

        return null;
    }

    private @Nullable Task reflect(int volMax, Term self, Task t, Derivation d) {
        When<What> when = d.when;
        long s, e;
        if (PROPAGATE_ETERNAL || !t.isEternal()) {
            s = t.start(); e = t.end();
        } else {
            s = when.start; e = when.end;
        }

        Task u = null;
        if (t.isBeliefOrGoal()) {
            Term r = reifyBeliefOrGoal(t, self);
            if ((r = validReification(r, volMax)) != null) {
                Truth tt = t.truth();
                u = new InperienceTask(r, $.INSTANCE.t(1.0F,
                    Math.max(d.confMin,
                        tt.conf() * tt.polarity()
//                        (tt.isNegative() ?
//                            tt.expectationNeg() : tt.expectation())
                )), t, s, e);
            }

        } else {
            Term r = reifyQuestion(t.term(), t.punc(), self);
            if ((r = validReification(r, volMax)) != null) {
                float beliefConfDefault = d.nar.confDefault(BELIEF);
                u = new InperienceTask(r, $.INSTANCE.t(1.0F,
                    Math.max(d.confMin, beliefConfDefault * t.priElseZero())
                ), t, s, e);
            }
        }

        if (u != null) {
            Task.fund(u, t, priFactor.floatValue(), true);
        }
        return u;
    }

    /** attempt to filter believe(believe(.... */
    private static boolean isRecursive(Task t, Term self) {
        Term x = t.term();
         if (x.hasAll(INH.bit | PROD.bit) && x.op()==INH && x.sub(0).op()==PROD && x.sub(1).equals(verb(t.punc()))) {
             Term inperiencer = x.sub(0).sub(0);
             return inperiencer instanceof nars.term.Variable || inperiencer.equals(self);
         }
         return false;
    }

//    private boolean accept(int volMax, Task t) {
//        if (t instanceof SeriesBeliefTable.SeriesTask)
//            return false;
//
//        Term tt = t.term();
//        if (tt.volume() > volMax)
//            return false;
//
////        if (tt.hasAny(Op.CONJ))
////            return false; //HACK temporary
//
//        return true;
//    }

    private static @Nullable Term validReification(Term r, int volMax) {
        if (r.op().taskable && r.volume() <= volMax) {
             r = r.normalize();
             if (Task.validTaskTerm(r, BELIEF, true))
                return r;
        }
        return null;
    }



    private static final class InperienceTask extends TemporalTask implements UnevaluatedTask {

        InperienceTask(Term r, Truth tr, Task t, long s, long e) {
            super(r, Op.BELIEF, tr, t.creation(), s, e, t.stamp());
        }

        @Override
        public boolean isInput() {
            return false;
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
