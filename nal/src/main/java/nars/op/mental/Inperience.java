package nars.op.mental;

import jcog.Util;
import jcog.data.list.FasterList;
import jcog.math.FloatRange;
import nars.$;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.bag.leak.LeakBack;
import nars.concept.Concept;
import nars.task.signal.SignalTask;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.util.Conj;
import nars.term.util.Image;
import nars.term.util.transform.TermTransform;
import nars.time.Tense;
import nars.truth.PreciseTruth;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.factory.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static nars.Op.*;
import static nars.time.Tense.ETERNAL;

/**
 * Internal Experience (NAL9)
 * To remember activity as internal action operations
 * <p>
 * https:
 * "Imperience": http:
 */
abstract public class Inperience extends LeakBack {

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
    final TermTransform Described = new TermTransform() {

        private final Atomic events = Atomic.the("so"); //so, then, thus
        private final Atomic duration = Atomic.the("dur");
        private final Atomic If = Atomic.the("if");
        private final Atomic inherits = Atomic.the("is");
        private final Atomic similar = Atomic.the("alike"); //similarity

        @Override
        public Term transform(Term term) {
            switch (term.op()) {

                case INH:
                    return $.func(inherits, transform(term.sub(0)), transform(term.sub(1)) );

                case SIM:
                    return $.func(similar, SETe.the(transform(term.sub(0)), transform(term.sub(1)) ));

                case IMPL:
                    //TODO insert 'dur' for dt()'s
                    return $.func(events, $.func(If, transform(term.sub(0))), transform(term.sub(1)) );

                case CONJ:

                    if (Conj.concurrent(term.dt())) {
                        return $.func(events, SETe.the(term.subterms().transformSubs(this, SETe)));
                    } else {
                        int dur = nar.dur();
                        List<Term> seq = new FasterList();
                        final long[] last = {0};
                        term.eventsWhile((when,what)->{
                            if (!seq.isEmpty()) {
                                long interval = when- last[0];
                                int durs = Math.round(interval/((float)dur));
                                if (durs > 0) {
                                    seq.add($.func(duration, $.the(durs)));
                                }
                            }
                            seq.add(transform(what));
                            last[0] = when;
                            return true;
                        },0, false, false, false,0);

                        return $.func(events, seq.toArray(Op.EmptyTermArray));

                    }
            }
            return term;
        }
    };


    /**
     * multiplier for he sensory task priority to determine inperienced task priority
     * should be < 1.0 to avoid feedback overload
     */
    public final FloatRange priFactor = new FloatRange(0.5f, 0, 2);


    public static class Believe extends Inperience {

        public Believe(NAR n, int capacity) {
            super(n, capacity);
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
            return t.isBelief() && polarized(t.freq());
        }

        @Override
        protected Term reify(Task t) {
            Term ss = t.term();
            Term tt = ss.eval(nar);
            if (!tt.op().conceptualizable)
                tt = ss; //use the task as-is, ignore the eval result

            return reifyBelief(t, tt, nar);
        }

        @Deprecated protected Term reifyBelief(Task t, Term tt, NAR nar) {
            Concept c = nar.conceptualizeDynamic(tt.unneg());
            if (c == null)
                return Null;

            Term self = nar.self();

            Term taskTerm = t.term();
            if (t.punc() == BELIEF) {
                Task belief = c.table(BELIEF)
                        .answer(t.start(), t.end(), taskTerm, null, nar);

                Term bb = belief != null ? Described.transform(belief.term()) : Described.transform(taskTerm);


                if (belief == null)
                    return Null;
                return $.func(believe, self, bb.negIf(belief.isNegative()));
            } else {
                Task goal = c.table(GOAL)
                        .answer(t.start(), t.end(), taskTerm, null, nar);


                Term gg = goal!=null ? Described.transform(goal.term()) : null;
                Term want = gg != null ? $.func(Inperience.want, self, gg.negIf(goal.isNegative())) : Null;

//                if (belief == null)
                    return want;
//                else {
//
//                    return CONJ.the(want, 0, $.func(believe, self, bb.negIf(belief.isNegative())));
//                }
            }
        }



    }
    public static class Want extends Believe {

        public Want(NAR n, int capacity) {
            super(n, capacity);
        }

        @Override
        public boolean acceptTask(Task t) {
            return t.isGoal();
        }

        @Override
        protected Term reify(Task t) {
            return reifyBelief(t, t.term().eval(nar), nar);
        }


    }
    public static class Wonder extends Inperience {

        public Wonder(NAR n, int capacity) {
            super(n, capacity);
        }

        @Override
        public boolean acceptTask(Task t) {
            return t.isQuestion();
        }

        @Override
        protected Term reify(Task t) {
            return reifyQuestion(t.term().eval(nar), t.punc(), nar);
        }
    }
    public static class Plan extends Inperience {

        public Plan(NAR n, int capacity) {
            super(n, capacity);
        }

        @Override
        public boolean acceptTask(Task t) {
            return t.isQuest();
        }

        @Override
        protected Term reify(Task t) {
            return reifyQuestion(t.term().eval(nar), t.punc(), nar);
        }
    }

    protected Inperience(NAR n, int capacity) {
        super(capacity, n);
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






    @Override
    public boolean preFilter(Task next) {


        if (next.isCommand() || next.isInput() || !acceptTask(next)
            /*|| task instanceof InperienceTask*/)
            return false;


        Term nextTerm = next.term();
        if (nextTerm.op() == INH) {
            Term nextTermInh = Image.imageNormalize(nextTerm);
            Term pred = nextTermInh.sub(1);
            return pred.op() != ATOM || !operators.contains(pred);
        }

        return true;
    }


    @Override
    protected float leak(Task x) {

        Term c = reify(x).normalize();
        if (!c.op().conceptualizable)
            return 0;

        float polarity = x.isQuestionOrQuest() ? 0.5f : x.polarity();
        PreciseTruth t = $.t(1, Util.lerp(polarity, nar.confMin.floatValue()*2, nar.confDefault(Op.BELIEF))).dither(nar);
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
            input(y.log("Inperience").priSet(x.priElseZero() * priFactor.floatValue()));
            return 1;
        }

        return 0;
    }


    @Deprecated private static Term reifyQuestion(Term x, byte punc, NAR nar) {
        //x = x.temporalize(Retemporalize.retemporalizeXTERNALToDTERNAL);
        x = x.hasAny(VAR_QUERY) ? TermTransform.queryToDepVar.transform(x) : x;
        if (x instanceof Bool) return Null;

        return $.func(punc == QUESTION ? wonder : evaluate, nar.self(), x);
    }

}
