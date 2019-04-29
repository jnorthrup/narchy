package nars.unify.constraint;

import nars.$;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Term;
import nars.term.Variable;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.var.VarPattern;

import javax.annotation.Nullable;

import static nars.Op.VAR_PATTERN;

abstract public class TermMatcher {

    public static TermMatcher get(Term x, int depth) {
        assert (!(x.op() == VAR_PATTERN));

        int xs = x.subterms().structure() & (~VAR_PATTERN.bit);

        int v = x.volume();
        return (xs != 0 || v > 1) ? new IsHas(x.op(), xs, v, depth) : new Is(x.op());
    }

    public static Term volMin(int volMin) {
        return $.func("volMin", $.the(volMin));
    }

    /**
     * test the exact target, if found
     */
    abstract public boolean test(Term t);

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
     * test what can be inferred from the superterm if the direct locator was not possible
     * this is not the direct superterm but the root of the target in which the path may be longer than length 1 so several layers may separate them
     */
    abstract public boolean testSuper(Term x);

    /**
     * target representing any unique parameters beyond the the class name which is automatically incorporated into the predicate it forms
     */
    @Nullable
    public abstract Term param();

    public UnifyConstraint constraint(Variable x, boolean trueOrFalse) {
        return new UnaryConstraint(this, x, trueOrFalse);
    }

    public abstract float cost();

//    public static class Eventable extends TermMatch {
//
//
//        public static final Eventable the = new Eventable();
//
//        private Eventable() {
//            super();
//        }
//
//        @Override
//        public float cost() {
//            return 0.035f;
//        }
//
//        @Override
//        public boolean test(Term x) {
//            return x.unneg().op().eventable;
//        }
//
//        @Override
//        public boolean testSuper(Term sx) {
//            return true; ///TODO
//        }
//
//        @Nullable @Override public Term param() {
//            return null;
//        }
//    }

    /**
     * is the op one of the true bits of the provide vector ("is any")
     */
    public static class Is extends TermMatcher {

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
            return 0.05f;
        }

        @Override
        public boolean test(Term x) {
            return super.test(x.unneg());
        }
    }

    /**
     * has the target in its structure, one of the true bits of the provide vector ("has any")
     */
    public final static class Has extends TermMatcher {
        private static final Atom ANY = (Atom) Atomic.the("any");
        private static final Atom ALL = (Atom) Atomic.the("all");
        final int struct;
        private final boolean anyOrAll;
        private final int volMin;
        private final Term param;
        private final float cost;

        public Has(Op op, boolean anyOrAll) {
            this(op.bit, anyOrAll, 0);
        }

        public Has(int struct, boolean anyOrAll, int volMin) {
            this.struct = struct;
            this.anyOrAll = anyOrAll;
            this.volMin = volMin;
            this.param = $.p(Op.strucTerm(struct), anyOrAll ? ANY : ALL, volMin(volMin));
            this.cost = Math.max(
                    (anyOrAll ? 0.21f : 0.19f) - 0.001f * Integer.bitCount(struct),
                    0.1f);
        }

        @Override
        public Term param() {
            return param;
        }

        @Override
        public float cost() {

            //all is more specific so should be prioritized ahead
            //more # of bits decreases the cost
            return cost;

        }

        @Override
        public boolean test(Term term) {
            return (anyOrAll ? term.hasAny(struct) : term.hasAll(struct)) && (volMin == 0 || term.volume() >= volMin);
        }

        @Override
        public boolean testSuper(Term superTerm) {

            if (volMin == 0 || superTerm.volume() >= 1 + volMin) {
                Subterms subs = superTerm.subterms();
                return (anyOrAll ? subs.hasAny(struct) : subs.hasAll(struct)) && subs.OR(x -> anyOrAll ? x.hasAny(struct) : x.hasAll(struct));
            }
            return false;
        }
    }

    /**
     * is of a specific type, and has the target in its structure
     */
    public final static class IsHas extends TermMatcher {

        final int structAll;
        final int struct;
        private final int volMin;
        private final byte is;

        private final Term param;
        private final float cost;

        private IsHas(Op is, int struct, int volMin, int depth) {
            this.is = is.id;
            this.struct = struct;
            this.volMin = volMin;
            this.structAll = struct | is.bit;
            this.cost = (0.07f + (0.01f * depth)) * (1f / (1 + ((volMin-1)+Integer.bitCount(struct))));

            Atom isParam = Op.ops[this.is].strAtom;
            if (struct > 0 && volMin > 1) {
                this.param = $.p(isParam, Op.strucTerm(struct), volMin(volMin));
            } else if (struct > 0) {
                this.param = $.p(isParam, Op.strucTerm(struct));
            } else {
                this.param = $.p(isParam, volMin(volMin));
            }
        }
        @Override
        public boolean test(Term term) {
            return term.op().id == is && term.hasAll(structAll) && testVol(term);
        }

        @Override
        public boolean testSuper(Term x) {
            return testVol(x) && x.subterms().hasAll(structAll);
        }

        private boolean testVol(Term term) {
            return volMin <= 1 || term.volume() >= volMin;
        }

        @Override
        public Term param() {
            return param;
        }

        @Override
        public float cost() {
            return cost;
        }
    }

    /**
     * non-recursive containment
     */
    public final static class Contains extends TermMatcher {

        public final Term x;

        public Contains(Term x) {
            assert(!(x instanceof VarPattern)); //HACK
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

    /**
     * non-recursive containment
     */
    public final static class Equals extends TermMatcher {

        public final Term x;

        public Equals(Term x) {
            this.x = x;
            assert(!(x instanceof VarPattern)); //HACK
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

//    /**
//     * non-recursive containment
//     */
//    public final static class EqualsRoot extends TermMatcher {
//
//        public final Term x;
//
//        public EqualsRoot(Term x) {
//            this.x = x;
//            assert(!(x instanceof VarPattern)); //HACK
//        }
//
//        @Override
//        public Term param() {
//            return x;
//        }
//
//        @Override
//        public float cost() {
//            return 0.15f;
//        }
//
//        @Override
//        public boolean test(Term term) {
//            return term.equalsRoot(x);
//        }
//
//        @Override
//        public boolean testSuper(Term x) {
//            return !x.impossibleSubTerm(this.x);
//        }
//    }

    public final static class SubsMin extends TermMatcher {

        final short subsMin;

        public SubsMin(short subsMin) {
            this.subsMin = subsMin; assert(subsMin > 0);
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
            return (x.volume() >= subsMin + 1); //this is the minimum possible volume, if it was the target and if it was only atoms
        }

        @Override
        public float cost() {
            return 0.075f;
        }

    }

    /** the term can appear as an event (condition) in a conjunction compound */
    public static final TermMatcher Eventable = new TermMatcher() {

        @Override
        public String toString() {
            return "Eventable";
        }

        @Override
        public Term param() {
            return null;
        }

        @Override
        public boolean test(Term term) {
            return term.unneg().op().eventable;
        }

        @Override
        public boolean testSuper(Term x) {
            //TODO: Op.Eventables
            return true;
        }

        @Override
        public float cost() {
            return 0.015f;
        }

    };

}
