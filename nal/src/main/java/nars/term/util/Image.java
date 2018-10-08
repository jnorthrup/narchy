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

    public static final int ImageBits = PROD.bit | Op.IMG.bit | INH.bit;

    public static final Functor imageNormalize = Functor.f1Inline("imageNormalize", Image::imageNormalize);
    public static final Functor imageInt = Functor.f2Inline("imageInt", Image::imageInt);
    public static final Functor imageExt = Functor.f2Inline("imageExt", Image::imageExt);

    public static Term imageExt(Term t, Term x) {
        
        
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
        return p.hasAny(Op.IMG) && p.OR(x -> (x == Op.ImgInt || x == Op.ImgExt));
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
        if (!(_t instanceof Compound) || !_t.hasAll(ImageBits))
            return _t;

        return _imageNormalize(_t);
    }


    public static Term _imageNormalize(Term z) {
        boolean negated;

        Term t;
        Op o = z.op();
        if (o==NEG) {
            negated = true;
            t = z.unneg();
            o = t.op();
        } else {
            t = z;
            negated = false;
        }

        if (o==INH && t.hasAll(ImageBits)) {
            Term s = t.sub(0);
            Subterms ss = null;
            boolean isInt = s.op()==PROD && (ss = s.subterms()).contains(Op.ImgInt);// && !ss.contains(Op.ImgExt);

            Term p = t.sub(1);
            Subterms pp = null;
            boolean isExt = p.op()==PROD && (pp = p.subterms()).contains(Op.ImgExt);// && !pp.contains(Op.ImgInt);

            if (isInt && !isExt) {


                Term u = INH.the(ss.sub(0), PROD.the(Util.replaceDirect(ss.subRangeArray(1, ss.subs()), Op.ImgInt, p)));
                if (!(u instanceof Bool))
                    return Image.imageNormalize(u).negIf(negated);
            } else if (isExt && !isInt) {


                Term u = INH.the(PROD.the(Util.replaceDirect(pp.subRangeArray(1, pp.subs()), Op.ImgExt, s)), pp.sub(0));
                if (!(u instanceof Bool))
                    return Image.imageNormalize(u).negIf(negated);
            }
        }

        return z;
    }

}
