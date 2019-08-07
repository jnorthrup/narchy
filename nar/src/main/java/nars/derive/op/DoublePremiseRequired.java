package nars.derive.op;

import nars.$;
import nars.Op;
import nars.Task;
import nars.derive.PreDerivation;
import nars.term.atom.Atomic;
import nars.term.control.AbstractPred;

import static nars.Op.*;

public class DoublePremiseRequired extends AbstractPred<PreDerivation> {

    final static Atomic key = $.the("DoublePremise");
    final boolean ifBelief, ifGoal, ifQuestionOrQuest;

    public DoublePremiseRequired(boolean ifBelief, boolean ifGoal, boolean ifQuestionOrQuest) {
        super($.func(key,
                ifBelief ? Task.BeliefAtom : Op.EmptyProduct,
                ifGoal ? Task.GoalAtom : Op.EmptyProduct,
                ifQuestionOrQuest ? Task.Que : Op.EmptyProduct));
        this.ifBelief = ifBelief;
        this.ifGoal = ifGoal;
        this.ifQuestionOrQuest = ifQuestionOrQuest;
    }

    @Override
    public boolean test(PreDerivation preDerivation) {
        byte x = preDerivation.taskPunc;
        boolean requireDouble;
        switch (x) {
            case BELIEF:
                requireDouble = ifBelief;
                break;
            case GOAL:
                requireDouble = ifGoal;
                break;
            case QUESTION:
            case QUEST:
                requireDouble = ifQuestionOrQuest;
                break;
            default:
                throw new UnsupportedOperationException();
        }
        return !requireDouble || preDerivation.hasBeliefTruth();
    }

    @Override
    public float cost() {
        return 0.005f;
    }
}