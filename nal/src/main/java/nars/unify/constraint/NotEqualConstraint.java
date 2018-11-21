package nars.unify.constraint;

import com.google.common.collect.Iterables;
import nars.Op;
import nars.derive.premise.PreDerivation;
import nars.term.Term;
import nars.term.Variable;
import nars.term.control.AbstractPred;
import nars.term.control.PREDICATE;
import nars.term.var.ImDep;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

import static nars.$.$$;
import static nars.Op.INH;
import static nars.Op.NEG;


public final class NotEqualConstraint extends RelationConstraint {

    public NotEqualConstraint(Term target, Term other) {
        super("neq", target.unneg(), other.negIf(target.op() == NEG));
    }

    @Override
    protected @Nullable RelationConstraint newMirror(Term newX, Term newY) {
        if (!(newX instanceof Variable))
            return null;
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
            super("neqRoot", target, other);
        }

        @Override
        protected @Nullable RelationConstraint newMirror(Term newX, Term newY) {
            return new NotEqualRootConstraint(newX, newY);
        }

        @Override
        public float cost() {
            return 0.35f;
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

    static final PREDICATE<PreDerivation> TaskOrBeliefHasNeg = new AbstractPred<>($$("TaskOrBeliefHasNeg")) {

        @Override
        public boolean test(PreDerivation d) {
            return d.taskTerm.hasAny(Op.NEG) || d.beliefTerm.hasAny(Op.NEG);
        }

        @Override
        public float cost() {
            return 0.12f;
        }
    };

    public static final class EqualNegConstraint extends RelationConstraint {

        public EqualNegConstraint(Term target, Term other) {
            super("eqNeg", target, other);
        }

        @Override
        public @Nullable PREDICATE<PreDerivation> preFilter(Term taskPattern, Term beliefPattern) {
            @Nullable PREDICATE<PreDerivation> p = super.preFilter(taskPattern, beliefPattern);
            if (p ==null) {
                return TaskOrBeliefHasNeg;
            }
            return p;
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
            super("neqRCom", x, y);
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
            return isSubtermOfTheOther(x, y, true, true);
        }

        final static Predicate<Term> limit =
                Op.recursiveCommonalityDelimeterWeak;
        //Op.recursiveCommonalityDelimeterStrong;


        static boolean isSubtermOfTheOther(Term a, Term b, boolean recurse, boolean excludeVariables) {

            if (a.equals(b) || ((excludeVariables) && (a instanceof Variable || b instanceof Variable)))
                return false;

            int av = a.volume(), bv = b.volume();


            //a > b |- a contains b?

            if (av < bv) {

                Term c = a;
                a = b;
                b = c;
            }

            Iterable<Term> bb = inhComponents(b);
            if (bb != null) {
                for (Term bbb : bb) {
                    if (test(a, recurse, excludeVariables, bbb))
                        return true;
                }
                return false;
            } else {
                if (av == bv) {
                    return false;
                } else {
                    return test(a, recurse, excludeVariables, b);
                }
            }
        }


        private static boolean test(Term a, boolean recurse, boolean excludeVariables, Term b) {
            if ((!excludeVariables || !(b instanceof Variable)) && !(b instanceof ImDep)) {
                return recurse ?
                        a.containsRecursively(b, true, limit) :
                        a.contains(b);
            } else
                return false;
        }

        @Nullable
        private static Iterable<Term> inhComponents(Term b) {
            switch (b.op()) {
//                case SETe:
//                case SETi:
                case SECTi:
                case SECTe: {
                    Iterable<Term> x = b.subterms();
                    if (b.hasAny(NEG))
                        x = Iterables.transform(x, Term::unneg);
                    return x;
                }
                default:
                    return null;
            }
        }


    }

    /**
     * if both are inheritance, prohibit if the subjects or predicates match.  this is to exclude
     * certain derivations which occurr otherwise in NAL1..NAL3
     */
    public static final class NoCommonInh extends RelationConstraint {

        public NoCommonInh(Term target, Term other) {
            super("noCommonInh", target, other);
        }

        @Override
        protected @Nullable RelationConstraint newMirror(Term newX, Term newY) {
            return new NoCommonInh(newX, newY);
        }

        @Override
        public float cost() {
            return 0.1f;
        }

        @Override
        public boolean invalid(Term x, Term y) {
            return (x.op() == INH && y.op() == INH && (x.sub(0).equals(y.sub(0)) || x.sub(1).equals(y.sub(1))));
        }

    }

    public static class NotSetsOrDifferentSets extends RelationConstraint {
        public NotSetsOrDifferentSets(Term target, Term other) {
            super("notSetsOrDifferentSets", target, other);
        }

        @Override
        protected @Nullable RelationConstraint newMirror(Term newX, Term newY) {
            return new NotSetsOrDifferentSets(newX, newY);
        }

        @Override
        public float cost() {
            return 0.1f;
        }

        @Override
        public boolean invalid(Term x, Term y) {
            Op xo = x.op();
            return xo.isSet() && (xo == y.op());
        }
    }
}
