package nars.op.mental;

import jcog.Util;
import jcog.math.FloatRange;
import nars.$;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.bag.leak.LeakBack;
import nars.concept.Concept;
import nars.table.BeliefTable;
import nars.task.signal.SignalTask;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.compound.util.Image;
import nars.time.Tense;
import nars.truth.PreciseTruth;
import nars.util.term.transform.TermTransform;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.factory.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static nars.Op.*;
import static nars.term.atom.Atomic.the;
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
    private static final Atomic believe = the("believe");
    private static final Atomic want = the("want");


    private static final Atomic wonder = the("wonder");
    private static final Atomic evaluate = the("plan");
    private static final Atomic reflect = the("reflect");
    private static final ImmutableSet<Atomic> operators = Sets.immutable.of(
            believe, want, wonder, evaluate, reflect);



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
            Term tt = ss.eval(nar, true);
            if (!tt.op().conceptualizable)
                tt = ss; //use the task as-is, ignore the eval result

            return reifyBelief(t, tt, nar);
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
            return reifyBelief(t, t.term().eval(nar, true), nar);
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
            return reifyQuestion(t.term().eval(nar, true), t.punc(), nar);
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
            return reifyQuestion(t.term().eval(nar, true), t.punc(), nar);
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
            if (pred.op() == ATOM && operators.contains(pred))
                return false;
        }

        return true;
    }


    @Override
    protected float leak(Task x) {

        Term c = reify(x).normalize();
        if (!c.op().conceptualizable)
            return 0;

        float polarity = x.isQuestionOrQuest() ? 0.5f : Math.abs(x.freq() - 0.5f) * 2f;
        PreciseTruth t = $.t(1, Util.lerp(polarity, nar.confMin.floatValue(), nar.confDefault(Op.BELIEF))).dither(nar);
        byte punc = puncResult();

        SignalTask y = (SignalTask) Task.tryTask(c, punc, t, (tt, tr)->{
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
            y.causeMerge(x);
            input(y.log("Inperience").priSet(x.priElseZero() * priFactor.floatValue()));
            return 1;
        }

        return 0;
    }

    @Deprecated private static Term reifyBelief(Task t, Term tt, NAR nar) {
        Concept c = nar.conceptualizeDynamic(tt.unneg());
        if (c == null)
            return Null;

        Task belief = ((BeliefTable) c.table(BELIEF))
                .answer(t.start(), t.end(), t.term(), null, nar);

        Term bb = belief != null ? belief.term() : t.term();

        Term self = nar.self();
        if (t.punc() == BELIEF) {
            if (belief == null)
                return Null;
            return $.func(believe, self, bb.negIf(belief.isNegative()));
        } else {
            Task goal = ((BeliefTable) c.table(GOAL))
                    .answer(t.start(), t.end(), bb, null, nar);

            Term want = goal != null ? $.func(Inperience.want, self, goal.term().negIf(goal.isNegative())) : Null;

            if (belief == null)
                return want;
            else {

                return CONJ.the(want, 0, $.func(believe, self, bb.negIf(belief.isNegative())));
            }
        }
    }

    @Deprecated private static Term reifyQuestion(Term x, byte punc, NAR nar) {
        //x = x.temporalize(Retemporalize.retemporalizeXTERNALToDTERNAL);
        x = x.hasAny(VAR_QUERY) ? TermTransform.queryToDepVar.transform(x) : x;
        if (x instanceof Bool) return Null;

        return $.func(punc == QUESTION ? wonder : evaluate, nar.self(), x);
    }

}
