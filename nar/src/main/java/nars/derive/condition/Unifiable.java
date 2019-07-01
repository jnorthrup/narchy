package nars.derive.condition;

import nars.$;
import nars.derive.model.Derivation;
import nars.derive.model.PreDerivation;
import nars.derive.rule.PremiseRule;
import nars.op.UniSubst;
import nars.term.*;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.control.AbstractPred;
import nars.unify.constraint.RelationConstraint;

public class Unifiable extends AbstractPred<PreDerivation> {



    private static final Atomic UnifyPreFilter = Atomic.the("unifiable");
    private static final float COST = 0.3f;

    private final byte[] xpInT, xpInB, ypInT, ypInB;
    private final boolean isStrict;
    private final int varBits;

//    UnifyPreFilter(Variable x, Term y, int varBits, boolean isStrict) {
//        super("unifiable", x, y, $.the(varBits), isStrict ? $.the("strict") : Op.EmptyProduct);

    Unifiable(byte[] xpInT, byte[] xpInB, byte[] ypInT, byte[] ypInB, int varBits, boolean isStrict) {
        super($.func(UnifyPreFilter, $.intRadix(varBits, 2), UniSubst.NOVEL.negIf(!isStrict),
                PremiseRule.pathTerm(xpInT), PremiseRule.pathTerm(xpInB),
                PremiseRule.pathTerm(ypInT), PremiseRule.pathTerm(ypInB)));
        this.xpInT = xpInT;
        this.xpInB = xpInB;
        this.ypInT = ypInT;
        this.ypInB = ypInB;
        this.varBits = varBits;
        this.isStrict = isStrict;
    }

    public static void tryAdd(Term x, Term y, Term taskPattern, Term beliefPattern, int varBits, boolean isStrict, PremiseRule r) {
        byte[] xpInT = Terms.pathConstant(taskPattern, x);
        byte[] xpInB = Terms.pathConstant(beliefPattern, x); //try the belief
        if (xpInT != null || xpInB != null) {
            byte[] ypInT = Terms.pathConstant(taskPattern, y);
            byte[] ypInB = Terms.pathConstant(beliefPattern, y); //try the belief
            if (ypInT != null || ypInB != null) {
                if (xpInT!=null && xpInB!=null) {
                    if (xpInB.length < xpInT.length)
                        xpInT = null;
                    else
                        xpInB = null;
                }
                if (ypInT!=null && ypInB!=null) {
                    if (ypInB.length < ypInT.length)
                        ypInT = null;
                    else
                        ypInB = null;
                }

                r.pre.add(new Unifiable(xpInT, xpInB, ypInT, ypInB, varBits, isStrict));
                return;
            }
        }

        if (x instanceof Variable && y instanceof Variable)
            r.constraints.add(new UnifiableConstraint((Variable)x, (Variable)y, isStrict, varBits));
    }

    /** TODO test for the specific derivation functors, in case of non-functor Atom in conclusion */
    public static boolean hasNoFunctor(Term x) {
        return !(x instanceof Compound) || !((Compound)x).ORrecurse(Functor::isFunc);
    }


    @Override
    public boolean test(PreDerivation d) {
        Term x = xpInT != null ? d.taskTerm.subPath(xpInT) : d.beliefTerm.subPath(xpInB); //assert (x != Bool.Null);
        if (x == null)
            return false; //ex: seeking a negation but wasnt negated

        Term y = ypInT != null ? d.taskTerm.subPath(ypInT) : d.beliefTerm.subPath(ypInB);
        if (y == null)
            return false; //ex: seeking a negation but wasnt negated

//        if (NAL.DEBUG) {
//            if (y == Bool.Null) {
//                throw new WTF((ypInT != null ? d.taskTerm : d.beliefTerm) + " does not resolve "
//                        + Arrays.toString((ypInT != null ? ypInT : ypInB)) + " in " + d.taskTerm);
//            }
//        }



        return Terms.possiblyUnifiable( x, y, isStrict, varBits);
    }




    @Override
    public float cost() {
        return COST;
    }


    static final class UnifiableConstraint extends RelationConstraint<Derivation> {
        final boolean isStrict; final int varBits;

        private static final Atom U = Atomic.atom(UnifiableConstraint.class.getSimpleName());

        public UnifiableConstraint(Variable x, Variable y, boolean isStrict, int varBits) {
            super(U, x, y, $.the(isStrict), $.the(varBits));
            this.isStrict = isStrict; this.varBits = varBits;
        }

        @Override
        protected RelationConstraint newMirror(Variable newX, Variable newY) {
            return new UnifiableConstraint(newX, newY, isStrict, varBits);
        }

        @Override
        public boolean invalid(Term x, Term y, Derivation context) {
            return !Terms.possiblyUnifiable( x, y, isStrict, varBits);
        }

        @Override
        public float cost() {
            return COST;
        }
    }
}
