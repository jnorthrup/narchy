package nars.term.util;

import jcog.Util;
import nars.Op;
import nars.Task;
import nars.subterm.Subterms;
import nars.task.proxy.ImageTask;
import nars.term.Compound;
import nars.term.Img;
import nars.term.Neg;
import nars.term.Term;
import nars.term.atom.Bool;
import nars.term.compound.LighterCompound;
import nars.term.util.builder.TermBuilder;
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

    public static Term[] imageNormalize(Term[] u) {
        return Util.mapIfChanged(Image::imageNormalize, u);
    }

    public static Term imageNormalize(Term _x) {

        boolean neg = _x instanceof Neg;
        Term x = neg ? _x.unneg() : _x;

        if (!(x instanceof Compound) || !x.hasAll(ImageBits))
            return _x;

        if (x.op() == INH) {
            Term y = _imgNormalize((Compound) x);//.normalize();
            if (x!=y)
                return y.negIf(neg);
        }

        return _x; //unchanged
    }


    public static Term _imgNormalize(Compound x) {
        return normalize(x, true, false);
    }


    /** tests the term and its subterms recursively for an occurrence of a normalizeable image */
    public static boolean imageNormalizable(Subterms x) {
        return x.hasAll(Image.ImageBits)
                 && x.OR(Image::imageSubtermNormalizable);
    }

    private static boolean imageSubtermNormalizable(Term x) {
        return
                !x.isNormalized()
                        ||
                        (x instanceof Compound && x.op() == INH &&
                                x.hasAll(ImageBits) &&
                                normalize(x, false, false) == null);
    }


    @Deprecated public static Term normalize(Term x, boolean actuallyNormalize, boolean onlyRecursionTest) {
        return normalize(x, actuallyNormalize, onlyRecursionTest, Op.terms);
    }

    /**
     * assumes that input is INH op has been tested for all image bits
     */
    @Nullable
    private static Term normalize(Term x, boolean transform, boolean testOnly, TermBuilder B) {

        //assert(x.op()==INH);

        Subterms xx = x.subterms();
        Term s = xx.sub(0), p = xx.sub(1);

        Subterms ss = null;
        boolean isInt = s.op() == PROD && (ss = s.subterms()).contains(Op.ImgInt);// && !ss.contains(Op.ImgExt);

        Subterms pp = null;
        boolean isExt = p.op() == PROD && (pp = p.subterms()).contains(Op.ImgExt);// && !pp.contains(Op.ImgInt);

        if (isInt == isExt)
            return x;

        if (transform || testOnly) {

            Term subj, pred;
            if (isInt) {

                subj = ss.sub(0);
                if (testOnly && subj.op()!=PROD)
                    return null;
                pred = PROD.the(B, Util.replaceDirect(ss.subRangeArray(1, ss.subs()), Op.ImgInt, p));

            } else {

                pred = pp.sub(0);
                if (testOnly && pred.op()!=PROD)
                    return null;
                subj = PROD.the(B, Util.replaceDirect(pp.subRangeArray(1, pp.subs()), Op.ImgExt, s));

            }

            if (testOnly)
                return subj.equals(pred) ? True : null;
            else
                return imageNormalize(INH.the(B, subj, pred));

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
    public static Term recursionFilter(Term subj, Term pred, TermBuilder B) {
        return Image.normalize(
                new LighterCompound(INH, subj, pred),
        true, true, B);
    }

    public static Task imageNormalizeTask(Task task) {
        if (task instanceof ImageTask)
            return ((ImageTask)task).task;
        else
            return task;
    }

    public static Term imageNormalizeTerm(Task task) {
        if (task instanceof ImageTask) {
            return ((ImageTask)task).task.term();
        } else
            return task.term();
    }
}