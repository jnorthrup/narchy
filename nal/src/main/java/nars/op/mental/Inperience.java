package nars.op.mental;

import jcog.Util;
import jcog.math.FloatRange;
import nars.$;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.bag.leak.LeakBack;
import nars.concept.TaskConcept;
import nars.table.BeliefTable;
import nars.task.signal.SignalTask;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.compound.util.Image;
import nars.time.Tense;
import nars.util.term.transform.Retemporalize;
import nars.util.term.transform.TermTransform;
import org.eclipse.collections.api.set.ImmutableSet;
import org.eclipse.collections.impl.factory.Sets;
import org.jetbrains.annotations.NotNull;
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
public class Inperience extends LeakBack {

    public static final Logger logger = LoggerFactory.getLogger(Inperience.class);
    public static final Atomic believe = the("believe");
    public static final Atomic want = the("want");


    public static final Atomic wonder = the("wonder");
    public static final Atomic evaluate = the("plan");
    public static final Atomic reflect = the("reflect");
    public static final ImmutableSet<Atomic> operators = Sets.immutable.of(
            believe, want, wonder, evaluate, reflect);


    /**
     * max frequency difference from either 0.0 or 1.0 to be polarized enough.
     * use the < 0.5 value here, ex: 0.1 means that 0..0.1 and 0.9..1.0 will be accepted
     */
    @NotNull
    public final FloatRange freqMax = new FloatRange(0.1f, 0f, 1f);
    /**
     * multiplier for he sensory task priority to determine inperienced task priority
     * should be < 1.0 to avoid feedback overload
     */
    private final float priFactor = 0.5f;


    public Inperience(@NotNull NAR n, int capacity) {
        super(capacity, n);


    }

    /**
     * compute current value of the truth for the task's time;
     * dont just regurgitate what the task says. the truth may differ
     * at some point after the task was created so we get a more
     * updated result.
     */
    static Term reify(Task t, NAR nar) {
        byte punc = t.punc();
        if (punc == QUEST || punc == QUESTION) {
            return reifyQuestion(t.term(), punc, nar);
        } else {
            return reifyBelief(t, nar);
        }
    }

    private static Term reifyBelief(Task t, NAR nar) {
        TaskConcept c = (TaskConcept) t.concept(nar, true);
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

                return CONJ.the(want, $.func(believe, self, bb.negIf(belief.isNegative())));
            }
        }


    }

    private static Term reifyQuestion(Term x, byte punc, NAR nar) {
        x = x.temporalize(Retemporalize.retemporalizeXTERNALToDTERNAL);
        x = x.hasAny(VAR_QUERY) ? TermTransform.queryToDepVar.transform(x) : x;
        if (x == Null) return Null;

        return $.func(punc == QUESTION ? wonder : evaluate, nar.self(), x);
    }

//    @Override
//    protected float pri(Task t) {
//        float p = super.pri(t);
//        if (t.term().isTemporal()) {
//
//            p /= t.volume();
//        }
//        return p;
//    }

    @Override
    public boolean preFilter(Task next) {


        if (next.isCommand() || next.isInput()
            /*|| task instanceof InperienceTask*/)
            return false;


        Term nextTerm = next.term();
        if (nextTerm.op() == INH) {
            Term nextTermInh = Image.imageNormalize(nextTerm);
            Term pred = nextTermInh.sub(1);
            if (pred.op() == ATOM && operators.contains(pred))
                return false;
        }

        if (next.isBeliefOrGoal()) {


            float f = next.freq();
            float fm = freqMax.floatValue();
            return f <= fm || f >= (1f - fm);
        } else {

            return true;
        }
    }

    @Override
    protected float leak(Task x) {

        float xPri = x.priElseZero();

        Term c = reify(x, nar).normalize();
        if (!c.op().conceptualizable)
            return 0;
        Term d = c.eval(nar, true);
        if (c!=d && !d.op().conceptualizable)
            return 0;
        c = d;

        long start = x.start();
        long end;
        if (start == ETERNAL) {
            long[] focus = nar.timeFocus();
            start = focus[0];
            end = focus[1];
        } else {
            start = Tense.dither(start, nar);
            end = Tense.dither(x.end(), nar);
        }


        float polarity = x.isQuestionOrQuest() ? 0.5f : Math.abs(x.freq() - 0.5f) * 2f;

        SignalTask y = new SignalTask(c, BELIEF,
                $.t(1, Util.lerp(polarity, nar.confMin.floatValue(), nar.confDefault(Op.BELIEF))),
                nar.time(), start, end, x.stamp()
        );
        y.causeMerge(x);

        input(y.log("Inperience").pri(xPri * priFactor));

        return 1;
    }


}
