package nars.term.util.transform;

import nars.NAL;
import nars.Op;
import nars.subterm.Subterms;
import nars.subterm.TermList;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Bool;
import nars.term.compound.LazyCompoundBuilder;
import nars.term.util.TermException;
import org.jetbrains.annotations.Nullable;

import static nars.Op.*;
import static nars.term.atom.Bool.Null;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;

/**
 * I = input target type, T = transformable subterm type
 */
public interface AbstractTermTransform extends TermTransform, nars.term.util.builder.TermConstructor {

    static Term transform(Term x, AbstractTermTransform transform) {
        return transform(x, transform, new LazyCompoundBuilder(), NAL.term.COMPOUND_VOLUME_MAX);
    }

    /** global default transform procedure: can decide semi-optimal transform implementation */
    static Term transform(Term x, AbstractTermTransform transform, LazyCompoundBuilder l, int volMax) {
        if (x instanceof Compound && NAL.TERMIFY_TRANSFORM_LAZY && x.volume() > NAL.TERMIFY_TRANSFORM_LAZY_VOL_MIN ) {

            try {
                l.clear();
                return transform.applyCompoundLazy((Compound) x, l, volMax);
            } catch (TermException t) {
                if (NAL.DEBUG)
                    throw t;
                //continue below
            } catch (RuntimeException e) {
                throw new TermException(e.toString(), x);
                //return Null;
            }

        }

        return transform.apply(x);
    }

    /**
     * transform pathway for compounds
     */
    default Term applyCompound(Compound c) {
        return applyCompound(c, null, XTERNAL);
    }

    default Term applyCompound(Compound x, Op newOp, int newDT) {

        boolean sameOpAndDT = newOp == null;
        Op xop = x.op();


        Op targetOp = sameOpAndDT ? xop : newOp;
        Subterms xx = x.subterms(), yy;

        //try {
        yy = xx.transformSubs(this, targetOp);

//        } catch (StackOverflowError e) {
//            System.err.println("TermTransform stack overflow: " + this + " " + xx + " " + targetOp);
//        }

        if (yy == null)
            return Bool.Null;

        int thisDT = x.dt();
        if (yy == xx && (sameOpAndDT || (xop == targetOp && thisDT == newDT)))
            return x; //no change

        if (targetOp == CONJ) {
            if (yy == Op.FalseSubterm)
                return Bool.False;
            if (yy.subs() == 0)
                return Bool.True;
        }

        if (sameOpAndDT) {
            newDT = thisDT;

            //apply any shifts caused by range changes
            if (yy != xx && targetOp.temporal && newDT != DTERNAL && newDT != XTERNAL && xx.subs() == 2 && yy.subs() == 2) {

                int subjRangeBefore = xx.subEventRange(0);
                int predRangeBefore = xx.subEventRange(1);
                int subjRangeAfter = yy.subEventRange(0);
                int predRangeAfter = yy.subEventRange(1);
                newDT += (subjRangeBefore - subjRangeAfter) + (predRangeBefore - predRangeAfter);

            }
        }

        return appliedCompound(x, targetOp, newDT, xx, yy);
    }



    /**
     * called after subterms transform has been applied
     */
    @Nullable
    default Term appliedCompound(Compound x, Op op, int dt, Subterms xx, Subterms yy) {
        if (yy != xx) {
            Term[] a = yy instanceof TermList ? ((TermList) yy).arrayKeep() : yy.arrayShared();
            if (op == INH && a[1] instanceof InlineFunctor && evalInline() && a[0].op() == PROD) {
                Term v = ((InlineFunctor) a[1] /* pred */).applyInline(a[0] /* args */);
                if (v != null)
                    return v;
            }
            return compound(op, dt, a); //transformed subterms
        } else {
            //same subterms
            if (op == INH && evalInline()) {
                Term p = xx.sub(1), s;
                if (p instanceof InlineFunctor && (s = xx.sub(0)).op() == PROD) {
                    Term v = ((InlineFunctor) p /* pred */).applyInline(s /* subj = args */);
                    if (v != null)
                        return v;
                }
            }
//            if (op != x.op())
                return compound(op, dt, xx);
//            else
//                return x.dt(dt);
        }

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


    default Term applyCompoundLazy(Compound x) {
        return applyCompoundLazy(x, new LazyCompoundBuilder(), NAL.term.COMPOUND_VOLUME_MAX);
    }

    default Term applyCompoundLazy(Compound x, LazyCompoundBuilder l, int volMax) {
        l = transformCompound(x, l) ? l : null;
        return l == null ? Null : l.get(volMax);
    }


    /**
     * operates transparently through negation subterms
     */
    class NegObliviousTermTransform implements AbstractTermTransform {

        @Override
        public final Term applyCompound(Compound c) {

            if (c.op() == NEG) {
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
