package nars.derive.constraint;

import nars.term.Term;
import nars.term.subst.Unify;


public final class NotEqualConstraint extends MatchConstraint {

    private final Term other;

    public NotEqualConstraint(Term target, Term other) {
        super(target, "neq", other);
        this.other = other;
    }

    @Override
    public float cost() {
        return 0.2f;
    }

    @Override
    public boolean invalid(Term y, Unify f) {
        Term canNotEqual = f.xy(other);
        return canNotEqual!=null &&
                //Terms.equalAtemporally(y, canNotEqual);
                //y.equals(canNotEqual);
                y.equals(canNotEqual);
    }


}
