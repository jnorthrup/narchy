package nars.derive.op;

import jcog.data.list.FasterList;
import nars.$;
import nars.NAL;
import nars.Op;
import nars.derive.condition.PuncMap;
import nars.derive.model.Derivation;
import nars.derive.op.Occurrify.BeliefProjection;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.control.AbstractPred;
import nars.truth.Truth;
import nars.truth.func.TruthFunc;

import java.util.function.Predicate;

import static nars.Op.*;

/**
 * Evaluates the (maximum possible) truth of a premise
 * After temporalization, truth may be recalculated.  the confidence
 * will not exceed the prior value calculated here.
 */
public class Truthify extends AbstractPred<Derivation> {

    private final TruthFunc belief;

    /**
     * mode:
     * +1 single premise
     * 0 double premise
     * -1 disabled
     */
    transient final byte beliefMode, goalMode, questionMode;
    boolean beliefOverlap, goalOverlap;

    private final TruthFunc goal;
    public final BeliefProjection beliefProjection;

    /**
     * punctuation transfer function
     * maps input punctuation to output punctuation. a result of zero cancels
     */
    private final PuncMap punc;

    private final Predicate<Derivation> timeFilter;


    private Truthify(Term id, PuncMap punc, TruthFunc belief, TruthFunc goal, boolean questionSingle, Occurrify.OccurrenceSolver time) {
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

    public static Truthify the(PuncMap punc, TruthFunc beliefTruthOp, TruthFunc goalTruthOp, boolean questionSingle, Occurrify.OccurrenceSolver time) {

        FasterList<Term> args = new FasterList(4);

        args.add(punc);

        String beliefLabel = beliefTruthOp != null ? beliefTruthOp.toString() : null;
        args.add(beliefLabel != null ? Atomic.the(beliefLabel) : Op.EmptyProduct);

        String goalLabel = goalTruthOp != null ? goalTruthOp.toString() : null;
        args.add(goalLabel != null ? Atomic.the(goalLabel) : Op.EmptyProduct);

        args.add(Atomic.the(time.name()));

        return new Truthify(
                $.func(TRUTH, args.toArrayRecycled(Term[]::new)),
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

        d.concTruth = null;
        d.concSingle = false;
        d.concPunc = 0;
        d.truthFunction = null;

        boolean single;
        Truth t;

        byte punc = this.punc.get(d.taskPunc);
        switch (punc) {
            case BELIEF:
            case GOAL:
                single = (punc == BELIEF ? beliefMode : goalMode) == 1;
                Truth beliefTruth;
                if (single) {
                    beliefTruth = null;
                } else {
                    if ((beliefTruth = beliefProjection.apply(d)) == null)
                        return false;
                }

                TruthFunc f = punc == BELIEF ? belief : goal;
                if ((t = f.apply(
                        d.taskTruth,
                        beliefTruth,
                        d.confMin, d.nar
                )) == null)
                    return false;


                d.truthFunction = f;

                break;

            case QUEST:
            case QUESTION:
                single = questionMode == 1;
                t = null;
                break;

            case 0:
                return false;

            default:
                throw new InvalidPunctuationException(punc);
        }

        d.concTruth = t;
        d.concPunc = punc;
        d.concSingle = single;



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