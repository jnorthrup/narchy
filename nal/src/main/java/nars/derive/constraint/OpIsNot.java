package nars.derive.constraint;

import nars.$;
import nars.term.Term;
import nars.term.subst.Unify;


public final class OpIsNot extends MatchConstraint {

    private final int op;

//    public OpNotConstraint(/*@NotNull*/ Op o) {
//        this(o.bit);
//    }

    public OpIsNot(Term target, int opVector) {
        super(target, "opIsNot", $.the(opVector));
        this.op = opVector;
    }

    @Override
    public boolean invalid(Term y, Unify f) {

        return y.op().in(op);
    }

    @Override
    public float cost() {
        return 0.1f;
    }
}

