//package nars.derive.constraint;
//
//import nars.$;
//import nars.control.Derivation;
//import nars.term.Term;
//import nars.term.anon.Anom;
//import nars.term.anon.Anon;
//import nars.term.subst.Unify;
//
//
//public final class OpIsAny extends MatchConstraint {
//
//    private final int structure;
//
//    public OpIsAny(Term target, int opVector) {
//        super(target, "OpIsAny", $.the(opVector));
//        this.structure = opVector;
//    }
//
//    @Override
//    public boolean invalid(Term y, Unify f) {
//        return y.op().in(structure);
//    }
//
//    @Override
//    public float cost() {
//        return 0.1f;
//    }
//}