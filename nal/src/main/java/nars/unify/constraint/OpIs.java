package nars.unify.constraint;

import nars.$;
import nars.Op;
import nars.term.Term;
import nars.unify.Unify;


public final class OpIs extends MatchConstraint {

    private final Op op;

    public static OpIs the(Term target, /*@NotNull*/ Op o) {
//        if (o.atomic)
//            return new OpIsAtomic(target, o);
//        else
            return new OpIs(target, o);
    }

    private OpIs(Term target, /*@NotNull*/ Op o) {
        super(target, "OpIs", $.quote(o.toString()));
        op = o;
    }

    @Override
    public boolean invalid(Term y, Unify f) {
        return y.op()!=op;
    }

    @Override
    public float cost() {
        return 0.1f;
    }



//    static class OpIsAtomic extends OpIs {
//        public OpIsAtomic(Term target, Op o) {
//            super(target, o);
//        }
//
//        @Override
//        public boolean invalid(Term y, Unify f) {
//            if (y instanceof Anom)
//                y = (((Derivation)f).anon.get(y)); //resolve original term
//
//            return super.invalid(y, f);
//        }
//    }

}

