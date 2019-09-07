package nars.unify.constraint;

import jcog.WTF;
import nars.$;
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
import static nars.Op.NEG;

/** tests a relation between two terms which may be involved (and prevened) from unifying */
abstract public class RelationConstraint<U extends Unify> extends UnifyConstraint<U> {


    public final Variable y;

    private RelationConstraint(Term id, Variable x, Variable y) {
        super(id, x);
        assert(!x.equals(y));
        this.y = y;
    }

    @Deprecated
    protected RelationConstraint(String func, Variable x, Variable y, Term... args) {
        this(Atomic.atom(func), x, y, args);
    }

    protected RelationConstraint(Atom func, Variable x, Variable y, Term... args) {
        this($.func(func, x, args.length > 0 ? $.p(y, $.p(args)) : y), x, y);
    }

    public final RelationConstraint<U> mirror() {
        return newMirror(y, x);
    }

    /** provide the reversed (mirror) constraint */
    abstract protected RelationConstraint<U> newMirror(Variable newX, Variable newY);

    public RelationConstraint<U> neg() {
        return new NegRelationConstraint(this);
    }

//    @Override
//    public final RelationConstraint<U> negIf(boolean negate) {
//        return negate ? neg() : this;
//    }

    @Override
    public final boolean invalid(Term x, U f) {
        Term yy = f.resolveVar(y);
        return yy != y && invalid(x, yy, f);
    }

    abstract public boolean invalid(Term x, Term y, U context);

    /** override to implement subsumption elimination */
    public boolean remainInAndWith(RelationConstraint<U> c) {
        return true;
    }

    public static final class NegRelationConstraint<U extends Unify> extends RelationConstraint<U> {

        public final RelationConstraint<U> r;

        private NegRelationConstraint(RelationConstraint<U> r) {
            super(r.ref.neg(), r.x, r.y);
            this.r = r;
            assert(!(r instanceof NegRelationConstraint) && (r.ref.op()!=NEG));
        }

        @Override
        public boolean remainInAndWith(RelationConstraint<U> c) {
            if (c.equals(r))
                throw new WTF(this + " present in a rule with its opposite " + c);
            return true;
        }

        @Override
        protected RelationConstraint newMirror(Variable newX, Variable newY) {
            return new NegRelationConstraint(this);
            //return new NegRelationConstraint(r.mirror());
        }

        @Override
        public RelationConstraint neg() {
            return r;
        }

        @Override
        public boolean invalid(Term x, Term y, U context) {
            return !r.invalid(x, y, context);
        }

        @Override
        public float cost() {
            return r.cost() + 0.001f;
        }


    }

    public static final class NotEqualConstraint extends RelationConstraint {

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

    }

    public static final class NotEqualPosNegConstraint extends RelationConstraint {

        public static final Atom neq = Atomic.atom("neqPN");

        public NotEqualPosNegConstraint(Variable target, Variable other) {
            super(neq, target, other);
        }

        @Override
        protected NotEqualPosNegConstraint newMirror(Variable newX, Variable newY) {
            return new NotEqualPosNegConstraint(newX, newY);
        }

        @Override
        public float cost() {
            return 0.19f;
        }

        @Override
        public boolean invalid(Term x, Term y, Unify context) {
            return y.equalsPosOrNeg(x);
        }
    }

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

    public static class SetsIntersect extends RelationConstraint {
        public SetsIntersect(Variable x, Variable y) {
            super("SetsIntersect", x, y);
        }

        @Override
        protected @Nullable RelationConstraint.SetsIntersect newMirror(Variable newX, Variable newY) {
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
}
