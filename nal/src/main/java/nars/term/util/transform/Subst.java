package nars.term.util.transform;

import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import org.jetbrains.annotations.Nullable;

import static nars.Op.NEG;


public interface Subst extends TermTransform {


    default Term resolveIfNeg(final Term x) {
        boolean neg = x.op()==NEG;
        Term x0 = neg ? x.unneg() : x;
        Term y = resolve(x0);
        if (neg) {
            if (y!=x0)
                return y.neg();
            else
                return x;
        } else
            return y;
    }

    /** completely dereferences a term (usually a variable)*/
    default Term resolve(final Term x) {

        Term y, z = x;
        while ((y = xy(z))!=null) {

            //assert(y!=z && y!=x);
//            if (y == z || y == x) //TEMPORARY
//                throw new WTF("loop in xy map: " + this);

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

    @Override @Nullable
    default Term transformCompound(Compound x) {
        Term y = xy(x);
        if (y==null || y==x) {
            return TermTransform.super.transformCompound((Compound) x);
        } else {
            return transform(y);
        }
    }


    /**
     * can be used to determine if this subst will have any possible effect on any transforms to any possible term,
     * used as a quick test to prevent transform initializations
     */
    boolean isEmpty();

    /**
     * the assigned value for x
     */
    @Nullable Term xy(Term t);




}
