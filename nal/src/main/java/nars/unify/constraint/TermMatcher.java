package nars.unify.constraint;

import nars.$;
import nars.Op;
import nars.term.Compound;
import nars.term.Neg;
import nars.term.Term;
import nars.term.Variable;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.control.TermMatch;
import nars.term.util.conj.Conj;
import nars.term.var.VarPattern;
import org.eclipse.collections.api.set.MutableSet;

import javax.annotation.Nullable;
import java.util.function.Function;

import static nars.Op.NEG;
import static nars.Op.VAR_PATTERN;

public abstract class TermMatcher {

    /** basic term constraints */
    public static void constrain(Term x, int depth, Function<?, Term> accessor, MutableSet pre) {

        {
            //structure constraint

            Op o = x.op(); assert (o != VAR_PATTERN);
            int xs = x.subStructure() & (~VAR_PATTERN.bit);
            TermMatcher s = xs != 0 ? new IsHas(o, xs, depth) : new Is(o);

            pre.add(new TermMatch(s, accessor, depth));
        }

        {
            //volume constraint
            int v = x.volume();
            if (v > 1)
                pre.add(new TermMatch(new VolMin(v, depth), accessor, depth));
        }
    }

//    private static final Atom VOL_MIN = Atomic.atom("volMin");

//    static Term volMin(int volMin) {
//        return $.func(VOL_MIN, $.the(volMin));
//    }

    /**
     * test the exact target, if found
     */
    public abstract boolean test(Term t);

    //    abstract public static class TermMatchEliminatesFalseSuper extends TermMatch {
//
//        @Override
//        public boolean testSuper(Term x, boolean trueOrFalse) {
//            return trueOrFalse ? testSuper(x) : testSuperCant(x);
//        }
//
//        abstract public boolean testSuperCant(Term x);
//    }

//    /**
//     * test what can be inferred from the superterm if the direct locator was not possible
//     * this is not the direct superterm but the root of the target in which the path may be longer than length 1 so several layers may separate them
//     */
//    abstract public boolean testSuper(Term x);

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
            return 0.02f;
        }

        @Override
        public boolean test(Term x) {
            return x.isAny(struct);
        }

