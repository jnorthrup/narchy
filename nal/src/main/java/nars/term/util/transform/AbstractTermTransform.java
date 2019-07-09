package nars.term.util.transform;

import nars.Op;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Neg;
import nars.term.Term;

import static nars.Op.PROD;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;

/**
 * I = input target type, T = transformable subterm type
 */
public interface AbstractTermTransform extends TermTransform, nars.term.util.builder.TermConstructor {

    static Term evalInhSubs(Subterms inhSubs) {
        Term p = inhSubs.sub(1); /* pred */
        if (p instanceof InlineFunctor) {
            Term s = inhSubs.sub(0);
            if (s.op() == PROD) {
                Term v = ((InlineFunctor) p).applyInline(s.subterms());
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
//        try {
            return c.transform(this, (Op) null, XTERNAL);
//        } catch (StackOverflowError e) {
//            //TEMPORARY
////            e.printStackTrace();
//            System.err.println("stack overflow in AbstractTermTransform: " + this.getClass() + " " + c);
//            throw new WTF();
//        }
    }

    static int realign(int ydt, Subterms xx, Subterms yy) {
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


        protected Term applyPosCompound(Compound x) {
            return AbstractTermTransform.super.applyCompound(x);
        }

    }


}
