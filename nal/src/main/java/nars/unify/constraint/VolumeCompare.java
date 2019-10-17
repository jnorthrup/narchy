package nars.unify.constraint;

import nars.Op;
import nars.term.Term;
import nars.term.Variable;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.unify.Unify;

/** TODO impl a generic volume comparison constraint to replace both Bigger and Smaller */
public final class VolumeCompare extends RelationConstraint {

    static final Term[] ONLY_IF_CONSTANT = new Term[] { Atomic.atom("onlyIfConstants") };
    static final Atom VOLUME_COMPARE = Atomic.atom(VolumeCompare.class.getSimpleName());

    private final boolean onlyIfConstant;
    private final int validComparison;


    public VolumeCompare(Variable target, Variable other, boolean onlyIfConstant, int validComparison) {
        super(VOLUME_COMPARE /* HACK */, target, other, onlyIfConstant ? ONLY_IF_CONSTANT : Op.EmptyTermArray);
        this.onlyIfConstant = onlyIfConstant;
        this.validComparison = validComparison;
    }


    @Override
    protected RelationConstraint newMirror(Variable newX, Variable newY) {
        return new VolumeCompare(newX, newY, onlyIfConstant, -validComparison);
    }

    @Override
    public float cost() {
        return 0.05f;
    }

    @Override
    public boolean invalid(Term x, Term y, Unify context) {
        if (onlyIfConstant && (x.hasVars() || y.hasVars()))
            return false; //assume valid
        return Integer.compare(x.volume(), y.volume()) != validComparison;
        //return (!onlyIfConstant || (!x.hasVars() && !y.hasVars())) && x.volume() <= y.volume();
    }

}
