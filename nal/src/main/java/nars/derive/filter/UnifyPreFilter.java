package nars.derive.filter;

import nars.$;
import nars.derive.premise.PreDerivation;
import nars.derive.premise.PremiseRuleSource;
import nars.term.Term;
import nars.term.Terms;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.control.AbstractPred;
import nars.term.control.PREDICATE;
import org.eclipse.collections.api.set.MutableSet;

import java.util.Arrays;

public class UnifyPreFilter extends AbstractPred<PreDerivation> {

    private final byte[] xpInT, xpInB, ypInT, ypInB;
    private final boolean isStrict;

    private static final Atomic UnifyPreFilter = Atomic.the("unifyPreFilter");
    private final int varBits;

    UnifyPreFilter(byte[] xpInT, byte[] xpInB, byte[] ypInT, byte[] ypInB, int varBits, boolean isStrict) {
        super($.func(UnifyPreFilter, PremiseRuleSource.pp(xpInT), PremiseRuleSource.pp(xpInB), PremiseRuleSource.pp(ypInT), PremiseRuleSource.pp(ypInB)));
        this.xpInT = xpInT;
        this.xpInB = xpInB;
        this.ypInT = ypInT;
        this.ypInB = ypInB;
        this.varBits = varBits;
        this.isStrict = isStrict;
    }

    public static void tryAdd(Term x, Term y, Term taskPattern, Term beliefPattern, int varBits, boolean strict, MutableSet<PREDICATE<PreDerivation>> pre) {
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
                }
                if (ypInT!=null && ypInB!=null) {
                    if (ypInB.length < ypInT.length)
                        ypInT = null;
                }

                pre.add(new UnifyPreFilter(xpInT, xpInB, ypInT, ypInB, varBits, strict));
            }
        }
    }


    @Override
    public boolean test(PreDerivation d) {
        Term x = xpInT != null ? d.taskTerm.subPath(xpInT) : d.beliefTerm.subPath(xpInB);
        assert (x != Bool.Null);
        if (x == null)
            return false; //ex: seeking a negation but wasnt negated
        Term y = ypInT != null ? d.taskTerm.subPath(ypInT) : d.beliefTerm.subPath(ypInB);
        assert (y != Bool.Null) : (ypInT != null ? d.taskTerm : d.beliefTerm) + " does not resolve "
                + Arrays.toString((ypInT != null ? ypInT : ypInB)) + " in " + d.taskTerm;
        if (y == null)
            return false; //ex: seeking a negation but wasnt negated

        return Terms.possiblyUnifiable( /*imageNormalize*/(x), /*imageNormalize*/(y), isStrict, varBits);
    }


    @Override
    public float cost() {
        return 0.3f;
    }


}
