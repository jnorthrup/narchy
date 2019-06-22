package nars.unify.constraint;

import nars.term.Term;
import nars.term.Variable;
import nars.unify.Unify;
import org.jetbrains.annotations.Nullable;

public final class Bigger extends RelationConstraint {

    public Bigger(Variable target, Variable other) {
        super("bigger", target, other);
    }


    @Override
    protected @Nullable RelationConstraint newMirror(Variable newX, Variable newY) {
        return new Smaller(newX, newY);
    }

    @Override
    public float cost() {
        return 0.05f;
    }

    @Override
    public boolean invalid(Term x, Term y, Unify context) {
        return x.volume() <= y.volume();
    }

}
