package nars.derive.filter;

import nars.$;
import nars.Op;
import nars.derive.Derivation;
import nars.derive.premise.PatternIndex;
import nars.derive.premise.PremiseRuleSource;
import nars.op.SubIfUnify;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Terms;
import nars.term.Variable;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.control.AbstractPred;
import nars.term.control.PREDICATE;
import nars.term.util.Image;
import nars.unify.match.Ellipsislike;
import org.eclipse.collections.api.set.MutableSet;

import java.util.Collection;

import static nars.derive.Derivation.Belief;
import static nars.derive.Derivation.Task;

public class UnifyPreFilter extends AbstractPred<Derivation> {

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

    public static void tryAdd(Term x, Term y, Term taskPattern, Term beliefPattern, Subterms a, MutableSet<PREDICATE> pre) {
        //some structure exists that can be used to prefilter
        byte[] xpInT = Terms.constantPath(taskPattern, x);
        byte[] xpInB = Terms.constantPath(beliefPattern, x); //try the belief
        if (xpInT != null || xpInB != null) {
            byte[] ypInT = Terms.constantPath(taskPattern, y);
            byte[] ypInB = Terms.constantPath(beliefPattern, y); //try the belief
            if (ypInT != null || ypInB != null) {

                //the unifying terms are deterministicaly extractable from the task or belief:

                int varBits = (a.contains(SubIfUnify.DEP_VAR)) ? Op.VAR_DEP.bit : (Op.VAR_INDEP.bit | Op.VAR_DEP.bit);

                boolean strict = a.contains(SubIfUnify.STRICT);

                pre.add(new UnifyPreFilter(xpInT, xpInB, ypInT, ypInB, varBits, strict));
            }
        }
    }


    @Override
    public boolean test(Derivation d) {
        Term x = xpInT != null ? d.taskTerm.subPath(xpInT) : d.beliefTerm.subPath(xpInB);
        assert (x != Bool.Null);
        if (x == null)
            return false; //ex: seeking a negation but wasnt negated
        Term y = ypInT != null ? d.taskTerm.subPath(ypInT) : d.beliefTerm.subPath(ypInB);
        assert (y != Bool.Null) : (ypInT != null ? d.taskTerm : d.beliefTerm) + " does not resolve " + (ypInT != null ? ypInT : ypInB);
        if (y == null)
            return false; //ex: seeking a negation but wasnt negated

        return possibleUnification(x, y, varBits, 0);
    }

    public boolean possibleUnification(Term x, Term y, int varExcluded, int level) {
        boolean xEqY = x.equals(y);
        if (xEqY) {
            return level > 0 || !isStrict;
        }

        Op xo = x.op();
        if ((xo.bit & ~varExcluded) == 0)
            return true; //unifies, allow

        Op yo = y.op();
        if ((yo.bit & ~varExcluded) == 0)
            return true; //unifies, allow

        if (xo != yo)
            return false;


        x = Image.imageNormalize(x);
        y = Image.imageNormalize(y);

        Subterms xx = x.subterms(), yy = y.subterms();
        int xxs = xx.subs();
        if (xxs != yy.subs())
            return false;

        if (!Subterms.possiblyUnifiable(xx, yy, varExcluded))
            return false;

        if (xo.commutative)
            return true;

        //if (mustUnify(xx) || mustUnify(yy)) {
        //if (!xo.commutative) {

        int nextLevel = level + 1;
        for (int i = 0; i < xxs; i++) {
            Term x0 = xx.sub(i), y0 = yy.sub(i);
            if (!possibleUnification(x0, y0, varExcluded, nextLevel))
                return false;
        }
        return true;
    }


    @Override
    public float cost() {
        return 0.3f;
    }


}
