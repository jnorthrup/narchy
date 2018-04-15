package nars.derive;

import nars.term.Term;
import nars.term.pred.AbstractPred;
import nars.truth.Truth;
import nars.truth.func.TruthOperator;

import static nars.Op.*;

/**
 * Evaluates the (maximum possible) truth of a premise
 * After temporalization, truth may be recalculated.  the confidence
 * will not exceed the prior value calculated here.
 */
abstract public class Solve extends AbstractPred<Derivation> {

    private final TruthOperator belief;
    private final TruthOperator goal;

    Solve(Term id, TruthOperator belief, TruthOperator goal) {
        super(id);
        this.belief = belief;
        this.goal = goal;
    }

    @Override
    public float cost() {
        return 2f;
    }

    @Override
    public final boolean test(Derivation d) {

        byte punc = punc(d);
        boolean single;
        Truth t;
        switch (punc) {
            case BELIEF:
            case GOAL:
                TruthOperator f = (punc == BELIEF) ? belief : goal;

                if (f == null)
                    return false; //there isnt a truth function for this punctuation

                Truth beliefTruth;
                if (single = f.single()) {
                    beliefTruth = null;
                } else {

                    beliefTruth = f.beliefProjected() ? d.beliefTruthProjected : d.beliefTruth;
                    if (beliefTruth == null)
                        return false; //double premise requiring a belief, but belief is null
                }

                if ((t = f.apply(
                        d.taskTruth, //task truth is not involved in the outcome of this; set task truth to be null to prevent any negations below:
                        beliefTruth,
                        d.nar, d.confMin
                )) == null)
                    return false;


                if (!f.allowOverlap() && (single ? d.overlapSingle : d.overlapDouble))
                    return false;


                d.truthFunction = f;

                break;

            case QUEST:
            case QUESTION:
                if (d.overlapSingle)
                    return false;
                //if (o > 0 && d.random.nextFloat() <= o)
                    //return false;

//                byte tp = d.taskPunct;
//                if ((tp == QUEST) || (tp == GOAL))
//                    punc = QUEST; //use QUEST in relation to GOAL or QUEST task

                d.truthFunction = null;
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


    public abstract byte punc(Derivation d);

    /**
     * Created by me on 5/26/16.
     */
    public static final class SolvePuncOverride extends Solve {
        private final byte puncOverride;


        public SolvePuncOverride(Term i, byte puncOverride, TruthOperator belief, TruthOperator desire) {
            super(i, belief, desire);
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
    public static final class SolvePuncFromTask extends Solve {

        public SolvePuncFromTask(Term i, TruthOperator belief, TruthOperator desire) {
            super(i, belief, desire);
        }

        @Override
        public byte punc(Derivation d) {
            return d.taskPunc;
        }

    }


//    static final AbstractPred<Derivation> NotCyclic = new AbstractPred<Derivation>($.the("notCyclic")) {
//
//        @Override
//        public boolean test(Derivation d) {
//            return !d.cyclic;
//        }
//
//        @Override
//        public float cost() {
//            return 0.1f;
//        }
//    };

//    static final AbstractPred<Derivation> NotCyclicIfTaskIsQuestionOrQuest = new AbstractPred<Derivation>($.the("notCyclicIfTaskQue")) {
//
//        @Override
//        public float cost() {
//            return 0.15f;
//        }
//
//        @Override
//        public boolean test(Derivation d) {
//            if (d.cyclic) {
//                byte p = d.taskPunct;
//                if (p == QUESTION || p == QUEST)
//                    return false;
//            }
//            return true;
//        }
//
//    };
}


//                    float e = t.evi() * (1f-overlap);
//                    if (e < Pri.EPSILON) //yes Pri epsilon
//                        return false;
//
//                    t = t.withEvi(e);
//                    if (t.conf() < confMin)
//                        return false;

//                    if (d.random.nextFloat() <= overlap)
//                        return false;
