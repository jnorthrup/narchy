package nars.derive.condition;

import nars.$;
import nars.Op;
import nars.derive.model.Derivation;
import nars.derive.rule.PremiseRule;
import nars.op.UniSubst;
import nars.subterm.Subterms;
import nars.term.Term;
import nars.term.Terms;
import nars.term.Variable;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.util.conj.Conj;
import nars.unify.constraint.RelationConstraint;

import static nars.Op.*;
import static nars.time.Tense.XTERNAL;

public enum Unifiable { ;

    private static final Atomic UnifyPreFilter = Atomic.the("unifiable");


    public static void constrainSubstitute(Subterms a, PremiseRule p) {
        //TODO for strict case
    }

    public static void constrainUnifiable(Subterms a, PremiseRule p) {

        Term x = /*unwrapPolarize...*/(a.sub(1));
        if (x instanceof Variable) {

            Term y = a.sub(2);
            if (y instanceof Variable) {

                //both x and y are constant

                int varBits;
                if (a.indexOf(UniSubst.DEP_VAR) > 2)
                    varBits = VAR_DEP.bit;
                else if (a.indexOf(UniSubst.INDEP_VAR) > 2)
                    varBits = VAR_INDEP.bit;
                else
                    varBits = VAR_DEP.bit | VAR_INDEP.bit;

                boolean strict = a.contains(UniSubst.NOVEL);

                p.constraints.add(new Unifiability((Variable)x, (Variable)y, strict, varBits));
            }
        }
    }

    public static void constrainConjBeforeAfter(Subterms a, PremiseRule p) {
        Term conj = a.sub(0);
        if (conj instanceof Variable) {
            Term x = a.sub(1);
            if (x instanceof Variable) {
                p.constraints.add(new EventUnifiability((Variable)conj, (Variable)x));
            }
        }
    }

    static class EventUnifiability extends RelationConstraint<Derivation> {
        private static final Atom U = Atomic.atom(EventUnifiability.class.getSimpleName());
        private final boolean forward;

        EventUnifiability(Variable conj, Variable x) {
            this(conj, x , true);
        }

        private EventUnifiability(Variable conj, Variable x, boolean forward) {
            super(U, conj, x, $.the(forward));
            this.forward = forward;
        }

        @Override
        protected RelationConstraint newMirror(Variable newX, Variable newY) {
            return new EventUnifiability(newX, newY, false);
        }

        @Override
        public boolean invalid(Term a, Term b, Derivation d) {
            Term conj, x;
            if (forward) {
                conj = a; x = b;
            } else {
                conj = b; x = a;
            }

            assert(conj.op()==CONJ);

            if (conj.volume() <= x.volume())
                return true;

            boolean cv = conj.hasVars();
            boolean xv = x.hasVars();
            if (!cv && !xv)
                return x.hasAny(CONJ) ? false /* undecidable */ : !Conj.eventOf(conj, x);

            if (x.op()==CONJ) {
                //TODO
                return false; //undecided
            } else {
                return !conj.eventsOR((when, what) -> {
                    return Terms.possiblyUnifiable(what, x, false, Op.Variables);
                }, 0, true, conj.dt() == XTERNAL);
            }

//                    conj.subterms().hasAll(x.structure() & ~(Op.Variables|CONJ.bit));
//            return false; //undecidable

        }

        @Override
        public float cost() {
            return 0.3f;
        }
    }

    static final class Unifiability extends RelationConstraint<Derivation> {
        final boolean isStrict; final int varBits;

        private static final Atom U = Atomic.atom(Unifiability.class.getSimpleName());

        Unifiability(Variable x, Variable y, boolean isStrict, int varBits) {
            super(U, x, y, $.the(isStrict), $.the(varBits));
            this.isStrict = isStrict; this.varBits = varBits;
        }

        @Override
        protected RelationConstraint newMirror(Variable newX, Variable newY) {
            return new Unifiability(newX, newY, isStrict, varBits);
        }

        @Override
        public boolean invalid(Term x, Term y, Derivation context) {
            return !Terms.possiblyUnifiable( x, y, isStrict, varBits);
        }

        @Override
        public float cost() {
            return 0.3f;
        }
    }
}

//    /** TODO test for the specific derivation functors, in case of non-functor Atom in conclusion */
//    public static boolean hasNoFunctor(Term x) {
//        return !(x instanceof Compound) || !((Compound)x).ORrecurse(Functor::isFunc);
//    }
//
//    private final byte[] xpInT, xpInB, ypInT, ypInB;
//    private final boolean isStrict;
//    private final int varBits;
//
////    UnifyPreFilter(Variable x, Term y, int varBits, boolean isStrict) {
////        super("unifiable", x, y, $.the(varBits), isStrict ? $.the("strict") : Op.EmptyProduct);
//
//    Unifiable(byte[] xpInT, byte[] xpInB, byte[] ypInT, byte[] ypInB, int varBits, boolean isStrict) {
//        super($.func(UnifyPreFilter, $.intRadix(varBits, 2), UniSubst.NOVEL.negIf(!isStrict),
//                PremiseRule.pathTerm(xpInT), PremiseRule.pathTerm(xpInB),
//                PremiseRule.pathTerm(ypInT), PremiseRule.pathTerm(ypInB)));
//        this.xpInT = xpInT;
//        this.xpInB = xpInB;
//        this.ypInT = ypInT;
//        this.ypInB = ypInB;
//        this.varBits = varBits;
//        this.isStrict = isStrict;
//    }
//    @Override
//    public boolean test(PreDerivation d) {
//        Term x = xpInT != null ? d.taskTerm.subPath(xpInT) : d.beliefTerm.subPath(xpInB); //assert (x != Bool.Null);
//        if (x == null)
//            return false; //ex: seeking a negation but wasnt negated
//
//        Term y = ypInT != null ? d.taskTerm.subPath(ypInT) : d.beliefTerm.subPath(ypInB);
//        if (y == null)
//            return false; //ex: seeking a negation but wasnt negated
//
////        if (NAL.DEBUG) {
////            if (y == Bool.Null) {
////                throw new WTF((ypInT != null ? d.taskTerm : d.beliefTerm) + " does not resolve "
////                        + Arrays.toString((ypInT != null ? ypInT : ypInB)) + " in " + d.taskTerm);
////            }
////        }
//
//
//
//        return Terms.possiblyUnifiable( x, y, isStrict, varBits);
//    }

//    @Override
//    public float cost() {
//        return COST;
//    }