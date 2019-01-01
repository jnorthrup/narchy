package nars.unify.constraint;

import nars.$;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Term;
import nars.term.Variable;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.unify.Unify;

import javax.annotation.Nullable;

import static nars.Op.VAR_PATTERN;

abstract public class TermMatch {

    /**
     * test the exact term, if found
     */
    abstract public boolean test(Term t);

    /**
     * test what can be inferred from the superterm if the direct locator was not possible
     * this is not the direct superterm but the root of the term in which the path may be longer than length 1 so several layers may separate them
     */
    abstract public boolean testSuper(Term x);

    //    abstract public static class TermMatchEliminatesFalseSuper extends TermMatch {
//
//        @Override
//        public boolean testSuper(Term x, boolean trueOrFalse) {
//            return trueOrFalse ? testSuper(x) : testSuperCant(x);
//        }
//
//        abstract public boolean testSuperCant(Term x);
//    }

    /**
     * term representing any unique parameters beyond the the class name which is automatically incorporated into the predicate it forms
     */
    @Nullable
    public abstract Term param();

    public UnifyConstraint constraint(Variable x, boolean trueOrFalse) {
        return new MyUnifyConstraint(x, trueOrFalse);
    }

    public abstract float cost();


    /**
     * is the op one of the true bits of the provide vector ("is any")
     */
    public static class Is extends TermMatch {

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
            return 0.03f;
        }

        @Override
        public boolean test(Term x) {
            return x.isAny(struct);
        }

        @Override
        public boolean testSuper(Term sx) {
            return sx.subterms().hasAny(struct);
        }
    }

    public static class IsUnneg extends Is {

        public IsUnneg(Op op) {
            super(op);
        }

        @Override
        public float cost() {
            return 0.11f;
        }

        @Override
        public boolean test(Term x) {
            return super.test(x.unneg());
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
                    (anyOrAll ? 0.21f : 0.19f) - 0.001f * Integer.bitCount(struct),
                    0.1f);

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

    /**
     * is of a specific type, and has the term in its structure
     */
    public final static class IsHas extends TermMatch {

        final int structAll;
        final int struct;
        private final int volMin;
        private final byte is;
        private final int depth;

        public static TermMatch get(Term x, int depth) {
            assert(!(x.op()==VAR_PATTERN));

            int xs = x.subterms().structure() & (~VAR_PATTERN.bit);

            return xs != 0 ? new IsHas(x.op(), xs, x.complexity(), depth) : new Is(x.op());
        }

        private IsHas(Op is, int struct, int volMin, int depth) {
            this.is = is.id;
            this.struct = struct;
            this.volMin = volMin;
            this.structAll = struct | is.bit;
            this.depth = depth;
        }


        @Override
        public Term param() {
            return $.p( Op.ops[is].strAtom, Op.strucTerm(struct), $.the(volMin));
        }

        @Override
        public float cost() {
            return 0.15f + 0.001f * depth;
        }

        @Override
        public boolean test(Term term) {
            return term.op().id == is && term.hasAll(structAll) && (volMin == 0 || term.volume() >= volMin);
        }

        @Override
        public boolean testSuper(Term x) {

            return (volMin == 0 || x.volume() >= 1 + volMin) && x.subterms().hasAll(structAll);
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
        public boolean testSuper(Term x) {
            return x.containsRecursively(this.x);
        }
    }
    /** non-recursive containment */
    public final static class Equals extends TermMatch {

        public final Term x;

        public Equals(Term x) {
            this.x = x;
        }

        @Override
        public Term param() {
            return x;
        }

        @Override
        public float cost() {
            return 0.05f;
        }

        @Override
        public boolean test(Term term) {
            return term.equals(x);
        }

        @Override
        public boolean testSuper(Term x) {
            return x.containsRecursively(this.x);
        }
    }
    /** non-recursive containment */
    public final static class EqualsRoot extends TermMatch {

        public final Term x;

        public EqualsRoot(Term x) {
            this.x = x;
        }

        @Override
        public Term param() {
            return x;
        }

        @Override
        public float cost() {
            return 0.08f;
        }

        @Override
        public boolean test(Term term) {
            return term.equalsRoot(x);
        }

        @Override
        public boolean testSuper(Term x) {
            return !x.impossibleSubTerm(this.x);
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
        public boolean testSuper(Term x) {
            return (x.volume() >= subsMin + 1); //this is the minimum possible volume, if it was the term and if it was only atoms
        }

        @Override
        public float cost() {
            return 0.15f;
        }

    }

    private final class MyUnifyConstraint extends UnifyConstraint {

        private final boolean trueOrFalse;

        MyUnifyConstraint(Variable x, boolean trueOrFalse) {
            super(x, TermMatch.this.getClass().getSimpleName(), $.p(TermMatch.this.param(), $.the(trueOrFalse)));
            this.trueOrFalse = trueOrFalse;
        }

        @Override
        public float cost() {
            return TermMatch.this.cost(); //TODO
        }

        @Override
        public boolean invalid(Term y, Unify f) {
            return TermMatch.this.test(y) != trueOrFalse;
        }
    }
}
