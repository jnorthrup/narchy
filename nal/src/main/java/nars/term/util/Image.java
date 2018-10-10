package nars.term.util;

import jcog.Util;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Functor;
import nars.term.Term;
import nars.term.atom.Bool;
import nars.term.var.ImDep;
import org.apache.commons.lang3.ArrayUtils;

import static nars.Op.*;

/** utilities for transforming image compound terms */
public enum Image { ;

    public static final int ImageBits = PROD.bit | Op.IMG.bit | INH.bit;

    public static final Functor imageNormalize = Functor.f1Inline("imageNormalize", Image::imageNormalize);
    public static final Functor imageInt = Functor.f2Inline("imageInt", Image::imageInt);
    public static final Functor imageExt = Functor.f2Inline("imageExt", Image::imageExt);



//    private static boolean imaged(Term p) {
//        return p.hasAny(Op.IMG) && p.OR(x -> (x == Op.ImgInt || x == Op.ImgExt));
//    }

    public static Term imageInt(Term t, Term x) {
        return image(true, t, x);
    }
    public static Term imageExt(Term t, Term x) {
        return image(false, t, x);
    }
    public static Term image(boolean intOrExt, Term t, Term x) {


        if (t.op()==INH) {

            int prodSub = intOrExt ? 1 : 0;

            Term s = t.sub(prodSub);

            if (s.op()==PROD) {

                ImDep target = intOrExt ? ImgInt : ImgExt;

                Subterms ss = s.subterms();
                if (!ss.contains(target)) {

                    int index = ss.indexOf(x);
                    if (index != -1) {
                        Term[] qq = ArrayUtils.prepend(t.sub(1 - prodSub), ss.arrayShared(), Term[]::new);

                        do {

                            qq[index + 1] = target;
                            index = ss.indexOf(x, index);
                        } while (index != -1);

                        Term q = PROD.the(qq);
                        return intOrExt ? INH.the(q, x) : INH.the(x, q);
                    }
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

        if (o==INH /*&& t.hasAll(ImageBits)*/) {
            Term s = t.sub(0);
            Subterms ss = null;
            boolean isInt = s.op()==PROD && (ss = s.subterms()).contains(Op.ImgInt);// && !ss.contains(Op.ImgExt);

            Term p = t.sub(1);
            Subterms pp = null;
            boolean isExt = p.op()==PROD && (pp = p.subterms()).contains(Op.ImgExt);// && !pp.contains(Op.ImgInt);

            Term u;
            if (isInt && !isExt) {

                u = INH.the(ss.sub(0), PROD.the(Util.replaceDirect(ss.subRangeArray(1, ss.subs()), Op.ImgInt, p)));

            } else if (isExt && !isInt) {

                u = INH.the(PROD.the(Util.replaceDirect(pp.subRangeArray(1, pp.subs()), Op.ImgExt, s)), pp.sub(0));

            } else {
                return z;
            }

            if (!(u instanceof Bool))
                return Image.imageNormalize(u).negIf(negated);

        }

        return z;

    }

}
