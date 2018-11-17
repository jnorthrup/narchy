package nars.unify.op;

import nars.$;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.unify.Unify;
import nars.unify.constraint.UnifyConstraint;

import javax.annotation.Nullable;

abstract public class TermMatch {

    /**
     * test the exact term, if found
     */
    abstract public boolean test(Term t);

    /**
     * test what can be inferred from the superterm if the direct locator was not possible
     * this is not the direct superterm but the root of the term in which the path may be longer than length 1 so several layers may separate them
     */
    abstract public boolean testSuper(Term superSuperTerm);

    /**
     * term representing any unique parameters beyond the the class name which is automatically incorporated into the predicate it forms
     */
    @Nullable
    public abstract Term param();

    public UnifyConstraint constraint(Term x, boolean trueOrFalse) {
        return new MyUnifyConstraint(x, trueOrFalse);
    }

    public abstract float cost();


    /**
     * is the op one of the true bits of the provide vector ("is any")
     */
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
            //return trueOrFalse == superTerm.hasAny(struct);
            Subterms subs = superTerm.subterms();
            return subs.hasAny(struct) && subs.OR(x -> x.hasAny(struct));
        }
    }

    /**
     * has the term in its structure, one of the true bits of the provide vector ("has any")
     */
    public final static class Has extends TermMatch {
        final int struct;
        private final boolean anyOrAll;
        private final int volMin;

        public Has(Op op, boolean anyOrAll) {
            this(op.bit, anyOrAll, 0);
        }

        public Has(int struct, boolean anyOrAll, int volMin) {
            this.struct = struct;
            this.anyOrAll = anyOrAll;
            this.volMin = volMin;
        }

        private static final Atom ANY = (Atom) Atomic.the("any");
        private static final Atom ALL = (Atom) Atomic.the("all");
        @Override
        public Term param() {
            return $.p( Op.strucTerm(struct), anyOrAll ? ANY : ALL, $.the(volMin));
        }

        @Override
        public float cost() {

            //all is more specific so should be prioritized ahead
            //more # of bits decreases the cost
            return Math.max(
                    (anyOrAll ? 0.11f : 0.09f) - 0.001f * Integer.bitCount(struct),
                    0.001f);

        }

        @Override
        public boolean test(Term term) {
            return term.has(struct, anyOrAll) && (volMin == 0 || term.volume() >= volMin);
        }

        @Override
        public boolean testSuper(Term superTerm) {

            if (volMin == 0 || superTerm.volume() >= 1+volMin) {
                Subterms subs = superTerm.subterms();
                return subs.has(struct, anyOrAll) && subs.OR(x -> x.has(struct, anyOrAll));
            }
            return false;
        }
    }

    /** non-recursive containment */
    public final static class Contains extends TermMatch {

        public final Term x;

        public Contains(Term x) {
            this.x = x;
        }

        @Override
        public Term param() {
            return x;
        }

        @Override
        public float cost() {
            return 0.1f;
        }

        @Override
        public boolean test(Term term) {
            return term.contains(x);
        }

        @Override
        public boolean testSuper(Term superTerm) {
            return superTerm.containsRecursively(x);
        }
    }

    public final static class SubsMin extends TermMatch {

        final short subsMin;

        public SubsMin(short subsMin) {
            this.subsMin = subsMin;
        }

        @Override
        public Term param() {
            return $.the(subsMin);
        }

        @Override
        public boolean test(Term term) {
            return term.subs() >= subsMin;
        }

        @Override
        public boolean testSuper(Term superTerm) {
            return (superTerm.volume() >= subsMin + 1); //this is the minimum possible volume, if it was the term and if it was only atoms
        }

        @Override
        public float cost() {
            return 0.15f;
        }
    }

    private final class MyUnifyConstraint extends UnifyConstraint {

        private final boolean trueOrFalse;

        MyUnifyConstraint(Term x, boolean trueOrFalse) {
            super(x, TermMatch.this.getClass().getSimpleName(), $.p(TermMatch.this.param(), $.the(trueOrFalse)));
            this.trueOrFalse = trueOrFalse;
        }

        @Override
        public float cost() {
            return TermMatch.this.cost(); //TODO
        }

        @Override
        public boolean invalid(Term y, Unify f) {
            return (TermMatch.this.test(y) != trueOrFalse);
        }
    }
}
