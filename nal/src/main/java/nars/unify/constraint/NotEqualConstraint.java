package nars.unify.constraint;

import nars.Op;
import nars.term.Term;
import nars.term.Variable;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;


public final class NotEqualConstraint extends RelationConstraint {

    public NotEqualConstraint(Term target, Term other) {
        super(target, other, "neq");
    }

    @Override
    protected @Nullable RelationConstraint newMirror(Term newX, Term newY) {
        return new NotEqualConstraint(newX, newY);
    }

    @Override
    public float cost() {
        return 0.1f;
    }

    @Override
    public boolean invalid(Term x, Term y) {
        return y.equals(x);
    }

//    @Override
//    public boolean remainInAndWith(RelationConstraint c) {
//        if (c instanceof NotEqualRootConstraint || c instanceof SubOfConstraint)
//            return false;
//        return true;
//    }

    public static final class NotEqualRootConstraint extends RelationConstraint {

        public NotEqualRootConstraint(Term target, Term other) {
            super(target, other, "neqRoot");
        }

        @Override
        protected @Nullable RelationConstraint newMirror(Term newX, Term newY) {
            return new NotEqualRootConstraint(newX, newY);
        }

        @Override
        public float cost() {
            return 0.15f;
        }

        @Override
        public boolean invalid(Term x, Term y) {
            return y.equalsRoot(x);
        }

//        @Override
//        public boolean remainInAndWith(RelationConstraint c) {
//            if (c instanceof NeqRootAndNotRecursiveSubtermOf)
//                return false;
//            return true;
//        }
    }
    public static final class EqualNegConstraint extends RelationConstraint {

        public EqualNegConstraint(Term target, Term other) {
            super(target, other, "eqNeg");
        }


        @Override
        protected @Nullable RelationConstraint newMirror(Term newX, Term newY) {
            return new EqualNegConstraint(newX, newY);
        }

        @Override
        public float cost() {
            return 0.15f;
        }

        @Override
        public boolean invalid(Term x, Term y) {
            return !y.equals(x.neg());
        }

//        @Override
//        public boolean remainInAndWith(RelationConstraint c) {
//            if (c instanceof NeqRootAndNotRecursiveSubtermOf)
//                return false;
//            return true;
//        }
    }
//    /** compares term equality, unnegated */
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
    public static final class NeqRootAndNotRecursiveSubtermOf extends RelationConstraint {

        public NeqRootAndNotRecursiveSubtermOf(Term x, Term y) {
            super(x, y, "neqRCom");
        }

        @Override
        protected @Nullable RelationConstraint newMirror(Term newX, Term newY) {
            return new NeqRootAndNotRecursiveSubtermOf(newX, newY);
        }

        @Override
        public float cost() {
            return 0.5f;
        }

        @Override
        public boolean invalid(Term x, Term y) {
            return  isSubtermOfTheOther(x, y, true, true);
        }

        final static Predicate<Term> limit =
                Op.recursiveCommonalityDelimeterWeak;
        //Op.recursiveCommonalityDelimeterStrong;


        static boolean isSubtermOfTheOther(Term a, Term b, boolean recurse, boolean excludeVariables) {

            if ((excludeVariables) && (a instanceof Variable || b instanceof Variable))
                return false;

            int av = a.volume(), bv = b.volume();
            if (av == bv) {
                return false;
            } else {


                if (av < bv) {

                    Term c = a;
                    a = b;
                    b = c;
                }

                return recurse ?
                        a.containsRecursively(b, false, limit) :
                        a.contains(b);
            }
        }


    }
}
