package nars.derive.condition;

import jcog.WTF;
import nars.$;
import nars.NAL;
import nars.derive.PreDerivation;
import nars.derive.premise.PremiseRuleSource;
import nars.op.UniSubst;
import nars.subterm.Subterms;
import nars.term.*;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.control.AbstractPred;
import nars.term.control.PREDICATE;
import nars.unify.Unify;
import org.eclipse.collections.api.set.MutableSet;

import java.util.Arrays;

import static nars.Op.VAR_DEP;
import static nars.Op.VAR_INDEP;
import static nars.term.util.Image.imageNormalize;

public class Unifiable extends AbstractPred<PreDerivation> {

    private final byte[] xpInT, xpInB, ypInT, ypInB;
    private final boolean isStrict;

    private static final Atomic UnifyPreFilter = Atomic.the("unifiable");
    private final int varBits;

//    UnifyPreFilter(Variable x, Term y, int varBits, boolean isStrict) {
//        super("unifiable", x, y, $.the(varBits), isStrict ? $.the("strict") : Op.EmptyProduct);

    Unifiable(byte[] xpInT, byte[] xpInB, byte[] ypInT, byte[] ypInB, int varBits, boolean isStrict) {
        super($.func(UnifyPreFilter, $.intRadix(varBits, 2), UniSubst.NOVEL.negIf(!isStrict),
                PremiseRuleSource.pathTerm(xpInT), PremiseRuleSource.pathTerm(xpInB),
                PremiseRuleSource.pathTerm(ypInT), PremiseRuleSource.pathTerm(ypInB)));
        this.xpInT = xpInT;
        this.xpInB = xpInB;
        this.ypInT = ypInT;
        this.ypInB = ypInB;
        this.varBits = varBits;
        this.isStrict = isStrict;
    }

    public static void tryAdd(Term x, Term y, Term taskPattern, Term beliefPattern, int varBits, boolean strict, MutableSet<PREDICATE<? extends Unify>> pre) {
        //some structure exists that can be used to prefilter
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

                pre.add(new Unifiable(xpInT, xpInB, ypInT, ypInB, varBits, strict));
            }
        }
    }

    /** TODO test for the specific derivation functors, in case of non-functor Atom in conclusion */
    public static boolean hasNoFunctor(Term x) {
        boolean f = x instanceof Variable || !x.ORrecurse(Functor::isFunc);

        return f;
    }


    @Override
    public boolean test(PreDerivation d) {
        Term x = xpInT != null ? d.taskTerm.subPath(xpInT) : d.beliefTerm.subPath(xpInB);
        assert (x != Bool.Null);
        if (x == null)
            return false; //ex: seeking a negation but wasnt negated
        Term y = ypInT != null ? d.taskTerm.subPath(ypInT) : d.beliefTerm.subPath(ypInB);
        if (NAL.DEBUG) {
            if (y == Bool.Null) {
                throw new WTF((ypInT != null ? d.taskTerm : d.beliefTerm) + " does not resolve "
                        + Arrays.toString((ypInT != null ? ypInT : ypInB)) + " in " + d.taskTerm);
            }
        }
        if (y == null)
            return false; //ex: seeking a negation but wasnt negated

        return Terms.possiblyUnifiable( imageNormalize(x), imageNormalize(y), isStrict, varBits);
    }


    @Override
    public float cost() {
        return 0.3f;
    }


}
