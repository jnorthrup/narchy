package nars.util.term.transform;

import jcog.Texts;
import nars.Op;
import nars.subterm.Subterms;
import nars.subterm.util.TermList;
import nars.term.Compound;
import nars.term.Evaluation;
import nars.term.Term;
import nars.term.Termed;
import nars.term.var.UnnormalizedVariable;
import org.jetbrains.annotations.Nullable;

import static nars.Op.*;
import static nars.time.Tense.DTERNAL;

/**
 * I = input term type, T = transformable subterm type
 */
public interface TermTransform extends Evaluation.TermContext {
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

        Subterms yy = xx.transformSubs(this);
        if (yy == null)
            return Null; //return null;

        return transformedCompound(x, op, dt, xx, yy);
    }

    /** called after subterms transform has been applied */
    @Nullable default Term transformedCompound(Compound x, Op op, int dt, Subterms xx, Subterms yy) {
        if (yy != xx || op != x.op()) {
            return the(op, op.temporal ? dt : DTERNAL, (TermList)yy);
        } else {
            return x.dt(dt);
        }
    }




    /**
     * constructs a new term for a result
     */
    default Term the(Op op, int dt, TermList t) {
        //return op.a(
        //optimized impl for TermList (FasterList)
        return op.compound(dt, t.arrayShared());
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
            public Term transformAtomic(Term atomic) {
                if (atomic.op() != from)
                    return atomic;
                else
                    return new UnnormalizedVariable(to, Texts.quote(atomic.toString()));
            }
        };
    }


//    TermTransform anyVarToQueryVar = new TermTransform() {
//        @Override
//        public Term transformAtomic(Term atomic) {
//            Op a = atomic.op();
//            return (a.var && a!= VAR_QUERY) ?
//                    $.varQuery((((NormalizedVariable) atomic).anonNum())) :
//                    atomic;
//        }
//    };
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
