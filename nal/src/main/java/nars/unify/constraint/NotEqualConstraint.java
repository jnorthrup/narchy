package nars.unify.constraint;

import nars.Op;
import nars.subterm.Subterms;
import nars.term.Term;
import nars.term.Terms;
import nars.term.Variable;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.unify.Unify;
import org.jetbrains.annotations.Nullable;

import static nars.Op.INH;


public final class NotEqualConstraint extends RelationConstraint {

    public static final Atom neq = Atomic.atom("neq");

    public NotEqualConstraint(Variable target, Variable other) {
        super(neq, target, other);
    }

    @Override
    protected RelationConstraint newMirror(Variable newX, Variable newY) {
        return new NotEqualConstraint(newX, newY);
    }

    @Override
    public float cost() {
        return 0.1f;
    }

    @Override
    public boolean invalid(Term x, Term y, Unify context) {
        return y.equals(x);
    }

//    @Override
//    public boolean remainInAndWith(RelationConstraint c) {
//        if (c instanceof NotEqualRootConstraint || c instanceof SubOfConstraint)
//            return false;
//        return true;
//    }

    public static final class NotEqualRootConstraint extends RelationConstraint {

        public NotEqualRootConstraint(Variable target, Variable other) {
            super("neqRoot", target, other);
        }

        @Override
        protected RelationConstraint newMirror(Variable newX, Variable newY) {
            return new NotEqualRootConstraint(newX, newY);
        }

        @Override
        public float cost() {
            return 0.3f;
        }

        @Override
        public boolean invalid(Term x, Term y, Unify context) {
            return y.equalsRoot(x);
        }

//        @Override
//        public boolean remainInAndWith(RelationConstraint c) {
//            if (c instanceof NeqRootAndNotRecursiveSubtermOf)
//                return false;
//            return true;
//        }
    }

//    static final PREDICATE<PreDerivation> TaskOrBeliefHasNeg = new AbstractPred<>($$("TaskOrBeliefHasNeg")) {
//
//        @Override
//        public boolean test(PreDerivation d) {
//            return d.taskTerm.hasAny(Op.NEG) || d.beliefTerm.hasAny(Op.NEG);
//        }
//
//        @Override
//        public float cost() {
//            return 0.12f;
//        }
//    };

    //    /** compares target equality, unnegated */
//    public static final class NotEqualUnnegConstraint extends RelationConstraint {
//
//
//        public NotEqualUnnegConstraint(Term target, Term y) {
//            super(target, y, "neqUnneg");
//        }
//
//        @Override
//        public float cost() {
//            return 0.2f;
//        }
//
//        @Override
//        public boolean invalid(Term x, Term y) {
//
//            return
//
//                    y.equals(x);
//
//        }
//
//
//    }

    /**
     * containment test of x to y's subterms and y to x's subterms
     */
    public static final class NotEqualAndNotRecursiveSubtermOf extends RelationConstraint {

        public static final Atom neqRCom = Atomic.atom("neqRCom");
        /**
         * TODO move to subclass
         */
        @Deprecated
        public static final boolean root = false;

        public NotEqualAndNotRecursiveSubtermOf(Variable x, Variable y) {
            super(neqRCom, x, y);
        }

        @Override
        protected RelationConstraint newMirror(Variable newX, Variable newY) {
            return new NotEqualAndNotRecursiveSubtermOf(newX, newY);
        }

        @Override
        public float cost() {
            return 0.5f;
        }

        @Override
        public boolean invalid(Term x, Term y, Unify context) {
            return Terms.eqRCom(x, y);
        }


    }

    /**
     * if both are inheritance, prohibit if the subjects or predicates match.  this is to exclude
     * certain derivations which occurr otherwise in NAL1..NAL3
     */
    public static final class NoCommonInh extends RelationConstraint {

        public NoCommonInh(Variable target, Variable other) {
            super("noCommonInh", target, other);
        }

        @Override
        protected RelationConstraint newMirror(Variable newX, Variable newY) {
            return new NoCommonInh(newX, newY);
        }

        @Override
        public float cost() {
            return 0.2f;
        }

        @Override
        public boolean invalid(Term x, Term y, Unify context) {
            return (x.op() == INH && y.op() == INH && (x.sub(0).equals(y.sub(0)) || x.sub(1).equals(y.sub(1))));
        }

    }

    public static final class SubCountEqual extends RelationConstraint {

        public SubCountEqual(Variable target, Variable other) {
            super("SubsEqual", target, other);
        }

        @Override
        protected RelationConstraint newMirror(Variable newX, Variable newY) {
            return new SubCountEqual(newX, newY);
        }

        @Override
        public float cost() {
            return 0.04f;
        }

        @Override
        public boolean invalid(Term x, Term y, Unify context) {
            return x != y && x.subs() != y.subs();
        }

    }

    public static class NotSetsOrDifferentSets extends RelationConstraint {
        public NotSetsOrDifferentSets(Variable target, Variable other) {
            super("notSetsOrDifferentSets", target, other);
        }

        @Override
        protected @Nullable RelationConstraint newMirror(Variable newX, Variable newY) {
            return new NotSetsOrDifferentSets(newX, newY);
        }

        @Override
        public float cost() {
            return 0.1f;
        }

        @Override
        public boolean invalid(Term x, Term y, Unify context) {
            Op xo = x.op();
            return xo.set && (xo == y.op());
        }
    }

    public static class SetsIntersect extends RelationConstraint {
        public SetsIntersect(Variable x, Variable y) {
            super("SetsIntersect", x, y);
        }

        @Override
        protected @Nullable SetsIntersect newMirror(Variable newX, Variable newY) {
            return new SetsIntersect(newX, newY);
        }

        @Override
        public float cost() {
            return 0.1f;
        }

        @Override
        public boolean invalid(Term x, Term y, Unify context) {
            if (x.equals(y))
                return false;
            Op xo = x.op();
            if (xo.set && y.op()== xo) {
                Subterms xx = x.subterms(), yy = y.subterms();
                //TODO check heuristic direction
                if (xx.volume() < yy.volume())
                    return !xx.containsAny(yy);
                else
                    return !yy.containsAny(xx);
            }
            return false;
        }
    }
}
