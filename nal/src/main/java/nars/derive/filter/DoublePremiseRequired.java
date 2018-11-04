package nars.derive.filter;

import nars.$;
import nars.Op;
import nars.derive.Derivation;
import nars.term.atom.Atomic;
import nars.term.control.AbstractPred;

import static nars.Op.*;

public class DoublePremiseRequired extends AbstractPred<Derivation> {

    final static Atomic key = $.the("DoublePremise");
    final boolean ifBelief, ifGoal, ifQuestionOrQuest;

    public DoublePremiseRequired(boolean ifBelief, boolean ifGoal, boolean ifQuestionOrQuest) {
        super($.func(key,
                ifBelief ? Belief : Op.EmptyProduct,
                ifGoal ? Goal : Op.EmptyProduct,
                ifQuestionOrQuest ? Op.Que : Op.EmptyProduct));
        this.ifBelief = ifBelief;
        this.ifGoal = ifGoal;
        this.ifQuestionOrQuest = ifQuestionOrQuest;
    }

    @Override
    public boolean test(Derivation preDerivation) {
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
        return 0.05f;
    }
}
