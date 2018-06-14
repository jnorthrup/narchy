package nars.derive.step;

import jcog.list.FasterList;
import nars.$;
import nars.Op;
import nars.derive.Derivation;
import nars.derive.premise.PremisePatternIndex;
import nars.derive.step.Occurrify.BeliefProjection;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.control.AbstractPred;
import nars.term.control.PrediTerm;
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
    private final PrediTerm<Derivation> timeFilter;


    public Truthify(Term id, byte puncOverride, TruthFunc belief, TruthFunc goal, BeliefProjection beliefProjection, PrediTerm<Derivation> timeFilter) {
        super(id);
        this.puncOverride = puncOverride;
        this.timeFilter = timeFilter;
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
        this.beliefProjection = beliefProjection;
    }

    private static final Atomic TRUTH = Atomic.the("truth");
    private static final Atomic BELIEF_AT = Atomic.the("beliefAt");

    public static Truthify the(PremisePatternIndex index, byte puncOverride, TruthFunc beliefTruthOp, TruthFunc goalTruthOp, BeliefProjection projection, Occurrify.TaskTimeMerge time) {
        Term truthMode;

        if (beliefTruthOp != null || goalTruthOp != null) {


            FasterList<Term> args = new FasterList(4);
            if (puncOverride != 0)
                args.add($.quote((char) puncOverride));

            String beliefLabel = beliefTruthOp != null ? beliefTruthOp.toString() : null;
            args.add(beliefLabel != null ? Atomic.the(beliefLabel) : Op.EmptyProduct);

            String goalLabel = goalTruthOp != null ? goalTruthOp.toString() : null;
            args.add(goalLabel != null ? Atomic.the(goalLabel) : Op.EmptyProduct);

            args.add($.func(BELIEF_AT, Atomic.the(projection.name())));

            truthMode = $.func(TRUTH, args.toArrayRecycled(Term[]::new));
        } else {
            if (puncOverride != 0) {
                truthMode = $.func(TRUTH, $.quote((char) puncOverride));
            } else {
                //truthMode = Op.EmptyProduct; //auto
                throw new UnsupportedOperationException("ambiguous truth/punctuation");
            }
        }

        return new Truthify(index.intern(truthMode),
                puncOverride,
                beliefTruthOp, goalTruthOp,
                projection, time.filter());
    }

    @Override
    public float cost() {
        return 2.0f;
    }


    @Override
    public final boolean test(Derivation d) {

        d.truthFunction = null;

        byte punc = preFilter(d.taskPunc);
        boolean single;
        Truth t;
        switch (punc) {
            case BELIEF:
            case GOAL:
                boolean overlap;
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
                    overlap = beliefOverlap == 1;
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
                    overlap = goalOverlap == 1;
                }

                if (!overlap && (single ? d.overlapSingle : d.overlapDouble))
                    return false;

                TruthFunc f = punc == BELIEF ? belief : goal;
                Truth beliefTruth;
                if (single) {
                    beliefTruth = null;
                } else {
                    switch (beliefProjection) {
                        case Raw:
                            beliefTruth = d.beliefTruth;
                            break;
                        case Task:
                            beliefTruth = d.beliefTruthDuringTask;
                            break;
                        //case Union: throw new TODO();
                        default:
                            throw new UnsupportedOperationException(beliefProjection + " unimplemented");
                    }
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
        d.single = single;
        return true;
    }


    public byte preFilter(byte punc) {
        return puncOverride == 0 ? punc : puncOverride;
    }

    /** returns the byte of punctuation of the task that the derivation ultimately will produce if completed.
     *  or 0 if the derivation is impossible.
     */
    public final byte preFilter(Derivation d) {

        if (timeFilter!=null && !timeFilter.test(d))
            return 0;

        byte i = d.taskPunc;
        boolean singleOnly = d.single, overlapSingle = d.overlapSingle, overlapDouble = d.overlapDouble;


        byte o = preFilter(i);
        switch (o) {
            case BELIEF: {
                boolean ts;
                switch (beliefSingle) {
                    case -1:
                        return 0;
                    case 0: //double
                        if (singleOnly) return 0;
                        ts = false; //if task is not single and premise is, fail
                        break;
                    default:
                        ts = true;
                        break;
                }
                //if belief does not allow overlap and there is overlap for given truth type, fail
                if (beliefOverlap != 1 && ((ts ? overlapSingle : overlapDouble)))
                    return 0;
                break;
            }
            case GOAL: {
                boolean ts;
                switch (goalSingle) {
                    case -1:
                        return 0;
                    case 0:
                        if (singleOnly) return 0;
                        ts = false; //if task is not single and premise is, fail
                        break;
                    default:
                        ts = true;
                        break;
                }
                //if goal does not allow overlap and there is overlap for given truth type, fail
                if (goalOverlap != 1 && ((ts ? overlapSingle : overlapDouble)))
                    return 0;
                break;
            }
            case QUESTION:
                if (overlapSingle) return 0;
                break;
        }
        return o;
    }


}