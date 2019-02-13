package nars.term.util.transform;

import nars.Op;
import nars.subterm.Subterms;
import nars.subterm.TermList;
import nars.term.Compound;
import nars.term.Functor;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.compound.LazyCompound;
import org.jetbrains.annotations.Nullable;

import static nars.Op.*;
import static nars.term.atom.Bool.Null;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;

/**
 * I = input target type, T = transformable subterm type
 */
public interface TermTransform {

    default Term transform(Term x) {
        return (x instanceof Compound) ?
                transformCompound((Compound) x)
                :
                transformAtomic((Atomic) x);
    }

    default boolean transform(Term x, LazyCompound out) {
        if (x instanceof Compound) {
            return transformCompound((Compound) x, out);
        } else {
            @Nullable Term y = transformAtomic((Atomic) x);
            if (y == null || y == Bool.Null)
                return false;
            else {
//                if (y instanceof EllipsisMatch) {
//                    int ys = y.subs();
//                    out.subsAdd(ys - 1);
//                    if (ys > 0) {
//                        if (!transformSubterms((EllipsisMatch) y, out))
//                            return false;
//                    }
//                    out.setChanged(true);
//                } else {
                    out.add(y);
                    if (y != x)
                        out.setChanged(true);
//                }
                return true;
            }
        }
    }

    /**
     * transform pathway for atomics
     */
    default Term transformAtomic(Atomic atomic) {
        return atomic;
    }

    /**
     * transform pathway for compounds
     */
    default Term transformCompound(Compound x) {
        return transformCompound(x, null, XTERNAL);
    }

    default Term transformCompound(Compound x, Op newOp, int newDT) {

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

        return transformedCompound(x, targetOp, newDT, xx, yy);
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

    default boolean transformCompound(Compound x, LazyCompound out) {
        boolean c = out.changed();
        int i = out.pos();

        Op o = x.op();
        if (o == NEG) {
            out.negStart();
            if (!transform(x.sub(0), out))
                return false;
        } else {
            out.compoundStart(o, o.temporal ? x.dt() : DTERNAL);

            Subterms s = x.subterms();
            out.subs((byte) s.subs());
            if (!transformSubterms(s, out))
                return false;
        }

        //out.compoundEnd(o); //??

        if (!c && !out.changed()) {
            //remains same; rewind and paste as-is
            out.rewind(i);
            out.add(x);
        }
        return true;
    }

    default boolean transformSubterms(Subterms s, LazyCompound out) {
        if (s.ANDwithOrdered(this::transform, out)) {
            out.subsEnd();
            return true;
        }
        return false;
    }

    /**
     * called after subterms transform has been applied
     */
    @Nullable
    default Term transformedCompound(Compound x, Op op, int dt, Subterms xx, Subterms yy) {
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
            if (op != x.op())
                return the(op, dt, xx);
            else
                return x.dt(dt);
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
        return the(op, dt, t.arrayShared());
    }

    default Term the(Op op, int dt, Term[] subterms) {
        return op.the(dt, subterms);
    }


    /**
     * HACK interface version
     */
    interface AbstractNegObliviousTermTransform extends TermTransform {

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
        default Term transformCompound(Compound x) {

            if (x.op() == NEG) {
                Term xx = x.unneg();
                Term yy = transform(xx);
                return yy == xx ? x : yy.neg();

            } else {
                return TermTransform.super.transformCompound(x);
            }

        }

    }

    default Term transformCompoundLazily(Compound x) {
        LazyCompound l = new LazyCompound.LazyEvalCompound();
        if (!transformCompound(x, l))
            return Null;
        else
            return l.get();
    }


    /**
     * operates transparently through negation subterms
     */
    class NegObliviousTermTransform implements TermTransform {

        @Override
        @Nullable
        public final Term transformCompound(Compound x) {

            if (x.op() == NEG) {
                Term xx = x.unneg();
                Term yy = transform(xx);
                return yy == xx ? x : yy.neg();

            } else {
                return transformNonNegCompound(x);
            }

        }

        /**
         * default implementation
         */
        protected Term transformNonNegCompound(Compound x) {
            return TermTransform.super.transformCompound(x);
        }

    }

}
