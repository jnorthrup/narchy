package nars.derive.util;

import nars.$;
import nars.Op;
import nars.derive.Derivation;
import nars.derive.action.PremisePatternAction;
import nars.derive.rule.PremiseRuleBuilder;
import nars.op.UniSubst;
import nars.subterm.Subterms;
import nars.term.Neg;
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


    public static void constrainSubstitute(Subterms a, PremiseRuleBuilder p) {
        //TODO for strict case
    }

    public static void constrainUnifiable(Subterms a, PremisePatternAction p) {

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

    public static void constraintEvent(Subterms a, PremisePatternAction p, boolean unifiable) {
        Term conj = a.sub(0);
        if (conj instanceof Variable) {
            Term x = a.sub(1);
            Term xu = x.unneg();
            if (xu instanceof Variable) {

                p.is(conj, CONJ);
                p.eventable((Variable)xu);

                p.neq((Variable)conj, xu);

                if (x instanceof Neg && (xu.equals(p.taskPattern) || xu.equals(p.beliefPattern)))
                    p.hasAny(conj, NEG);


                if (unifiable) {
                    p.biggerIffConstant((Variable)conj, (Variable)xu /* x */); //TODO
                    p.constraints.add(new EventUnifiability((Variable) conj, (Variable) xu, x instanceof Neg));
                } else {
                    //p.constraints.add(new SubOfConstraint((Variable)conj, (Variable)xu, Event, x instanceof Neg ? -1 : +1));
                }
            }
        }
    }

    static class EventUnifiability extends RelationConstraint<Derivation> {
        private static final Atom U = Atomic.atom(EventUnifiability.class.getSimpleName());
        private final boolean forward, xNeg;

        EventUnifiability(Variable conj, Variable x, boolean xNeg) {
            this(conj, x, xNeg, true);
        }

        private EventUnifiability(Variable conj, Variable x, boolean xNeg, boolean forward) {
            super(U, conj, x, $.the(xNeg), $.the(forward));
            this.xNeg = xNeg;
            this.forward = forward;
        }

        @Override
        protected RelationConstraint newMirror(Variable newX, Variable newY) {
            return new EventUnifiability(newX, newY, xNeg, false);
        }

        @Override
        public boolean invalid(Term a, Term b, Derivation d) {
            Term conj, _x;
            if (forward) {
                conj = a; _x = b;
            } else {
                conj = b; _x = a;
            }

            assert(conj.op()==CONJ);

//            int conjV = conj.volume();

            Term x;
            if (xNeg) {
                if (_x instanceof Neg) {
                    x = _x.unneg();
//                    if (conjV <= x.volume())
//                        return true;
                } else {
//                    if (conjV <= _x.volume()+1)
//                        return true;
                    x = _x.neg();
                }

            } else {
                x = _x;
//                if (conjV <= x.volume())
//                    return true;
            }

            int cs = conj.structure();
            if (x instanceof Neg && !Op.hasAny(cs, NEG.bit))
                return true; // simple test

            boolean cv = conj.hasVars();
            boolean xv = x.hasVars();
            if (!cv && !xv)
                return x.hasAny(CONJ) ? false /* undecidable */ : !Conj.eventOf(conj, x);

            if (x.op()==CONJ) {
                //TODO
                return false; //undecided
            } else {
                return !conj.eventsOR((when, what) -> {
                    return Terms.possiblyUnifiable(what, x, false, Variable);
                }, 0, true, conj.dt() == XTERNAL);
            }

//                    conj.subterms().hasAll(x.structure() & ~(Op.Variables|CONJ.bit));

        }

        @Override
        public float cost() {
            return 0.6f;
        }
    }

    public static final class Unifiability extends RelationConstraint<Derivation> {
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
            return 0.4f;
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