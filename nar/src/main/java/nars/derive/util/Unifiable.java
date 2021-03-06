package nars.derive.util;

import nars.$;
import nars.Op;
import nars.derive.Derivation;
import nars.derive.action.PatternHow;
import nars.derive.rule.HowBuilder;
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
import org.eclipse.collections.api.block.predicate.primitive.LongObjectPredicate;

import static nars.Op.*;
import static nars.time.Tense.XTERNAL;

public enum Unifiable { ;

    private static final Atomic UnifyPreFilter = Atomic.the("unifiable");


    public static void constrainSubstitute(Subterms a, HowBuilder p) {
        //TODO for strict case
    }

    public static void constrainUnifiable(Subterms a, PatternHow p) {

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

                p.constrain(new Unifiability((Variable)x, (Variable)y, strict, varBits));
            }
        }
    }

    public static void constraintEvent(Subterms a, PatternHow p, boolean unifiable) {
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
                    p.constrain(new EventUnifiability((Variable) conj, (Variable) xu, x instanceof Neg));
                } else {
                    //p.constraints.add(new SubOfConstraint((Variable)conj, (Variable)xu, Event, x instanceof Neg ? -1 : +1));
                }
            }
        }
    }

    static class EventUnifiability extends RelationConstraint<Derivation.PremiseUnify> {
        private static final Atom U = Atomic.atom(EventUnifiability.class.getSimpleName());
        private final boolean forward;
        private final boolean xNeg;

        EventUnifiability(Variable conj, Variable x, boolean xNeg) {
            this(conj, x, xNeg, true);
        }

        private EventUnifiability(Variable conj, Variable x, boolean xNeg, boolean forward) {
            super(U, conj, x, $.INSTANCE.the(xNeg), $.INSTANCE.the(forward));
            this.xNeg = xNeg;
            this.forward = forward;
        }

        @Override
        protected RelationConstraint newMirror(Variable newX, Variable newY) {
            return new EventUnifiability(newX, newY, xNeg, false);
        }

        @Override
        public boolean invalid(Term a, Term b, Derivation.PremiseUnify d) {
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
                //                    if (conjV <= x.volume())
                //                        return true;
                //                    if (conjV <= _x.volume()+1)
                //                        return true;
                x = _x instanceof Neg ? _x.unneg() : _x.neg();

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
            /* undecidable */
            if (!cv && !xv)
                return !x.hasAny(CONJ) && !Conj.eventOf(conj, x);

            //TODO
            //undecided
            return x.opID() == (int) CONJ.id ? false : !conj.eventsOR(new LongObjectPredicate<Term>() {
                                                                          @Override
                                                                          public boolean accept(long when, Term what) {
                                                                              return Terms.possiblyUnifiable(what, x, false, Variable);
                                                                          }
                                                                      }, 0L,
                true, conj.dt() == XTERNAL
            );

//                    conj.subterms().hasAll(x.structure() & ~(Op.Variables|CONJ.bit));

        }

        @Override
        public float cost() {
            return 0.6f;
        }
    }

    public static final class Unifiability extends RelationConstraint<Derivation.PremiseUnify> {
        final boolean isStrict; final int varBits;

        private static final Atom U = Atomic.atom(Unifiability.class.getSimpleName());

        Unifiability(Variable x, Variable y, boolean isStrict, int varBits) {
            super(U, x, y, $.INSTANCE.the(isStrict), $.INSTANCE.the(varBits));
            this.isStrict = isStrict; this.varBits = varBits;
        }

        @Override
        protected RelationConstraint newMirror(Variable newX, Variable newY) {
            return new Unifiability(newX, newY, isStrict, varBits);
        }

        @Override
        public boolean invalid(Term x, Term y, Derivation.PremiseUnify context) {
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