package nars.term.transform;

import nars.$;
import nars.Op;
import nars.derive.match.EllipsisMatch;
import nars.index.term.TermContext;
import nars.subterm.Subterms;
import nars.subterm.util.DisposableTermList;
import nars.subterm.util.TermList;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.var.NormalizedVariable;
import org.jetbrains.annotations.Nullable;

import static nars.Op.NEG;
import static nars.Op.VAR_QUERY;
import static nars.time.Tense.DTERNAL;

/**
 * I = input term type, T = transformable subterm type
 */
public interface TermTransform extends TermContext {
    /**
     * general pathway. generally should not be overridden
     */
    @Override
    default @Nullable Termed apply(Term x) {
        return x instanceof Compound ? transformCompound((Compound) x) : transformAtomic(x);
    }

    /**
     * transform pathway for atomics
     */
    default @Nullable Termed transformAtomic(Term atomic) {
        assert (!(atomic instanceof Compound));
        return atomic;
    }

    /**
     * transform pathway for compounds
     */
    default Term transformCompound(Compound x) {
        return transformCompound(x, x.op(), x.dt());
    }

    /**
     * should not be called directly except by implementations of TermTransform
     */
    @Nullable
    default Term transformCompound(Compound x, Op op, int dt) {

        Subterms xx = x.subterms();

        Subterms yy = transformSubterms(xx);

        if (yy == null) {
            return null;
        } else if (yy != xx || op != x.op()) {


            Term z = the(op, op.temporal ? dt : DTERNAL, (TermList)yy);


//            if (op==x.op() && Arrays.equals(xx.arrayShared(),z.subterms().arrayShared())) {
//                System.err.println("duplicated unnecessarily? ");
//            }

//            //seems to happen very infrequently so probably not worth the test
            if (x != z && x.equals(z))
                return x; //unchanged

            return z;
        } else {
            return x.dt(dt);
        }
    }

    /**
     * returns 'x' unchanged if no changes were applied,
     * returns 'y' if changes
     * returns null if untransformable
     */
    default Subterms transformSubterms(Subterms x) {

        int s = x.subs();

        TermList y = null;

        for (int i = 0; i < s; i++) {

            Term xi = x.sub(i);

            Term yi = xi.transform(this);

            if (yi == null)
                return null;

            if (yi instanceof EllipsisMatch) {
                EllipsisMatch xe = (EllipsisMatch) yi;
                int xes = xe.subs();

                if (y == null) {
                    y = new DisposableTermList(s - 1 + xes /*estimate */); //create anyway because this will signal if it was just empty
                    if (i > 0) {
                        y.addAll(x, 0, i); //add previously skipped subterms
                    }
                }

                if (xes > 0) {
                    for (int j = 0; j < xes; j++) {
                        @Nullable Term k = xe.sub(j).transform(this);
                        if (k==null) {
                            return null;
                        } else {
                            y.add(k);
                        }
                    }
                }

            } else {

                if (xi != yi /*&& (yi.getClass() != xi.getClass() || !xi.equals(yi))*/) {

//                    if (xi.equals(yi)) {
//                        System.err.println("duplicated unnecessarily? ");
//                        xi.printRecursive();
//                        yi.printRecursive();
//                        System.out.println();
//                    }

//                    if (yi == null) {
//                        return null;
//                    }

                    if (y == null) {
                        y = new DisposableTermList(s);
                        if (i > 0) y.addAll(x, 0, i); //add previously skipped subterms
                    }
                }

                if (y != null)
                    y.add(yi);

            }

        }

        return y != null ? y : x;
    }


    /**
     * constructs a new term for a result
     */
    default Term the(Op op, int dt, TermList t) {
        return op.the(
        //return op.a(
                dt,
                t.arrayShared() //optimized impl for TermList (FasterList)
                );
    }


    /**
     * change all query variables to dep vars by use of Op.imdex
     */
    TermTransform queryToDepVar = new TermTransform() {
        @Override public Term transformAtomic(Term atomic) {
            return atomic.op() != VAR_QUERY ?
                    atomic
                    :
                    Op.Imdex;
        }
    };

    TermTransform anyVarToQueryVar = new TermTransform() {
        @Override
        public Term transformAtomic(Term atomic) {
            Op a = atomic.op();
            return (a.var && a!= VAR_QUERY) ?
                    $.varQuery((((NormalizedVariable) atomic).anonNum())) :
                    atomic;
        }
    };
    /**
     * operates transparently through negation subterms
     */
    interface NegObliviousTermTransform extends TermTransform {

        @Override
        @Nullable
        default Term transformCompound(Compound x) {
            Op op = x.op();
            if (op == NEG) {
                Term xx = x.unneg();
                Termed y = apply(xx);
                if (y == null)
                    return null;
                Term yy = y.term();
                if (yy.equals(xx))
                    return x; //no change
                else {
                    Term y2 = yy.neg(); //negate the transformed subterm
                    if (y2.equals(x))
                        return x;
                    else
                        return y2;
                }
            } else {
                return transformCompoundUnneg(x);
            }

        }

        /** transforms a compound that has been un-negged */
        @Nullable default Term transformCompoundUnneg(Compound x) {
            return TermTransform.super.transformCompound(x);
        }
    }

}
