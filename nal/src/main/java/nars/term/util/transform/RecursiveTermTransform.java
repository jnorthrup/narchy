package nars.term.util.transform;

import nars.Op;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Neg;
import nars.term.Term;
import nars.term.var.ellipsis.Ellipsislike;
import nars.term.var.ellipsis.Fragment;

import static nars.Op.*;
import static nars.term.atom.IdempotentBool.*;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;

/**
 * I = input target type, T = transformable subterm type
 */
public interface RecursiveTermTransform extends TermTransform, nars.term.util.builder.TermConstructor {

    static Term evalInhSubs(Subterms inhSubs) {
        Term p = inhSubs.sub(1); /* pred */
        if (p instanceof InlineFunctor) {
            Term s = inhSubs.sub(0);
            if (s instanceof Compound && s.opID() == PROD.id)
                return ((InlineFunctor) p).applyInline(((Compound)s).subtermsDirect());
        }
        return null;
    }

//    /**
//     * transform pathway for compounds
//     */
    default Term applyCompound(Compound x) {
        return applyCompound(x, null, XTERNAL);
    }

    default Term applyCompound(Compound x, Op newOp, int ydt) {
        RecursiveTermTransform f = this;

        Op xOp = x.op();
        Op yOp = newOp == null ? xOp : newOp;

        Subterms xx = x.subtermsDirect();

        Subterms yy = xx.transformSubs(f, yOp);
        if (yy == null)
            return Null;

        //inline reductions
        if (yOp == CONJ && xx!=yy) {
            int yys = yy.subs();
            if (yys == 0)
                return True;
            if (yy.containsInstance(False))
                return False; //short-circuit
            if (yys == 2) {
                if (yy.sub(0) == True)
                    return yy.sub(1);
                if (yy.sub(1) == True)
                    return yy.sub(0);

            }
        } else if (yOp == INH && f.evalInline() && yy.subs()==2) {
            //inline eval
            Term v = RecursiveTermTransform.evalInhSubs(yy);
            if (v != null)
                return v;
        }

        int xdt = x.dt();
        if (newOp == null)
            ydt = xdt;

        if (yOp.commutative) {
            int ys = yy.subs();
            if (ys == 1) {
                if (yOp == CONJ) {
                    Term y0 = yy.sub(0);
                    if (!(y0 instanceof Ellipsislike) && !(y0 instanceof Fragment))
                        return y0;
                }
            }
//            if (xdt == ydt && ydt != XTERNAL && dtSpecial(ydt)) {
//                int xs = x.subs();
//                if (ys == xs){
//                    //pre-sort because it may be identical
//                    Subterms ySorted = yy.commuted();
//                    int yss = ySorted.subs();
//                    if (yss == xs) {
//
//                        if (xx.equalTermsIdentical(ySorted))
//                            return x;
//
//                    } else {
//                        if (yss == 1 && yOp == SIM) {
//                            //similarity collapse to identity
//                            return True;
//                        }
//                    }
//                    yy = ySorted; //use the pre-sorted version since
//                    ys = yss;
//                }
//            }
        }

        if (yy == xx && xOp == yOp && xdt == ydt)
            return x; //no change


        if (yOp.temporal) {
            if (ydt != XTERNAL)
                ydt = RecursiveTermTransform.realign(ydt, xx, yy);

            if (ydt == 0)
                ydt = DTERNAL; //HACK
        }

        return f.compound(yOp, ydt, yy);
    }


////        try {
//            return c.transform(this, (Op) null, XTERNAL);
////        } catch (StackOverflowError e) {
////            //TEMPORARY
//////            e.printStackTrace();
////            System.err.println("stack overflow in AbstractTermTransform: " + this.getClass() + " " + c);
////            throw new WTF();
////        }
//    }

    static int realign(int ydt, Subterms xx, Subterms yy) {
        if (ydt == DTERNAL)
            ydt = 0; //HACK

        //apply any shifts caused by internal range changes (ex: True removal)
        if (!yy.equals(xx) && xx.subs() == 2 && yy.subs() == 2) {

            int subjRangeBefore = xx.subEventRange(0);
            int predRangeBefore = xx.subEventRange(1);
            int subjRangeAfter = yy.subEventRange(0);
            int predRangeAfter = yy.subEventRange(1);
            ydt += (subjRangeBefore - subjRangeAfter) + (predRangeBefore - predRangeAfter);

        }

        return ydt;
    }

    /**
     * enable for inline functor evaluation
     *
     * @param args
     */
    default boolean evalInline() {
        return false;
    }


    @Override
    default Term compound(Op op, int dt, Subterms t) {
        return op.the(dt, t);
    }

    @Override
    default Term compound(Op op, int dt, Term[] subterms) {
        return op.the(dt, subterms);
    }


    /**
     * operates transparently through negation subterms
     */
    class NegObliviousTermTransform implements RecursiveTermTransform {

        @Override
        public final Term applyCompound(Compound x) {

            if (x instanceof Neg) {
                Term xu = x.unneg();
                Term yu = apply(xu);
                return (yu == xu) ? x : yu.neg();
            } else {
                return applyPosCompound(x);
            }

        }

        public Term applyPosCompound(Compound c) {
            return RecursiveTermTransform.super.applyCompound(c);
        }


    }


}
