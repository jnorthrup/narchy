package nars.unify.constraint;

import nars.Op;
import nars.term.Term;
import nars.term.Variable;
import nars.unify.Unify;
import org.jetbrains.annotations.Nullable;

/** TODO impl a generic volume comparison constraint to replace both Bigger and Smaller */
public final class Bigger extends RelationConstraint {

    private final boolean onlyIfConstant;

    public Bigger(Variable target, Variable other, boolean onlyIfConstant) {
        super("bigger", target, other, onlyIfConstant ? Smaller.ONLY_IF_CONSTANT : Op.EmptyTermArray);
        this.onlyIfConstant = onlyIfConstant;
    }


    @Override
    protected @Nullable RelationConstraint newMirror(Variable newX, Variable newY) {
        return new Smaller(newX, newY, onlyIfConstant);
    }

    @Override
    public float cost() {
        return 0.05f;
    }

    @Override
    public boolean invalid(Term x, Term y, Unify context) {
        return (!onlyIfConstant || (!x.hasVars() && !y.hasVars())) && x.volume() <= y.volume();
    }

}
