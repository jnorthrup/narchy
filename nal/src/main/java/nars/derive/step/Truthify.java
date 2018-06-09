package nars.derive.step;

import nars.derive.Derivation;
import nars.derive.step.Occurrify.BeliefProjection;
import nars.term.Term;
import nars.term.control.AbstractPred;
import nars.truth.Truth;
import nars.truth.func.TruthFunc;

import static nars.Op.*;

/**
 * Evaluates the (maximum possible) truth of a premise
 * After temporalization, truth may be recalculated.  the confidence
 * will not exceed the prior value calculated here.
 */
abstract public class Truthify extends AbstractPred<Derivation>  {

    private final TruthFunc belief;
    private final TruthFunc goal;
    private final BeliefProjection beliefProjection;

    Truthify(Term id, TruthFunc belief, TruthFunc goal, BeliefProjection beliefProjection) {
        super(id);
        this.belief = belief;
        this.goal = goal;
        this.beliefProjection = beliefProjection;
    }

    @Override
    public float cost() {
        return 2.0f;
    }


    @Override
    public final boolean test(Derivation d) {

        d.truthFunction = null; 

        byte punc = punc(d);
        boolean single;
        Truth t;
        switch (punc) {
            case BELIEF:
            case GOAL:
                TruthFunc f = (punc == BELIEF) ? belief : goal;
                if (f == null)
                    return false; 

                single = f.single();
                if (!f.allowOverlap() && (single ? d.overlapSingle : d.overlapDouble))
                    return false;

                Truth beliefTruth;
                if (single) {
                    beliefTruth = null;
                } else {
                    switch (beliefProjection) {
                        case Raw: beliefTruth = d.beliefTruth; break;
                        case Task: beliefTruth = d.beliefTruthDuringTask; break;
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

            default:
                throw new InvalidPunctuationException(punc);
        }

        d.concTruth = t;
        d.concPunc = punc;
        d.single = single;
        return true;
    }


    abstract byte punc(Derivation d);



    /**
     * Created by me on 5/26/16.
     */
    public static final class TruthifyPuncOverride extends Truthify {
        private final byte puncOverride;


        public TruthifyPuncOverride(Term id, byte puncOverride, TruthFunc belief, TruthFunc desire, BeliefProjection projectBeliefToTask) {
            super(id, belief, desire, projectBeliefToTask);
            this.puncOverride = puncOverride;
        }


        @Override
        public byte punc(Derivation d) {
            return puncOverride;
        }

    }

    /**
     * Created by me on 5/26/16.
     */
    public static final class TruthifyPuncFromTask extends Truthify {

        public TruthifyPuncFromTask(Term i, TruthFunc belief, TruthFunc desire, BeliefProjection projectBeliefToTask) {
            super(i, belief, desire, projectBeliefToTask);
        }

        @Override
        byte punc(Derivation d) {
            return d.taskPunc;
        }

    }



















































































































}












