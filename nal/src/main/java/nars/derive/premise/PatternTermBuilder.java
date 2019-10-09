package nars.derive.premise;

import nars.term.Compound;
import nars.term.Neg;
import nars.term.Term;
import nars.term.compound.PatternCompound;
import nars.term.util.transform.RecursiveTermTransform;
import nars.term.util.transform.Retemporalize;
import nars.term.var.ellipsis.Ellipsis;
import nars.term.var.ellipsis.Ellipsislike;
import org.jetbrains.annotations.Nullable;

import static nars.term.var.ellipsis.Ellipsis.firstEllipsis;

/**
 * Index which specifically holds the target components of a deriver ruleset.
 */
public enum PatternTermBuilder /* implements TermBuilder ? */ { ;


    public static Term patternify(Term x) {
        return patternify(x, true);
    }

    public static Term patternify(Term x, boolean xternalize) {
        return x instanceof Compound ? (xternalize ? Patternify : PatternifyNoXternalize).applyCompound((Compound) x) : x;
    }


    @Deprecated public static /*@NotNull*/ Term rule(Term x) {
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

    private static final Patternify Patternify = new Patternify(true);
    private static final Patternify PatternifyNoXternalize = new Patternify(false);

    private static final class Patternify extends RecursiveTermTransform.NegObliviousTermTransform {

        final boolean xternalize;

        private Patternify(boolean xternalize) {
            this.xternalize = xternalize;
        }

        @Override
		@Nullable
		public Term applyPosCompound(Compound x) {
            if (xternalize) {
                x = (Compound) Retemporalize.retemporalizeAllToXTERNAL.applyCompound(x);
//                if (!(x instanceof Compound))
//                    return x;
            }

            Term _x = super.applyPosCompound(x);
            if (!(_x instanceof Compound)) {
                return _x;
            }

            x = (Compound) _x;

            Term xx;
            boolean neg = x instanceof Neg;
            if (neg)
                xx = x.unneg();
            else xx = x;

            @Nullable Ellipsislike e = xx instanceof Compound ? firstEllipsis(((Compound)xx).subtermsContainer()) : null;
            return (e != null ? PatternCompound.ellipsis((Compound) xx, xx.subterms(), (Ellipsis) e) : xx).negIf(neg);
        }
    }
}
