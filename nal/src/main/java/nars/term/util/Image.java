package nars.term.util;

import jcog.Util;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Bool;
import nars.term.compound.LighterCompound;
import nars.term.var.Img;
import org.jetbrains.annotations.Nullable;

import static nars.Op.*;
import static nars.term.atom.Bool.True;

/**
 * utilities for transforming image compound terms
 */
public enum Image {
    ;

    public static final int ImageBits = PROD.bit | Op.IMG.bit | INH.bit;


    public static Term imageInt(Term t, Term x) {
        return image(true, t, x);
    }

    public static Term imageExt(Term t, Term x) {
        return image(false, t, x);
    }

    public static Term image(boolean intOrExt, Term t, Term x) {


        if (t.op() == INH) {

            int prodSub = intOrExt ? 1 : 0;

            Term s = t.sub(prodSub);

            if (s.op() == PROD) {

                Subterms ss = s.subterms();
                int n = ss.subs();
                if (n >= 2 && (ss.structureSurface() & IMG.bit) == 0) {
                    Img target = intOrExt ? ImgInt : ImgExt;

                    int index = ss.indexOf(x);
                    if (index != -1) {

                        Term[] qq = new Term[n + 1];
                        qq[0] = t.sub(1 - prodSub);
                        for (int i = 0; i < n; i++) {
                            Term y;
                            if (i == index) {
                                y = target;
                                index = ss.indexOf(x, index);
                            } else
                                y = ss.sub(i);
                            qq[i + 1] = y;
                        }

                        Term q = PROD.the(qq);
                        return intOrExt ? INH.the(q, x) : INH.the(x, q);
                    }
                }
            }
        }
        return Bool.Null;
    }

    public static Term imageNormalize(Term x) {

        if (!(x instanceof Compound) || !x.hasAll(ImageBits))
            return x;

        Op xo = x.op();
        if (xo == NEG) {
            Term u = x.unneg();
            if (u instanceof Compound && u.op() == INH) {
                Term y = _imgNormalize((Compound) u).normalize();
                if (!y.equals(u))
                    return y.neg();
            }
        } else if (xo == INH) {
            return _imgNormalize((Compound) x).normalize();
        }

        return x;
    }


    public static Term _imgNormalize(Compound x) {
        return normalize(x, true, false);
    }


    public static boolean imageNormalizable(Subterms x) {
        return x.hasAll(Image.ImageBits) && !x.AND(Image::imageSubtermNormalizable);
    }

    private static boolean imageSubtermNormalizable(Term x) {
        return
                !x.isNormalized()
                        ||
                        (x instanceof Compound && x.op() == INH &&
                                x.hasAll(ImageBits) &&
                                normalize((Compound) x, false, false) == null);
    }


    /**
     * assumes that input is INH op has been tested for all image bits
     */
    @Nullable
    public static Term normalize(Term x, boolean actuallyNormalize, boolean onlyRecursionTest) {

        //assert(x.op()==INH);

        Subterms xx = x.subterms();
        Term s = xx.sub(0);
        Term p = xx.sub(1);

        Subterms ss = null;
        boolean isInt = s.op() == PROD && (ss = s.subterms()).contains(Op.ImgInt);// && !ss.contains(Op.ImgExt);

        Subterms pp = null;
        boolean isExt = p.op() == PROD && (pp = p.subterms()).contains(Op.ImgExt);// && !pp.contains(Op.ImgInt);


        boolean normalizable = isInt ^ isExt;

        if (!normalizable)
            return x;

        if (actuallyNormalize || onlyRecursionTest) {

            Term subj, pred;
            if (isInt) {

                subj = ss.sub(0);
                pred = PROD.the(Util.replaceDirect(ss.subRangeArray(1, ss.subs()), Op.ImgInt, p));

            } else {

                subj = PROD.the(Util.replaceDirect(pp.subRangeArray(1, pp.subs()), Op.ImgExt, s));
                pred = pp.sub(0);

            }

            if (onlyRecursionTest) {
                if (subj.equals(pred))
                    return True;
                else
                    return null;
            }

            return imageNormalize(INH.the(subj, pred));

        } else {
            return null;
        }


    }


    /**
     * prior to creating new inheritance terms - determine if there is a cyclic
     * dependency between subj and pred components that would cause the Image
     * to collapse if normalized -- but without doing a full normalization.
     * <p>
     * ex: (ANIMAL-->((cat,ANIMAL),cat,/))
     *
     * @return Bool term if collapsed, otherwise null
     */
    @Nullable
    public static Term recursionFilter(Term subj, Term pred) {
        return Image.normalize(
                new LighterCompound(INH, subj, pred),
        true, true);
    }
}