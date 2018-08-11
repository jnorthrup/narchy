package nars.derive.step;

import jcog.data.list.FasterList;
import nars.$;
import nars.Op;
import nars.derive.Derivation;
import nars.derive.step.Occurrify.BeliefProjection;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.control.AbstractPred;
import nars.term.control.PREDICATE;
import nars.truth.Truth;
import nars.truth.func.TruthFunc;

import static nars.Op.*;

/**
 * Evaluates the (maximum possible) truth of a premise
 * After temporalization, truth may be recalculated.  the confidence
 * will not exceed the prior value calculated here.
 */
public class Truthify extends AbstractPred<Derivation> {

    private final TruthFunc belief;

    /**
     * cached fields from the truth function: +1=true, 0=false, -1=disabled
     */
    transient final byte beliefSingle, goalSingle, beliefOverlap, goalOverlap;

    private final TruthFunc goal;
    private final BeliefProjection beliefProjection;
    private final byte puncOverride;
    private final PREDICATE<Derivation> timeFilter;


    public Truthify(Term id, byte puncOverride, TruthFunc belief, TruthFunc goal, Occurrify.TaskTimeMerge time) {
        super(id);
        this.puncOverride = puncOverride;
        this.timeFilter = time.filter();
        this.beliefProjection = time.beliefProjection();
        this.belief = belief;
        if (belief != null) {
            beliefSingle = (byte) (belief.single() ? +1 : 0);
            beliefOverlap = (byte) (belief.allowOverlap() ? +1 : 0);
        } else {
            beliefSingle = beliefOverlap = -1;
        }
        this.goal = goal;
        if (goal != null) {
            goalSingle = (byte) (goal.single() ? +1 : 0);
            goalOverlap = (byte) (goal.allowOverlap() ? +1 : 0);
        } else {
            goalSingle = goalOverlap = -1;
        }
    }

    private static final Atomic TRUTH = Atomic.the("truth");

    public static Truthify the(byte puncOverride, TruthFunc beliefTruthOp, TruthFunc goalTruthOp, Occurrify.TaskTimeMerge time) {
        Term truthMode;



            FasterList<Term> args = new FasterList(4);
            if (puncOverride != 0)
                args.add($.quote((char) puncOverride));

            String beliefLabel = beliefTruthOp != null ? beliefTruthOp.toString() : null;
            args.add(beliefLabel != null ? Atomic.the(beliefLabel) : Op.EmptyProduct);

            String goalLabel = goalTruthOp != null ? goalTruthOp.toString() : null;
            args.add(goalLabel != null ? Atomic.the(goalLabel) : Op.EmptyProduct);


            args.add(Atomic.the(time.name()));

            truthMode = $.func(TRUTH, args.toArrayRecycled(Term[]::new));


        return new Truthify(truthMode,
                puncOverride,
                beliefTruthOp, goalTruthOp, time);
    }

    @Override
    public float cost() {
        return 2.0f;
    }


    @Override
    public final boolean test(Derivation d) {

        d.concTruth = null; //reset

        byte punc = punc(d.taskPunc);
        boolean single;
        Truth t;
        switch (punc) {
            case BELIEF:
            case GOAL:
                boolean allowsOverlap;
                if (punc == BELIEF) {
                    switch (beliefSingle) {
                        case -1:
                            return false; //actually this shouldnt reach here if culled in a prior stage
                        case 1:
                            single = true;
                            break;
                        default:
                            single = false;
                            break;
                    }
                    allowsOverlap = beliefOverlap == 1;
                } else {
                    switch (goalSingle) {
                        case -1:
                            return false; //actually this shouldnt reach here if culled in a prior stage
                        case 1:
                            single = true;
                            break;
                        default:
                            single = false;
                            break;
                    }
                    allowsOverlap = goalOverlap == 1;
                }

                if (!allowsOverlap && (single ? d.overlapSingle : d.overlapDouble))
                    return false;

                TruthFunc f = punc == BELIEF ? belief : goal;
                Truth beliefTruth;
                if (single) {
                    beliefTruth = null;
                } else {
                    beliefTruth = beliefProjection(d);
                    if (beliefTruth == null)
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


        return true;
    }

    public Truth beliefProjection(Derivation d) {

        switch (beliefProjection) {
            case Raw:
                return d.beliefTruthRaw;

            case Task:
                return d.beliefTruthDuringTask;

            //case Union: throw new TODO();
            default:
                throw new UnsupportedOperationException(beliefProjection + " unimplemented");
        }

    }


    public byte punc(byte punc) {
        return puncOverride == 0 ? punc : puncOverride;
    }

    /** returns the byte of punctuation of the task that the derivation ultimately will produce if completed.
     *  or 0 if the derivation is impossible.
     */
    public final byte preFilter(Derivation d) {


        byte i = d.taskPunc;

        boolean single;
        byte o = punc(i);
        switch (o) {
            case BELIEF: {
                switch (beliefSingle) {
                    case -1:
                        return 0;
                    case 0: //double
                        single = false; //if task is not single and premise is, fail
                        break;
                    default:
                        single = true;
                        break;
                }
                //if belief does not allow overlap and there is overlap for given truth type, fail
                if (beliefOverlap != 1 && ((single ? d.overlapSingle : d.overlapDouble)))
                    return 0;
                break;
            }
            case GOAL: {
                switch (goalSingle) {
                    case -1:
                        return 0;
                    case 0:
                        single = false; //if task is not single and premise is, fail
                        break;
                    default:
                        single = true;
                        break;
                }
                //if goal does not allow overlap and there is overlap for given truth type, fail
                if (goalOverlap != 1 && ((single ? d.overlapSingle : d.overlapDouble)))
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

        if (timeFilter!=null) {
            d.concSingle = single; //HACK set this temporarily because timeFilter needs it
            if (!timeFilter.test(d)) {
                return 0;
            }
        }

        return o;
    }


}