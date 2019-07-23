package nars.unify.constraint;

import nars.term.Term;
import nars.term.Variable;
import nars.unify.Unify;

public final class EqualNegConstraint extends RelationConstraint {

    public EqualNegConstraint(Variable target, Variable other) {
        super("eqNeg", target, other);
    }


    @Override
    protected RelationConstraint newMirror(Variable newX, Variable newY) {
        return new EqualNegConstraint(newX, newY);
    }

    @Override
    public float cost() {
        return 0.15f;
    }

    @Override
    public boolean invalid(Term x, Term y, Unify context) {
        return !x.equalsNeg(y);
    }

}
