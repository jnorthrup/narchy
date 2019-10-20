package nars.derive.action.op;

import jcog.data.list.FasterList;
import nars.$;
import nars.NAL;
import nars.Op;
import nars.derive.Derivation;
import nars.derive.util.PuncMap;
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
    public final byte beliefMode;
    public final byte goalMode;
    public final boolean beliefOverlap;
    public final boolean goalOverlap;

    final TruthFunction goal;

    /**
     * punctuation transfer function
     * maps input punctuation to output punctuation. a result of zero cancels
     */
    public final PuncMap punc;

    private final Occurrify.OccurrenceSolver time;


    private Truthify(Term id, PuncMap punc, TruthFunction belief, TruthFunction goal, Occurrify.OccurrenceSolver time) {
        super(id);
        this.punc = punc;
        this.time = time;
        this.belief = belief;

        if (belief != null) {
            beliefMode = (byte) (belief.single() ? +1 : 0);
            beliefOverlap = NAL.OVERLAP_ALLOW_BELIEF || belief.allowOverlap();
        } else {
            beliefMode = (byte) -1;
            beliefOverlap = false; //N/A
        }

        this.goal = goal;
        if (goal != null) {
            goalMode = (byte) (goal.single() ? +1 : 0);
            goalOverlap = NAL.OVERLAP_ALLOW_GOAL || goal.allowOverlap();
        } else {
            goalMode = (byte) -1;
            goalOverlap = false; //N/A
        }

    }

    private static final Atomic TRUTH = Atomic.the("truth");

    public static Truthify the(PuncMap punc, TruthFunction beliefTruthOp, TruthFunction goalTruthOp, Occurrify.OccurrenceSolver time) {


        FasterList<Term> args = new FasterList<Term>(4);

        args.add(punc);

        String beliefLabel = beliefTruthOp != null ? beliefTruthOp.toString() : null;
        args.add(beliefLabel != null ? Atomic.the(beliefLabel) : Op.EmptyProduct);

        String goalLabel = goalTruthOp != null ? goalTruthOp.toString() : null;
        args.add(goalLabel != null ? Atomic.the(goalLabel) : Op.EmptyProduct);

        args.add(time.term);

        return new Truthify( $.func(TRUTH, args.toArrayRecycled(Term[]::new)),
                punc,
                beliefTruthOp, goalTruthOp,
                time);
    }

    @Override
    public float cost() {
        return 2.0f;
    }

    @Override
    public final boolean test(Derivation d) {

        Predicate<Derivation> tf = time.filter();
        if (tf!=null && !tf.test(d))
            return false;

        boolean single;
        byte punc = this.punc.get(d.taskPunc);
        switch (punc) {
            case BELIEF:
            case GOAL:
                single = (int) ((int) punc == (int) BELIEF ? beliefMode : goalMode) == 1;

                boolean overlapping = (single ? d.overlapSingle : d.overlapDouble);
                if (overlapping && !((int) punc == (int) BELIEF ? beliefOverlap : goalOverlap))
                    return false;

                MutableTruth beliefTruth;
                if (single) {
                    beliefTruth = null;
                } else {
                    if (!d.hasBeliefTruth())
                        return false;
                    beliefTruth = time.beliefProjection().apply(d);
                    if (!beliefTruth.is())
                        return false; //double but beliefTruth not defined
                }

                TruthFunction f = (int) punc == (int) BELIEF ? belief : goal;

                MutableTruth taskTruth = d.taskTruth;
                if (!taskTruth.is())
                    taskTruth = null;

                Truth ff = f.apply(taskTruth, beliefTruth, d.confMin, d.nar);
                if (!d.truth.set(ff).is())
                    return false;

                break;

            case QUEST:
            case QUESTION:
                single = true; //questionMode == 1;
                if (d.overlapSingle)
                    return false;
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


}