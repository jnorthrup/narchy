package nars.term.util;

import jcog.Util;
import nars.NAL;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Neg;
import nars.term.Term;
import nars.term.atom.theBool;
import nars.term.compound.LighterCompound;
import nars.term.util.builder.TermBuilder;
import org.jetbrains.annotations.Nullable;

import static nars.Op.*;
import static nars.term.atom.theBool.True;

/**
 * utilities for transforming image compound terms
 */
public enum Image {
    ;

    public static final int ImageBits = PROD.bit | IMG.bit | INH.bit;


    public static Term imageInt(Term t, Term x) {
        return image(true, t, x);
    }

    public static Term imageExt(Term t, Term x) {
        return image(false, t, x);
    }

    public static Term image(boolean intOrExt, Term t, Term x) {


        if (t instanceof Compound && t.opID() == INH.id) {

            var prodSub = intOrExt ? 1 : 0;

            var prod = t.sub(prodSub);

            if (prod instanceof Compound && prod.opID() == PROD.id) {

                var ss = ((Compound)prod).subtermsDirect();
                var n = ss.subs();
                if (n >= NAL.term.imageTransformSubMin /*&& (ss.structureSurface() & IMG.bit) == 0*/) {
                    var target = intOrExt ? ImgInt : ImgExt;

                    var index = ss.indexOf(x);
                    if (index != -1) {

                        var qq = new Term[n + 1];
                        qq[0] = t.sub(1 - prodSub);
                        for (var i = 0; i < n; i++) {
                            Term y;
                            if (i == index) {
                                y = target;
                                index = ss.indexOf(x, index);
                            } else
                                y = ss.sub(i);
                            qq[i + 1] = y;
                        }

                        var q = PROD.the(qq);
                        return INH.the(intOrExt ? new Term[] { q, x } : new Term[] { x, q });
                    }
                }
            }
        }
        return theBool.Null;
    }

    public static Term[] imageNormalize(Term[] u) {
        return Util.mapIfChanged(Image::imageNormalize, u);
    }

    public static Term imageNormalize(Term _x) {

        var neg = _x instanceof Neg;
        var x = neg ? _x.unneg() : _x;

        if (x instanceof Compound && x.hasAll(ImageBits) && x.opID() == INH.id) {
            var y = _imgNormalize((Compound) x);
            if (x != y)
                return y.negIf(neg);
        }

        return _x;
    }


    public static Term _imgNormalize(Compound x) {
        return normalize(x, true, false);
    }


    /** tests the term and its subterms recursively for an occurrence of a normalizeable image */
    public static boolean imageNormalizable(Subterms x) {
        return x.hasAll(Image.ImageBits) && x.OR(Image::imageSubtermNormalizable);
    }

    private static boolean imageSubtermNormalizable(Term x) {
        return
                !x.isNormalized()
                        ||
                        (x instanceof Compound &&
                            x.opID() == INH.id &&
                                x.hasAll(ImageBits) &&
                                normalize(x, false, false) == null);
    }


    @Deprecated public static Term normalize(Term x, boolean actuallyNormalize, boolean onlyRecursionTest) {
        return normalize(x, actuallyNormalize, onlyRecursionTest, Op.terms);
    }

    /**
     * assumes that input is INH op has been tested for all image bits
     */
    private static @Nullable Term normalize(Term x, boolean transform, boolean testOnly, TermBuilder B) {

        //assert(x.op()==INH);

        var xx = x.subterms();
        Term s = xx.sub(0), p = xx.sub(1);

        Subterms ss = null;
        var isInt = s instanceof Compound && s.opID() == PROD.id &&
            (ss = s.subterms()).containsInstance(Op.ImgInt) && !ss.containsInstance(Op.ImgExt);

        Subterms pp = null;
        var isExt = p instanceof Compound && p.opID() == PROD.id &&
            (pp = p.subterms()).containsInstance(Op.ImgExt) && !pp.containsInstance(Op.ImgInt);

        if (isInt == isExt)
            return x; //both or neither

        if (transform || testOnly) {

            Term subj, pred;
            if (isInt) {

                subj = ss.sub(0);
                if (testOnly && subj.op()!=PROD)
                    return null;
                pred = PROD.the(B, Util.replaceDirect(ss.subRangeArray(1, ss.subs()), Op.ImgInt, p));

            } else { //isExt

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
    public static @Nullable Term recursionFilter(Term subj, Term pred, TermBuilder B) {
        return Image.normalize(
                new LighterCompound(INH, subj, pred),
        true, true, B);
    }


}