//        @Override
//        public boolean testSuper(Term sx) {
//            //return sx.subterms().hasAny(struct);
//            return true;
//        }
    }

    public static class IsUnneg extends Is {

        private final boolean requireNegation;

        public IsUnneg(Op op) {
            this(op, true);
        }

        public IsUnneg(Op op, boolean requireNegation) {
            super(op);
            this.requireNegation = requireNegation;
        }

        @Override
        public float cost() {
            return 0.05f;
        }

        @Override
        public boolean test(Term x) {
            return (!requireNegation || x instanceof Neg) && super.test(x.unneg());
        }

//        @Override
//        public boolean testSuper(Term sx) {
//            return sx.hasAny(struct | (requireNegation ? NEG.bit : 0));
//        }
    }

    public static final class VolMin extends TermMatcher {
        private final int volMin;
        private final Atomic param;
        private final float cost;

        public VolMin(int volMin) {
            this(volMin, 0);
        }

        public VolMin(int volMin, int depth) {
            assert(volMin > 1);
            this.param = $.the(this.volMin = volMin);
            this.cost = (0.03f + (0.01f * depth));
        }

        @Override
        public Term param() {
            return param;
        }

        @Override
        public float cost() {
            return cost;
        }

        @Override
        public boolean test(Term term) {
            return term instanceof Compound && term.volume() >= volMin;
        }

    }
    public static final class VolMax extends TermMatcher {
        private final int volMax;
        private final Atomic param;
        private final float cost;

        public VolMax(int volMax) {
            assert(volMax >= 1);
            this.param = $.the(this.volMax = volMax);
            this.cost = (0.04f);
        }

        @Override
        public Term param() {
            return param;
        }

        @Override
        public float cost() {
            return cost;
        }

        @Override
        public boolean test(Term term) {
            return term.volume() <= volMax;
        }

    }
    /**
     * has the target in its structure, meaning it either IS or HAS
     * one of the true bits of the provide vector ("has any")
     */
    public static final class Has extends TermMatcher {
        private static final Atom ANY = (Atom) Atomic.the("any");
        private static final Atom ALL = (Atom) Atomic.the("all");
        final int struct;
        private final boolean anyOrAll;
        private final Term param;
        private final float cost;


        public Has(Op op) {
            this(op.bit, true);
        }

        public Has(int struct, boolean anyOrAll) {
            this.struct = struct;
            this.anyOrAll = anyOrAll;
            this.param = $.func(anyOrAll ? ANY : ALL, Op.strucTerm(struct));
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
            return (anyOrAll ? term.hasAny(struct) : term.hasAll(struct));
        }

//        @Override
//        public boolean testSuper(Term superTerm) {
////
//////            if (volMin == 0 || superTerm.volume() >= 1 + volMin) {
////                Subterms subs = superTerm.subterms();
////                return (anyOrAll ? subs.hasAny(struct) : subs.hasAll(struct))
////                    && subs.OR(anyOrAll ? x -> x.hasAny(struct) : x-> x.hasAll(struct));
//////            }
//////            return false;
//            return true;
//        }
    }

    /**
     * compound is of a specific type, and has the target in its structure
     */
    public static final class IsHas extends TermMatcher {

        final int struct;
        final int structSubs;
        private final byte is;

        private final Term param;
        private final float cost;

        private IsHas(Op is, int structSubs, int depth) {
            assert(!is.atomic && depth > 0 || is!=NEG): "taskTerm or beliefTerm will never be --";

            this.is = is.id;
            this.struct = structSubs | is.bit;
            this.structSubs = structSubs;

            assert(Integer.bitCount(struct) >= Integer.bitCount(structSubs));

            this.cost = (0.07f + (0.01f * depth)) * (1f / (1 + (Integer.bitCount(struct))));

            Atom isParam = Op.the(this.is).strAtom;
			this.param = structSubs != 0 ? $.p(isParam, Op.strucTerm(structSubs)) : isParam;
        }
        @Override
        public boolean test(Term term) {
            return term instanceof Compound &&
                    term.opID() == is &&
                    (structSubs==0 || Op.hasAll(term.subStructure(), structSubs));
        }
//
//        @Override
//        public boolean testSuper(Term term) {
////            return term instanceof Compound &&
////                    testVol(term) && term.subterms().hasAll(struct);
//            return true;
//        }

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
    public static final class Contains extends TermMatcher {

        public final Term x;
//        private final int xStruct;

        public Contains(Term x) {
            assert(!(x instanceof VarPattern)); //HACK
            this.x = x;
//            this.xStruct = x.structure();
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
            return term instanceof Compound && term.contains(x);
        }

//        @Override
//        public boolean testSuper(Term term) {
//            //return term.hasAll(xStruct);
//                    //containsRecursively(this.x);
//            return true;
//        }
    }

    /**
     * non-recursive containment
     */
    public static final class Equals extends TermMatcher {

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
//
//        @Override
//        public boolean testSuper(Term x) {
//            return true;
//        }

        //        @Override
//        public boolean testSuper(Term x) {
//            return x.containsRecursively(this.x);
//        }
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

    public static final class SubsMin extends TermMatcher {

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
            return term instanceof Compound && (subsMin==1 || term.subs() >= subsMin);
        }

        @Override
        public float cost() {
            return 0.075f;
        }

    }
    public static final TermMatcher SubstructureCompounds = new TermMatcher() {

        @Override
        public boolean test(Term term) {
            return Op.hasAny(term.subStructure(), Op.Compound);
        }

        @Nullable
        @Override
        public Term param() {
            return null;
        }

        @Override
        public float cost() {
            return 0.05f;
        }

    };

    public static final class ConjSequence extends TermMatcher {

        public static final ConjSequence the = new ConjSequence();

        private ConjSequence() {
            super();
        }

        @Override
        public boolean test(Term t) {
            return Conj.isSeq(t);
        }

//        @Override
//        public boolean testSuper(Term x) {
//            return true;
//            //return x.hasAny(CONJ);
//        }

        @Nullable
        @Override
        public Term param() {
            return null;
        }

        @Override
        public float cost() {
            return 0.1f;
        }
    }
    public static final class ConjParallel extends TermMatcher {

        public static final ConjParallel the = new ConjParallel();

        private ConjParallel() {
            super();
        }

        @Override
        public boolean test(Term t) {
            return !Conj.isSeq(t);
        }

//        @Override
//        public boolean testSuper(Term x) {
//            //return x.hasAny(CONJ);
//            return true;
//        }

        @Nullable
        @Override
        public Term param() {
            return null;
        }

        @Override
        public float cost() {
            return 0.1f;
        }
    }

    /** the term can appear as an event (condition) in a conjunction compound */
    public static class Eventable extends TermMatcher {

        public static final Eventable the = new Eventable();

        private Eventable() {
            super();
        }

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

//        @Override
//        public boolean testSuper(Term x) {
//            //TODO: Op.Eventables
//            return true;
//        }

        @Override
        public float cost() {
            return 0.015f;
        }

    }
}
