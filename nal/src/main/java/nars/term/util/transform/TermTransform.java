package nars.term.util.transform;

import jcog.Texts;
import nars.Op;
import nars.subterm.Subterms;
import nars.subterm.TermList;
import nars.term.Compound;
import nars.term.Functor;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.compound.LazyCompound;
import nars.term.var.UnnormalizedVariable;
import org.jetbrains.annotations.Nullable;

import static nars.Op.*;
import static nars.time.Tense.XTERNAL;

/**
 * I = input term type, T = transformable subterm type
 */
public interface TermTransform {

    default Term transform(Term x) {
        return (x instanceof Compound) ?
                transformCompound((Compound)x)
                :
                transformAtomic((Atomic)x);
    }

    default boolean transform(Term x, LazyCompound out) {
        if (x instanceof Compound) {
            return transformCompound((Compound)x, out);
        } else {
            @Nullable Term y = transformAtomic((Atomic) x);
            if (y == null || y == Bool.Null)
                return false;
            out.add(y);
            return true;
        }
    }

    /**
     * transform pathway for atomics
     */
    default @Nullable Term transformAtomic(Atomic atomic) {
        return atomic;
    }

    /**
     * transform pathway for compounds
     */
    default Term transformCompound(Compound x) {
        return transformCompound(x, null, XTERNAL);
    }

    /**
     * should not be called directly except by implementations of TermTransform
     */
    @Nullable
    default Term transformCompound(final Compound x, Op op, int dt) {

        boolean sameOpAndDT = op == null;
        Op xop = x.op();


        Op targetOp = sameOpAndDT ? xop : op;
        Subterms xx = x.subterms(), yy;

        //try {
            yy = xx.transformSubs(this, targetOp);

//        } catch (StackOverflowError e) {
//            System.err.println("TermTransform stack overflow: " + this + " " + xx + " " + targetOp);
//        }

        if (yy == null)
            return Bool.Null;

        if (yy == xx && (sameOpAndDT || (xop == targetOp && x.dt() == dt)))
            return x; //no change

        if (targetOp == CONJ) {
            if (yy == Op.FalseSubterm)
                return Bool.False;
            if (yy.subs() == 0)
                return Bool.True;
        }

        if (sameOpAndDT)
            dt = x.dt();

        return transformedCompound(x, targetOp, dt, xx, yy);
    }


//    /** default lazy implementation, doesnt offer any benefit by just calling the non-lazy */
//    default boolean transformCompound(Compound x, LazyCompound out) {
//        Term y = transformCompound(x, x.op(), x.dt());
//        if (y == null)
//            return false;
//
//        out.add(y);
//        return true;
//    }

    default boolean transformCompound(Compound x, LazyCompound out) {
        out.compound(x.op(), x.dt());
        return transformSubterms(x.subterms(), out);
    }

    default boolean transformSubterms(Subterms x, LazyCompound out) {
        out.subs((byte)x.subs());
        return x.AND(sub -> transform(sub, out));
    }

    /** called after subterms transform has been applied */
    @Nullable default Term transformedCompound(Compound x, Op op, int dt, Subterms xx, Subterms yy) {
        Term y;
        if (yy != xx) {
            Term[] a = yy instanceof TermList ? ((TermList) yy).arrayKeep() : yy.arrayShared();
            if (op == INH && evalInline() && a[1] instanceof Functor.InlineFunctor && a[0].op()==PROD) {
                Term v = ((Functor.InlineFunctor) a[1] /* pred */).applyInline(a[0] /* args */);
                if (v != null)
                    return v;
            }
            return the(op, dt, a); //transformed subterms
        } else  {
            //same subterms
            if (op == INH && evalInline() && xx.sub(1) instanceof Functor.InlineFunctor && xx.sub(0).op()==PROD) {
                Term v = ((Functor.InlineFunctor) xx.sub(1) /* pred */).applyInline(xx.sub(0) /* args */);
                if (v != null)
                    return v;
            }
            if (op != x.op())
                return the(op, dt, xx);
            else
                return x.dt(dt);
        }

    }

    /** enable for inline functor evaluation
     * @param args*/
    default boolean evalInline() {
        return false;
    }



    /**
     * constructs a new term for a result
     */
//    default Term the(Op op, int dt, TermList t) {
//        return the(op, dt, (Subterms)t);
//    }

    default Term the(Op op, int dt, Subterms t) {
        return the(op, dt, t.arrayShared());
    }

    default Term the(Op op, int dt, Term[] subterms) {
        return op.the(dt, subterms);
    }
    /**
     * change all query variables to dep vars by use of Op.imdex
     */
    TermTransform queryToDepVar = variableTransform(VAR_QUERY, VAR_DEP);
    TermTransform indepToDepVar = variableTransform(VAR_INDEP, VAR_DEP);

    private static TermTransform
    variableTransform(Op from, Op to) {
        return new TermTransform() {
            @Override
            public Term transformAtomic(Atomic atomic) {
                if (atomic.op() != from)
                    return atomic;
                else
                    return new UnnormalizedVariable(to, Texts.quote(atomic.toString()));
            }
        };
    }











    /**
     * operates transparently through negation subterms
     */
    class NegObliviousTermTransform implements TermTransform {

        @Override
        @Nullable
        public final Term transformCompound(Compound x) {

            if (x.op()==NEG) {
                Term xx = x.unneg();
                Term yy = transform(xx);
                if (yy == null || yy == Bool.Null)
                    return Bool.Null;

                return yy == xx ? x : yy.neg();

            } else {
                return transformNonNegCompound(x);
            }

        }

        /** default implementation */
        protected Term transformNonNegCompound(Compound x) {
            return TermTransform.super.transformCompound(x);
        }

        /** HACK */
        protected final Term transformCompoundPlease(Compound x) {
            return transformCompound(x, null, XTERNAL);
        }

    }

}
