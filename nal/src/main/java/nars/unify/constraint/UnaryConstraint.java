package nars.unify.constraint;

import nars.$;
import nars.Op;
import nars.derive.premise.op.ConstraintAsPremisePredicate;
import nars.term.Term;
import nars.term.Terms;
import nars.term.Variable;
import nars.term.control.PREDICATE;
import nars.unify.Unify;
import org.jetbrains.annotations.Nullable;

public final class UnaryConstraint<U extends Unify> extends UnifyConstraint<U> {

    private TermMatcher termMatcher;
    private final boolean trueOrFalse;

    UnaryConstraint(TermMatcher termMatcher, Variable x, boolean trueOrFalse) {
        super(x, !termMatcher.getClass().isAnonymousClass() ?
                        termMatcher.getClass().getSimpleName() :
                        termMatcher.getClass().toString(),
                ($.p(termMatcher.param() != null ? termMatcher.param() : Op.EmptyProduct).negIf(!trueOrFalse)));
        this.termMatcher = termMatcher;
        this.trueOrFalse = trueOrFalse;
    }

    @Override
    public @Nullable PREDICATE preFilter(Term taskPattern, Term beliefPattern) {
        byte[] xInTask = Terms.pathConstant(taskPattern, x);
        byte[] xInBelief = Terms.pathConstant(beliefPattern, x);
        if (xInTask!=null || xInBelief!=null) {
            return ConstraintAsPremisePredicate.the(this, xInTask, xInBelief, null, null);
        }

        return null;
    }

    @Override
    public float cost() {
        return termMatcher.cost(); //TODO
    }

    @Override
    public boolean invalid(Term y, Unify f) {
        return valid(y) != trueOrFalse;
    }

    public final boolean valid(Term y) {
        return termMatcher.test(y);
    }
}
