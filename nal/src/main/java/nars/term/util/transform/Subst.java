package nars.term.util.transform;

import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import org.jetbrains.annotations.Nullable;

import static nars.term.atom.Bool.Null;


@FunctionalInterface
public interface Subst extends RecursiveTermTransform {

    /**
     * the assigned value for x
     */
    @Nullable Term xy(Term t);

    @Override
    default Term applyAtomic(Atomic x) {
        var y = xy(x);
        return y != null ? y : x;
    }

    @Override
    default @Nullable Term applyCompound(Compound x) {
        Term y = RecursiveTermTransform.super.applyCompound(x);
        if (y == Null)
            return Null;
        var z = xy(y);
        return (z!=null) ? z : y;

//        Term y = xy(x);
//        if (y!=null && y!=x) {
//            return y instanceof Compound && y.equals(x) ? y : apply(y);
//        } else
//            return AbstractTermTransform.super.applyCompound(x);

//        if (y == null || y == x) {
//            return AbstractTermTransform.super.applyCompound(x);
//        } else
//            return y;
    }


}
