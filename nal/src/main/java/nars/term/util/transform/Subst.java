package nars.term.util.transform;

import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import org.jetbrains.annotations.Nullable;


public interface Subst extends AbstractTermTransform {


//    /** completely dereferences a target (usually a variable)*/
//    default Term resolve(final Term x) {
//        Term y, z = x;
//        while ((y = xy(z))!=null) {
//            z = y;
//        }
//        return z;
//    }

    @Override
    default Term applyAtomic(Atomic x) {
        Term y = xy(x);
        return y != null ? y : x;
        //return resolve(atomic);
    }

    @Override
    @Nullable
    default Term applyCompound(Compound x) {
        Term y = xy(x);
        if (y!=null && y!=x) {
            return y instanceof Compound && y.equals(x) ? y : apply(y);
        } else
            return AbstractTermTransform.super.applyCompound(x);

//        if (y == null || y == x) {
//            return AbstractTermTransform.super.applyCompound(x);
//        } else
//            return y;
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
     * the assigned value for x
     */
    @Nullable Term xy(Term t);


}
