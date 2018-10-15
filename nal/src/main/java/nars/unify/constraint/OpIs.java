//package nars.unify.constraint;
//
//import nars.$;
//import nars.Op;
//import nars.term.Term;
//import nars.unify.Unify;
//
//
//public final class OpIs extends UnifyConstraint {
//
//    private final Op op;
//
//    public OpIs(Term target, /*@NotNull*/ Op o) {
//        super(target, "OpIs", $.quote(o.toString()));
//        op = o;
//    }
//
//    @Override
//    public boolean invalid(Term y, Unify f) {
//        return y.op()!=op;
//    }
//
//    @Override
//    public float cost() {
//        return 0.1f;
//    }
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//
//}
//
