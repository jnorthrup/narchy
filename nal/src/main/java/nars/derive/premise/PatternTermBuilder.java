package nars.derive.premise;

import nars.term.Compound;
import nars.term.Term;
import nars.term.compound.PatternCompound;
import nars.term.util.transform.AbstractTermTransform;
import nars.term.util.transform.Retemporalize;
import nars.term.var.ellipsis.Ellipsis;
import nars.term.var.ellipsis.Ellipsislike;
import org.jetbrains.annotations.Nullable;

import static nars.Op.NEG;
import static nars.term.var.ellipsis.Ellipsis.firstEllipsis;

/**
 * Index which specifically holds the target components of a deriver ruleset.
 */
public class PatternTermBuilder /* implements TermBuilder ? */ {


    public static Term patternify(Term x) {
        if (x instanceof Compound)
            return Ellipsify.applyCompound((Compound) x);
        return x;
    }


    public /*@NotNull*/ Term rule(Term x) {
        return patternify(new PremiseRuleNormalization().apply(x));
    }

//    public final PrediTerm<Derivation> intern(@Nullable PrediTerm<Derivation> x) {
//        if (x == null)
//            return null;
//        PrediTerm<Derivation> y = pred.putIfAbsent(x.target(), x);
//        return y != null ? y : x;
//    }

//
//    public final Termed intern(Term x) {
//        return get(x); //.target();
//    }

    private static final AbstractTermTransform.NegObliviousTermTransform Ellipsify = new AbstractTermTransform.NegObliviousTermTransform() {

        @Override
        protected @Nullable Term applyPosCompound(Compound x) {
            Term __x = Retemporalize.retemporalizeAllToXTERNAL.applyCompound(x);
            if (!(__x instanceof Compound))
                return __x;

            Term _x = super.applyPosCompound((Compound) __x);
            if (!(_x instanceof Compound)) {
                return _x;
            }

            x = (Compound) _x;

            Term xx;
            boolean neg = x.op() == NEG;
            if (neg)
                xx = x.unneg();
            else xx = x;

            @Nullable Ellipsislike e = firstEllipsis(xx.subterms());
            return (e != null ? PatternCompound.ellipsis((Compound) xx, xx.subterms(), (Ellipsis) e) : xx).negIf(neg);
        }
    };
}
