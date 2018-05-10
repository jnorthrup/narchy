package nars.derive.step;

import nars.derive.Derivation;
import nars.term.Term;
import nars.term.control.AbstractPred;
import nars.truth.Truth;
import nars.truth.func.TruthOperator;

import static nars.Op.*;

/**
 * Evaluates the (maximum possible) truth of a premise
 * After temporalization, truth may be recalculated.  the confidence
 * will not exceed the prior value calculated here.
 */
abstract public class Truthify extends AbstractPred<Derivation>  {

    private final TruthOperator belief;
    private final TruthOperator goal;
    private final boolean projectBeliefToTask;

    Truthify(Term id, TruthOperator belief, TruthOperator goal, boolean projectBeliefToTask) {
        super(id);
        this.belief = belief;
        this.goal = goal;
        this.projectBeliefToTask = projectBeliefToTask;
    }

    @Override
    public float cost() {
        return 2.0f;
    }


    @Override
    public final boolean test(Derivation d) {

        d.truthFunction = null; //reset

        byte punc = punc(d);
        boolean single;
        Truth t;
        switch (punc) {
            case BELIEF:
            case GOAL:
                TruthOperator f = (punc == BELIEF) ? belief : goal;
                if (f == null)
                    return false; //there isnt a truth function for this punctuation

                single = f.single();
                if (!f.allowOverlap() && (single ? d.overlapSingle : d.overlapDouble))
                    return false;

                Truth beliefTruth;
                if (single) {
                    beliefTruth = null;
                } else {
                    beliefTruth = projectBeliefToTask ? d.beliefTruthDuringTask : d.beliefTruth;
                    if (beliefTruth == null)
                        return false; //double premise requiring a belief, but belief is null
                }

                if ((t = f.apply(
                        d.taskTruth, //task truth is not involved in the outcome of this; set task truth to be null to prevent any negations below:
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
                //if (o > 0 && d.random.nextFloat() <= o)
                //return false;

//                byte tp = d.taskPunct;
//                if ((tp == QUEST) || (tp == GOAL))
//                    punc = QUEST; //use QUEST in relation to GOAL or QUEST task

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


        public TruthifyPuncOverride(Term id, byte puncOverride, TruthOperator belief, TruthOperator desire, boolean projectBeliefToTask) {
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

        public TruthifyPuncFromTask(Term i, TruthOperator belief, TruthOperator desire, boolean projectBeliefToTask) {
            super(i, belief, desire, projectBeliefToTask);
        }

        @Override
        byte punc(Derivation d) {
            return d.taskPunc;
        }

    }

//    static byte unpunc(byte punc) {
//        switch (punc) {
//            case BELIEF:
//                return 0;
//            case GOAL:
//                return 1;
//            case QUESTION:
//                return 2;
//            case QUEST:
//                return 3;
//            default:
//                throw new UnsupportedOperationException();
//        }
//
//    }
//
//    public PrediTerm preFilter() {
//        boolean belief = this.belief != null;
//        boolean beliefSingle = belief && this.belief.single();
//        boolean beliefAllowOverlap = belief && this.belief.allowOverlap();
//        boolean goal = this.goal != null;
//        boolean goalSingle = goal && this.goal.single();
//        boolean goalAllowOverlap = goal && this.goal.allowOverlap();
//
//        /** punc translation table */
//        byte punc[] = new byte[]{
//                unpunc(punc(BELIEF)),
//                unpunc(punc(GOAL)),
//                unpunc(punc(QUESTION)),
//                unpunc(punc(QUEST)) };
//
//        return new AbstractPred<Derivation>($.func("preTruthify",
//                $.p(punc[0], punc[1], punc[2], punc[3]),
//                $.p(belief, beliefSingle, beliefAllowOverlap, goal, goalSingle, goalAllowOverlap)
//        )) {
//
//            private byte punc(byte taskPunc) {
//                switch (taskPunc) {
//                    case BELIEF:
//                        return punc[0];
//                    case GOAL:
//                        return punc[1];
//                    case QUESTION:
//                        return punc[2];
//                    case QUEST:
//                        return punc[3];
//                    default:
//                        throw new UnsupportedOperationException();
//                }
//            }
//
//            @Override
//            public boolean test(Derivation derivation) {
//                boolean single = derivation.belief == null;
//                switch (punc(derivation.taskPunc)) {
//                    case BELIEF:
//                        return belief && ((!single || beliefSingle) || beliefAllowOverlap || !derivation.overlapDouble);
//                    case GOAL:
//                        return goal && ((!single || goalSingle) || goalAllowOverlap || !derivation.overlapDouble);
//                    case QUESTION:
//                    case QUEST:
//                        return !derivation.overlapSingle;
//                    default:
//                        return false;
//                }
//            }
//        };
//    }
//
//    boolean valid(byte taskPunc, boolean single, boolean overlap, boolean cyclic) {
//        switch (punc(taskPunc)) {
//            case BELIEF:
//                return belief != null && ((!single || belief.single()) || belief.allowOverlap() || !overlap);
//            case GOAL:
//                return goal != null && ((!single || goal.single()) || goal.allowOverlap() || !overlap);
//            case QUESTION:
//            case QUEST:
//                return !cyclic;
//            default:
//                throw new UnsupportedOperationException();
//        }
//    }

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
