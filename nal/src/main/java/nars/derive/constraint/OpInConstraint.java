//package nars.derive.constraint;
//
//import nars.$;
//import nars.Op;
//import nars.term.Term;
//import nars.term.subst.Unify;
//import org.jetbrains.annotations.NotNull;
//
//
//public final class OpInConstraint extends MatchConstraint {
//
//    private final int ops;
//
//    public OpInConstraint(Term target, /*@NotNull*/ Op... accepted) {
//        super(target, "opIn", $.the(Op.or(accepted)));
//        ops = Op.or(accepted);
//    }
//
//    @Override
//    public boolean invalid(@NotNull Term y, @NotNull Unify f) {
//        return !y.op().in(ops);
//    }
//
//    @Override
//    public float cost() {
//        return 0.1f;
//    }
//}
//
//
