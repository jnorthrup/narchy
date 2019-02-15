package nars.term.util.transform;

import nars.term.Term;
import nars.term.atom.Atomic;
import org.jetbrains.annotations.Nullable;


public interface Subst extends TermTransform.AbstractNegObliviousTermTransform {



    /** completely dereferences a target (usually a variable)*/
    default Term resolve(final Term x) {
        Term y, z = x;
        while ((y = xy(z))!=null) {
            z = y;
        }
        return z;
    }

    @Override @Nullable
    default Term transformAtomic(Atomic atomic) {
//        Term y = xy(atomic);
//        return y != null ? y : atomic;
        return resolve(atomic);
    }


//    @Override @Nullable
//    default Term transformCompound(Compound x) {
//        Term y = xy(x);
//        if (y==null || y==x) {
//            return TermTransform.super.transformCompound((Compound) x);
//        } else {
//            return transform(y);
//        }
//    }


    /**
     * can be used to determine if this subst will have any possible effect on any transforms to any possible target,
     * used as a quick test to prevent transform initializations
     */
    boolean isEmpty();

    /**
     * the assigned value for x
     */
    @Nullable Term xy(Term t);




}
