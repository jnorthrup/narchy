package nars.unify.op;

import nars.$;
import nars.Op;
import nars.term.Term;
import nars.unify.Unify;
import nars.unify.constraint.MatchConstraint;

abstract public class TermMatch {

    /**
     * test the exact term, if found
     */
    abstract public boolean test(Term t);

    /**
     * test what can be inferred from the superterm if the direct locator was not possible
     * this is not the direct superterm but the root of the term in which the path may be longer than length 1 so several layers may separate them
     */
    public boolean testSuper(Term superTerm, boolean trueOrFalse) {
        return true;
    }

    /**
     * term representing any unique parameters beyond the the class name which is automatically incorporated into the predicate it forms
     */
    public abstract Term param();

    public MatchConstraint constraint(Term x, boolean trueOrFalse) {
        return new MyMatchConstraint(x, trueOrFalse);
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
        public boolean testSuper(Term superTerm, boolean trueOrFalse) {
            //return trueOrFalse == superTerm.hasAny(struct);
            return trueOrFalse == (((superTerm.hasAny(struct) && superTerm.subterms().OR(x -> x.hasAny(struct)))));
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

        @Override
        public Term param() {
            if (Integer.bitCount(struct)==1) {
                return Op.strucTerm(struct);
            } else {
                return $.func((anyOrAll ? Op.SECTi.strAtom : Op.SECTe.strAtom), Op.strucTerm(struct));
            }
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
        public boolean testSuper(Term superTerm, boolean trueOrFalse) {

            return  (trueOrFalse) ==
                        (superTerm.has(struct, anyOrAll) &&
                        (volMin == 0 || (superTerm.volume() >= 1+volMin)) &&
                        superTerm.subterms().OR(x -> x.has(struct, anyOrAll)));
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
        public boolean testSuper(Term superTerm, boolean trueOrFalse) {
            return trueOrFalse == (superTerm.volume() >= subsMin + 1); //this is the minimum possible volume, if it was the term and if it was only atoms
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
            return TermMatch.this.cost(); //TODO
        }

        @Override
        public boolean invalid(Term y, Unify f) {
            return (TermMatch.this.test(y) != trueOrFalse);
        }
    }
}
