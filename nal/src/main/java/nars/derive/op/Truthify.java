package nars.derive.op;

import jcog.data.list.FasterList;
import nars.$;
import nars.Op;
import nars.derive.Derivation;
import nars.derive.op.Occurrify.BeliefProjection;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.control.AbstractPred;
import nars.term.control.PREDICATE;
import nars.truth.Truth;
import nars.truth.func.TruthFunc;
import org.eclipse.collections.api.block.function.primitive.ByteToByteFunction;

import static nars.Op.*;
import static nars.time.Tense.ETERNAL;
import static nars.time.Tense.TIMELESS;

/**
 * Evaluates the (maximum possible) truth of a premise
 * After temporalization, truth may be recalculated.  the confidence
 * will not exceed the prior value calculated here.
 */
public class Truthify extends AbstractPred<Derivation> {

    private final TruthFunc belief;

    /**
     * mode:
     *       +1 single premise
     *        0 double premise
     *       -1 disabled
     */
    transient final byte beliefMode, goalMode;
    boolean beliefOverlap, goalOverlap;

    private final TruthFunc goal;
    private final BeliefProjection beliefProjection;

    /**
     * punctuation transfer function
     * maps input punctuation to output punctuation. a result of zero cancels
     */
    private final ByteToByteFunction punc;

    private final PREDICATE<Derivation> timeFilter;


    public Truthify(Term id, ByteToByteFunction punc, TruthFunc belief, TruthFunc goal, Occurrify.TaskTimeMerge time) {
        super(id);
        this.punc = punc;
        this.timeFilter = time.filter();
        this.beliefProjection = time.beliefProjection();
        this.belief = belief;
        if (belief != null) {
            beliefMode = (byte) (belief.single() ? +1 : 0);
            beliefOverlap = (belief.allowOverlap());
        } else {
            beliefMode = -1;
            beliefOverlap = false; //N/A
        }
        this.goal = goal;
        if (goal != null) {
            goalMode = (byte) (goal.single() ? +1 : 0);
            goalOverlap = goal.allowOverlap();
        } else {
            goalMode = -1;
            goalOverlap = false; //N/A
        }
    }

    private static final Atomic TRUTH = Atomic.the("truth");

    public static Truthify the(ByteToByteFunction punc, TruthFunc beliefTruthOp, TruthFunc goalTruthOp, Occurrify.TaskTimeMerge time) {
        Term truthMode;

        FasterList<Term> args = new FasterList(4);

        args.add($.quote(punc.toString())); //HACK

        String beliefLabel = beliefTruthOp != null ? beliefTruthOp.toString() : null;
        args.add(beliefLabel != null ? Atomic.the(beliefLabel) : Op.EmptyProduct);

        String goalLabel = goalTruthOp != null ? goalTruthOp.toString() : null;
        args.add(goalLabel != null ? Atomic.the(goalLabel) : Op.EmptyProduct);


        args.add(Atomic.the(time.name()));

        truthMode = $.func(TRUTH, args.toArrayRecycled(Term[]::new));


        return new Truthify(truthMode,
                punc,
                beliefTruthOp, goalTruthOp, time);
    }

    @Override
    public float cost() {
        return 2.0f;
    }

    @Override
    public final boolean test(Derivation d) {

        boolean single;
        Truth t;

        byte punc = this.punc.valueOf(d.taskPunc);
        switch (punc) {
            case BELIEF:
            case GOAL:
                single = (punc==BELIEF ? beliefMode : goalMode)==1;
                TruthFunc f = punc == BELIEF ? belief : goal;
                Truth beliefTruth;
                if (single) {
                    beliefTruth = null;
                } else {
                    if ((beliefTruth = beliefProjection(d))==null)
                        return false;
                }

                if ((t = f.apply(
                        d.taskTruth,
                        beliefTruth,
                        d.nar, d.confMin
                )) == null)
                    return false;


                d.truthFunction = f;

                break;

            case QUEST:
            case QUESTION:
                if (d.overlapSingle)
                    return false;

                single = true;
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

        if (d._belief == null || d.concSingle) {

            d.beliefStart = d.beliefEnd = TIMELESS;

        } else {
            switch (beliefProjection) {
                case Raw:
                    d.beliefStart = d._belief.start();
                    d.beliefEnd = d._belief.end();
                    break;
                case Task:
                    long range = (d.taskStart == ETERNAL || d._belief.start()==ETERNAL) ? 0 : d._belief.range() - 1;
                    d.beliefStart = d.taskStart;
                    d.beliefEnd = d.beliefStart + range;
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
        }

        return true;
    }

    private Truth beliefProjection(Derivation d) {

        switch (beliefProjection) {
            case Raw:
                return d.beliefTruthRaw;

            case Task:
                return d.beliefTruthProjectedToTask;

            //case Union: throw new TODO();
            default:
                throw new UnsupportedOperationException();
        }

    }




    /**
     * returns the byte of punctuation of the task that the derivation ultimately will produce if completed.
     * or 0 if the derivation is impossible.
     */
    public final byte preFilter(Derivation d) {

        boolean single;
        byte o = this.punc.valueOf(d.taskPunc);
        switch (o) {
            case GOAL:
            case BELIEF: {
                switch (o == BELIEF ? beliefMode : goalMode) {
                    case -1:
                        return 0;
                    case 0: //double
                        single = false; //if task is not single and premise is, fail
                        break;
                    default:
                        single = true;
                        break;
                }
                boolean overlapIf = (o == BELIEF) ? beliefOverlap : goalOverlap;
                if (!overlapIf && ((single ? d.overlapSingle : d.overlapDouble)))
                    return 0;
                break;
            }
            case QUEST:
            case QUESTION:
                if (d.overlapSingle)
                    return 0;

                single = true;

                break;

            default:
                throw new UnsupportedOperationException();
        }

        if (!single) {
            if (beliefProjection(d) == null)
                return 0;
        }

        if (timeFilter != null) {
            d.concSingle = single; //HACK set this temporarily because timeFilter needs it
            if (!timeFilter.test(d)) {
                return 0;
            }
        }

        return o;
    }


}