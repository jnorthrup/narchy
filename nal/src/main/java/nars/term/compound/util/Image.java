package nars.term.compound.util;

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

    public static final Functor imageNormalize = Functor.f1("imageNormalize", Image::imageNormalize);
    public static final Functor imageInt = Functor.f2("imageInt", Image::imageInt);
    public static final Functor imageExt = Functor.f2("imageExt", Image::imageExt);

    public static Term imageExt(Term t, Term x) {
        //P-->I
        //int: (X --> append(I,substitute(P,X,/)))
        if (t.op()==INH) {
            Term p = t.sub(0);
            if (p.op()==PROD) {
                Term r = p.replace(x, Op.imExt);
                if (r!=p) {
                    Term i = t.sub(1);
                    return INH.the(x, PROD.the(ArrayUtils.prepend(i, r.subterms().arrayShared(), Term[]::new)));
                }
            }
        }
        return Null;
    }

    public static Term imageInt(Term t, Term x) {
        //I-->P
        //ext: (append(I,substitute(P,X,\)) --> X)
        if (t.op()==INH) {
            Term p = t.sub(1);
            if (p.op()==PROD) {
                Term r = p.replace(x, Op.imInt);
                if (r!=p) {
                    Term i = t.sub(0);
                    return INH.the(PROD.the(ArrayUtils.prepend(i, r.subterms().arrayShared(), Term[]::new)), x);
                }
            }
        }
        return Null;
    }

    public static Term imageNormalize(Term _t) {
        if (!(_t instanceof Compound))
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

        if (t.hasAll(imageBits) && t.op()==INH) {
            Term s = t.sub(0);
            Subterms ss = null;
            boolean isInt = s.op()==PROD && (ss = s.subterms()).contains(Op.imInt);
            Term p = t.sub(1);

            Subterms pp = null;
            boolean isExt = p.op()==PROD && (pp = p.subterms()).contains(Op.imExt);

            if (isInt && !isExt) {
                //(neutralization --> (acid,base))
                //((neutralization,\\,base) --> acid)
                Term u = INH.the(ss.sub(0), PROD.the(Util.replaceDirect(ss.toArraySubRange(1, ss.subs()), Op.imInt, p)));
                if (!(u instanceof Bool))
                    return u.negIf(negated);
            } else if (isExt && !isInt) {
                //((acid,base)-->reaction)
                //(acid --> (reaction,/,base))
                Term u = INH.the(PROD.the(Util.replaceDirect(pp.toArraySubRange(1, pp.subs()), Op.imExt, s)), pp.sub(0));
                if (!(u instanceof Bool))
                    return u.negIf(negated);
            }
        }

        return _t;
    }

}
