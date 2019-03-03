package nars.term.util.transform;

import nars.Op;
import nars.Param;
import nars.subterm.Subterms;
import nars.subterm.TermList;
import nars.term.Compound;
import nars.term.Functor;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.compound.LazyCompound;
import nars.term.util.builder.TermBuilder;
import org.jetbrains.annotations.Nullable;

import static nars.Op.*;
import static nars.term.atom.Bool.Null;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;

/**
 * I = input target type, T = transformable subterm type
 */
public interface AbstractTermTransform extends TermTransform {


    static Term applyBest(Term x, TermTransform transform) {
        if (Param.TERMIFY_TRANSFORM_LAZY && x instanceof Compound) {
            return ((AbstractTermTransform)transform).applyCompoundLazy((Compound)x,
                    //HeapTermBuilder.the
                    Op.terms
            );
        } else {
            return transform.apply(x);
        }
    }

    /**
     * transform pathway for atomics
     */
    default Term applyAtomic(Atomic atomic) {
        return atomic;
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


//    /** default lazy implementation, doesnt offer any benefit by just calling the non-lazy */
//    default boolean transformCompound(Compound x, LazyCompound out) {
//        Term y = transformCompound(x, x.op(), x.dt());
//        if (y == null)
//            return false;
//
//        out.addAt(y);
//        return true;
//    }


    /**
     * called after subterms transform has been applied
     */
    @Nullable
    default Term appliedCompound(Compound x, Op op, int dt, Subterms xx, Subterms yy) {
        if (yy != xx) {
            Term[] a = yy instanceof TermList ? ((TermList) yy).arrayKeep() : yy.arrayShared();
            if (op == INH && evalInline() && a[1] instanceof Functor.InlineFunctor && a[0].op() == PROD) {
                Term v = ((Functor.InlineFunctor) a[1] /* pred */).applyInline(a[0] /* args */);
                if (v != null)
                    return v;
            }
            return the(op, dt, a); //transformed subterms
        } else {
            //same subterms
            if (op == INH && evalInline()) {
                Term p = xx.sub(1), s;
                if (p instanceof Functor.InlineFunctor && (s = xx.sub(0)).op() == PROD) {
                    Term v = ((Functor.InlineFunctor) p /* pred */).applyInline(s /* subj = args */);
                    if (v != null)
                        return v;
                }
            }
//            if (op != x.op())
                return the(op, dt, xx);
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
    default Term the(Op op, int dt, Subterms t) {
        return op.the(dt, t);
    }

    default Term the(Op op, int dt, Term[] subterms) {
        return op.the(dt, subterms);
    }


    /**
     * HACK interface version
     */
    @Deprecated interface AbstractNegObliviousTermTransform extends AbstractTermTransform {

//        @Override
//        default boolean transformCompound(Compound x, LazyCompound out) {
//            if (x.op() == NEG) {
//                out.negStart();
//                return transform(x.unneg(), out);
//            } else {
//                return TermTransform.super.transformCompound(x, out);
//            }
//        }

        @Override
        @Nullable
        default Term applyCompound(Compound c) {

            if (c.op() == NEG) {
                Term xx = c.unneg();
                Term yy = apply(xx);
                return yy == xx ? c : yy.neg();

            } else {
                return AbstractTermTransform.super.applyCompound(c);
            }

        }

    }

    default LazyCompound applyLazy(Compound x) {
        return applyLazy(new LazyCompound(), x);
    }

    default LazyCompound applyLazy(LazyCompound l, Compound x) {
        return !transformCompound(x, l) ? null : l;
    }

    default Term applyCompoundLazy(Compound x) {
        return applyCompoundLazy(x, Op.terms);
    }

    default Term applyCompoundLazy(Compound x, TermBuilder b) {
        LazyCompound l = applyLazy(x);
        return l == null ? Null : l.get(b);
    }


    /**
     * operates transparently through negation subterms
     */
    class NegObliviousTermTransform implements AbstractTermTransform {

        @Override
        @Nullable
        public final Term applyCompound(Compound c) {

            if (c.op() == NEG) {
                Term xx = c.unneg();
                Term yy = apply(xx);
                return (yy==null || yy == xx) ? c : yy.neg();

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
