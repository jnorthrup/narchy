package nars.derive.op;

import jcog.data.list.FasterList;
import nars.$;
import nars.NAL;
import nars.Op;
import nars.derive.Derivation;
import nars.derive.op.Occurrify.BeliefProjection;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.control.AbstractPred;
import nars.truth.MutableTruth;
import nars.truth.Truth;
import nars.truth.func.TruthFunction;

import java.util.function.Predicate;

import static nars.Op.*;

/**
 * Evaluates the (maximum possible) truth of a premise
 * After temporalization, truth may be recalculated.  the confidence
 * will not exceed the prior value calculated here.
 */
public class Truthify extends AbstractPred<Derivation> {

    private final TruthFunction belief;

    /**
     * mode:
     * +1 single premise
     * 0 double premise
     * -1 disabled
     */
    final byte beliefMode, goalMode, questionMode;
    final private boolean beliefOverlap, goalOverlap;

    final TruthFunction goal;
    final BeliefProjection beliefProjection;

    /**
     * punctuation transfer function
     * maps input punctuation to output punctuation. a result of zero cancels
     */
    private final PuncMap punc;

    private final Predicate<Derivation> timeFilter;


    private Truthify(Term id, PuncMap punc, TruthFunction belief, TruthFunction goal, boolean questionSingle, Occurrify.OccurrenceSolver time) {
        super(id);
        this.punc = punc;
        this.timeFilter = time.filter();
        this.beliefProjection = time.beliefProjection();
        this.belief = belief;

        if (belief != null) {
            beliefMode = (byte) (belief.single() ? +1 : 0);
            beliefOverlap = NAL.OVERLAP_ALLOW_BELIEF || belief.allowOverlap();
        } else {
            beliefMode = -1;
            beliefOverlap = false; //N/A
        }

        this.goal = goal;
        if (goal != null) {
            goalMode = (byte) (goal.single() ? +1 : 0);
            goalOverlap = NAL.OVERLAP_ALLOW_GOAL || goal.allowOverlap();
        } else {
            goalMode = -1;
            goalOverlap = false; //N/A
        }

        this.questionMode = (byte) (questionSingle ? 1 : 0);
    }

    private static final Atomic TRUTH = Atomic.the("truth");
    private static final Atomic QUESTION_SINGLE = Atomic.the("?");
    private static final Atomic QUESTION_DOUBLE = Atomic.the("??");

    public static Truthify the(PuncMap punc, TruthFunction beliefTruthOp, TruthFunction goalTruthOp, boolean questionSingle, Occurrify.OccurrenceSolver time) {

        boolean outQQ = !questionSingle && (punc.outAny(QUESTION) || punc.outAny(QUEST));

        FasterList<Term> args = new FasterList<>(outQQ ? 5 : 4);

        args.add(punc);

        String beliefLabel = beliefTruthOp != null ? beliefTruthOp.toString() : null;
        args.add(beliefLabel != null ? Atomic.the(beliefLabel) : Op.EmptyProduct);

        String goalLabel = goalTruthOp != null ? goalTruthOp.toString() : null;
        args.add(goalLabel != null ? Atomic.the(goalLabel) : Op.EmptyProduct);

        if (outQQ)
            args.add(QUESTION_DOUBLE);

        args.add(time.term);

        return new Truthify( $.func(TRUTH, args.toArrayRecycled(Term[]::new)),
                punc,
                beliefTruthOp, goalTruthOp,
                questionSingle,
                time);
    }

    @Override
    public float cost() {
        return 2.0f;
    }

    @Override
    public final boolean test(Derivation d) {

        d.truth.clear(); //<- may not be necessary
        d.single = false;
        d.punc = 0;
        d.truthFunction = null;

        boolean single;

        byte punc = this.punc.get(d.taskPunc);
        switch (punc) {
            case BELIEF:
            case GOAL:
                single = (punc == BELIEF ? beliefMode : goalMode) == 1;
                MutableTruth beliefTruth;
                if (single) {
                    beliefTruth = null;
                } else {
                    beliefTruth = beliefProjection.apply(d);
                    if (!beliefTruth.is())
                        return false; //double but beliefTruth not defined
                }

                TruthFunction f = punc == BELIEF ? belief : goal;

                MutableTruth taskTruth = d.taskTruth;
                if (!taskTruth.is())
                    taskTruth = null;

                Truth ff = f.apply(taskTruth, beliefTruth, d.confMin, d.nar);
                if (!d.truth.set(ff).is())
                    return false;

                d.truthFunction = f;

                break;

            case QUEST:
            case QUESTION:
                single = questionMode == 1;
                break;

            case 0:
                return false;

            default:
                throw new InvalidPunctuationException(punc);
        }

        d.punc = punc;
        d.single = single;

        return true;
    }


    /**
     * returns the byte of punctuation of the task that the derivation ultimately will produce if completed.
     * or 0 if the derivation is impossible.
     */
    public final byte preFilter(Derivation d) {


        byte o = this.punc.get(d.taskPunc);

        int m = -1;
        switch (o) {
            case BELIEF: m = beliefMode; break;
            case GOAL: m = goalMode; break;
            case QUESTION:
            case QUEST:
                m = questionMode; break;
        }
        if(m == -1)
            return 0;
        boolean single = (m == 1);
        boolean overlapping = (single ? d.overlapSingle : d.overlapDouble);
        switch (o) {
            case GOAL:
            case BELIEF: {
                //allow overlap?
                if (overlapping && !((o == BELIEF) ? beliefOverlap : goalOverlap))
                    return 0;
                break;
            }
            case QUEST:
            case QUESTION:
                if (overlapping)
                    return 0;

                break;

            default:
                throw new UnsupportedOperationException();
        }

        if (!single) {
            if (beliefProjection.apply(d) == null)
                return 0;
        }

        if (timeFilter != null) {
            if (!timeFilter.test(d)) {
                return 0;
            }
        }

        return o;
    }


}