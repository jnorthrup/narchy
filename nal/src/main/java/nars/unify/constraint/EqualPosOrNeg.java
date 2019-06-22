package nars.unify.constraint;

import nars.term.Term;
import nars.term.Variable;
import nars.unify.Unify;
import org.jetbrains.annotations.Nullable;

public final class EqualPosOrNeg extends RelationConstraint {

    public EqualPosOrNeg(Variable target, Variable other) {
        super("eqPN", target, other);
    }


    @Override
    protected @Nullable RelationConstraint newMirror(Variable newX, Variable newY) {
        return new EqualPosOrNeg(newX, newY);
    }

    @Override
    public float cost() {
        return 0.175f;
    }

    @Override
    public boolean invalid(Term x, Term y, Unify context) {
        return !x.equals(y) && !x.equalsNeg(y);
    }

}
