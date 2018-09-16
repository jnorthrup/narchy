package nars.term.util;

import jcog.Util;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Functor;
import nars.term.Term;
import nars.term.atom.Bool;
import org.apache.commons.lang3.ArrayUtils;

import static nars.Op.*;

/** utilities for transforming image compound terms */
public enum Image { ;

    static final int imageBits = PROD.bit | Op.VAR_DEP.bit | INH.bit;

    public static final Functor imageNormalize = Functor.f1Inline("imageNormalize", Image::imageNormalize);
    public static final Functor imageInt = Functor.f2Inline("imageInt", Image::imageInt);
    public static final Functor imageExt = Functor.f2Inline("imageExt", Image::imageExt);

    static Term imageExt(Term t, Term x) {
        
        
        if (t.op()==INH) {
            Term p = t.sub(0);
            if (p.op()==PROD && !imaged(p)) {
                Term r = p.replace(x, Op.ImgExt);
                if (r!=p) {
                    Term i = t.sub(1);
                    return INH.the(x, PROD.the(ArrayUtils.prepend(i, r.subterms().arrayShared(), Term[]::new)));
                }
            }
        }
        return Bool.Null;
    }

    private static boolean imaged(Term p) {
        return p.hasAny(Op.VAR_DEP) && p.OR(x -> (x == Op.ImgInt || x == Op.ImgExt));
    }

    private static Term imageInt(Term t, Term x) {
        
        
        if (t.op()==INH) {
            Term p = t.sub(1);
            if (p.op()==PROD && !imaged(p)) {
                Term r = p.replace(x, Op.ImgInt);
                if (r!=p) {
                    Term i = t.sub(0);
                    return INH.the(PROD.the(ArrayUtils.prepend(i, r.subterms().arrayShared(), Term[]::new)), x);
                }
            }
        }
        return Bool.Null;
    }

    public static Term imageNormalize(Term _t) {
        if (!(_t instanceof Compound) || !_t.hasAll(imageBits))
            return _t;

        boolean negated;
        Term t;
        if (_t.op()==NEG) {
            t = _t.unneg();
            negated = true;
        } else {
            t = _t;
            negated = false;
        }

        if (t.op()==INH && t.hasAll(imageBits)) {
            Term s = t.sub(0);
            Subterms ss = null;
            boolean isInt = s.op()==PROD && (ss = s.subterms()).contains(Op.ImgInt);// && !ss.contains(Op.ImgExt);

            Term p = t.sub(1);
            Subterms pp = null;
            boolean isExt = p.op()==PROD && (pp = p.subterms()).contains(Op.ImgExt);// && !pp.contains(Op.ImgInt);

            if (isInt && !isExt) {
                
                
                Term u = INH.the(ss.sub(0), PROD.the(Util.replaceDirect(ss.subRangeArray(1, ss.subs()), Op.ImgInt, p)));
                if (!(u instanceof Bool))
                    return u.negIf(negated);
            } else if (isExt && !isInt) {
                
                
                Term u = INH.the(PROD.the(Util.replaceDirect(pp.subRangeArray(1, pp.subs()), Op.ImgExt, s)), pp.sub(0));
                if (!(u instanceof Bool))
                    return u.negIf(negated);
            }
        }

        return _t;
    }

}
