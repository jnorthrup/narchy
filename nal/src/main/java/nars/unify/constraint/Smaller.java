//package nars.unify.constraint;
//
//import nars.Op;
//import nars.term.Term;
//import nars.term.Variable;
//import nars.term.atom.Atomic;
//import nars.unify.Unify;
//
///** TODO impl a generic volume comparison constraint to replace both Bigger and Smaller */
//@Deprecated public final class Smaller extends RelationConstraint {
//
//    static final Term[] ONLY_IF_CONSTANT = new Term[] { Atomic.atom("onlyIfConstants") };
//    private final boolean onlyIfConstant;
//
//    public Smaller(Variable target, Variable other, boolean onlyIfConstant) {
//        super("smaller", target, other, onlyIfConstant ? ONLY_IF_CONSTANT : Op.EmptyTermArray);
//        this.onlyIfConstant = onlyIfConstant;
//    }
//
//
//    @Override
//    protected RelationConstraint newMirror(Variable newX, Variable newY) {
//        return new Bigger(newX, newY, onlyIfConstant);
//    }
//
//    @Override
//    public float cost() {
//        return 0.05f;
//    }
//
//    @Override
//    public boolean invalid(Term x, Term y, Unify context) {
//        return (!onlyIfConstant || (!x.hasVars() && !y.hasVars())) && x.volume() >= y.volume();
//    }
//
//}
