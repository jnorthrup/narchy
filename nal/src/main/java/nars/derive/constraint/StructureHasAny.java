//package nars.derive.constraint;
//
//import nars.$;
//import nars.term.Term;
//import nars.term.subst.Unify;
//
//public final class StructureHasAny extends MatchConstraint {
//
//    private final int structure;
//
//    public StructureHasAny(Term target, int opVector) {
//        super(target, "StructHasAny", $.the(opVector));
//        this.structure = opVector;
//    }
//
//    @Override
//    public boolean invalid(Term y, Unify f) {
//        return !y.hasAny(structure);
//    }
//
//    @Override
//    public float cost() {
//        return 0.1f;
//    }
//}
