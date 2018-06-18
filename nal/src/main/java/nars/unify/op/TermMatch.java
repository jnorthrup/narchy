package nars.unify.op;

import nars.$;
import nars.Op;
import nars.term.Term;
import nars.unify.Unify;
import nars.unify.constraint.MatchConstraint;

abstract public class TermMatch  {

    /** test the exact term, if found */
    abstract public boolean test(Term t);

    /** test what can be inferred from the superterm if the direct locator was not possible */
    public boolean testSuper(Term superTerm) {
        return true;
    }

    /** term representing any unique parameters beyond the the class name which is automatically incorporated into the predicate it forms */
    public abstract Term param();

    public MatchConstraint constraint(Term x, boolean trueOrFalse) {
        return new MyMatchConstraint(x, trueOrFalse);
    }

    public abstract float cost();



    /** is the op one of the true bits of the provide vector ("is any") */
    public final static class Is extends TermMatch {

        public final int struct;

        public Is(Op op) {
            this(op.bit);
        }
        public Is(int struct) {
            this.struct = struct;
        }

        @Override
        public Term param() {
            return Op.strucTerm(struct);
        }

        @Override
        public float cost() {
            return 0.1f;
        }

        @Override
        public boolean test(Term term) {
            return term.isAny(struct);
        }

        @Override
        public boolean testSuper(Term superTerm) {
            return superTerm.hasAny(struct);
        }
    }

    /** has the term in its structure, one of the true bits of the provide vector ("has any") */
    public final static class Has extends TermMatch {
        final int struct;

        public Has(Op op) {
            this(op.bit);
        }

        @Override
        public Term param() {
            return Op.strucTerm(struct);
        }

        public Has(int struct) {
            this.struct = struct;
        }

        @Override
        public float cost() {
            return 0.11f;
        }

        @Override
        public boolean test(Term term) {
            return term.hasAny(struct);
        }

        @Override
        public boolean testSuper(Term superTerm) {
            return superTerm.hasAny(struct);
        }
    }

    public final static class SubsMin extends TermMatch {

        final short minSubs;

        public SubsMin(short minSubs) {
            this.minSubs = minSubs;
        }
        @Override
        public Term param() {
            return $.the(minSubs);
        }
        @Override
        public boolean test(Term term) {
            return term.subs() >= minSubs;
        }

        @Override
        public float cost() {
            return 0.15f;
        }
    }

    private final class MyMatchConstraint extends MatchConstraint {

        private final boolean trueOrFalse;

        MyMatchConstraint(Term x, boolean trueOrFalse) {
            super(x, TermMatch.this.getClass().getSimpleName(), TermMatch.this.param());
            this.trueOrFalse = trueOrFalse;
        }

        @Override
        public float cost() {
            return 0.1f; //TODO
        }

        @Override
        public boolean invalid(Term y, Unify f) {
            return TermMatch.this.test(y)==trueOrFalse;
        }
    }
}
