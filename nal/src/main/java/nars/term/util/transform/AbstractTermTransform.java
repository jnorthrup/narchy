package nars.term.util.transform;

import nars.Op;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Neg;
import nars.term.Term;
import nars.term.atom.Bool;

import static nars.Op.*;
import static nars.term.atom.Bool.Null;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;

/**
 * I = input target type, T = transformable subterm type
 */
public interface AbstractTermTransform extends TermTransform, nars.term.util.builder.TermConstructor {

    private static Term eval(Subterms yy) {
        Term p = yy.sub(1);
        if (p instanceof InlineFunctor) {
            Term s = yy.sub(0);
            if (s.op() == PROD) {
                Term v = ((InlineFunctor) p /* pred */).applyInline(s /* args */);
                if (v != null)
                    return v;
            }
        }
        return null;
    }

    /**
     * transform pathway for compounds
     */
    default Term applyCompound(Compound c) {
        return applyCompound(c, null, XTERNAL);
    }

    default Term applyCompound(Compound x, Op newOp, int ydt) {

        boolean sameOpAndDT = newOp == null;
        Op xop = x.op();


        Op yOp = sameOpAndDT ? xop : newOp;
        Subterms xx = x.subterms();

        Subterms yy = xx.transformSubs(this, yOp);

        if (yy == null)
            return Null;

        int xdt = x.dt();
        if (yy == xx && (sameOpAndDT || (xop == yOp && xdt == ydt)))
            return x; //no change

        if (yOp == CONJ) {
            if (yy == Op.FalseSubterm)
                return Bool.False;
            if (yy.subs() == 0)
                return Bool.True;
        }

        if (yOp == INH && evalInline()) {
            Term v = eval(yy);
            if (v != null)
                return v;
        }




        if (yOp.temporal) {
            if (sameOpAndDT)
                ydt = xdt;

            if (ydt != XTERNAL)
                ydt = realign(ydt, xx, yy);

        } else
            ydt = DTERNAL;


        if (yy != xx) {
            //transformed subterms
            return compound(yOp, ydt, yy);
        } else {
            if (yOp == xop) {
                //same op and same subterms, maybe different dt
                return xdt != ydt ? x.dt(ydt) : x;
            } else {
                //same subterms, different op
                return compound(yOp, ydt, xx);
            }
        }

    }

    private static int realign(int ydt, Subterms xx, Subterms yy) {
        if (ydt == DTERNAL)
            ydt = 0; //HACK

        //apply any shifts caused by internal range changes
        if (!yy.equals(xx) && xx.subs() == 2 && yy.subs() == 2) {

            int subjRangeBefore = xx.subEventRange(0);
            int predRangeBefore = xx.subEventRange(1);
            int subjRangeAfter = yy.subEventRange(0);
            int predRangeAfter = yy.subEventRange(1);
            ydt += (subjRangeBefore - subjRangeAfter) + (predRangeBefore - predRangeAfter);

        }

        if (ydt == 0) ydt = DTERNAL; //HACK
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


    /**
     * constructs a new target for a result
     */
//    default Term the(Op op, int dt, TermList t) {
//        return the(op, dt, (Subterms)t);
//    }
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
    class NegObliviousTermTransform implements AbstractTermTransform {

        @Override
        public final Term applyCompound(Compound c) {

            if (c instanceof Neg) {
                Term xx = c.unneg();
                Term yy = apply(xx);
                return (yy == xx) ? c : yy.neg();
            } else {
                return applyPosCompound(c);
            }

        }

        /**
         * default implementation
         */
        protected Term applyPosCompound(Compound x) {
            return AbstractTermTransform.super.applyCompound(x);
        }

    }

    abstract class FilteredTermTransform extends NegObliviousTermTransform {

        @Override
        protected final Term applyPosCompound(Compound x) {
            return preFilter(x) ? applyFilteredPosCompound(x) : x;
        }

        protected Term applyFilteredPosCompound(Compound x) {
            return x;
        }

        abstract public boolean preFilter(Compound x);
    }

}
