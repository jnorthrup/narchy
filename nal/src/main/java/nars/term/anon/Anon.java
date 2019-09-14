package nars.term.anon;

import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.atom.Interval;
import nars.term.util.map.ByteAnonMap;
import nars.term.util.transform.AbstractTermTransform;
import nars.term.var.UnnormalizedVariable;
import org.jetbrains.annotations.Nullable;

/**
 * target anonymization context, for canonicalization and generification of compounds
 * //return new DirectTermTransform() {
 * //return new TermTransform.NegObliviousTermTransform() {
 */
public class Anon extends AbstractTermTransform.NegObliviousTermTransform {

    public final ByteAnonMap map;


    @Override
    public String toString() {
        return map.toString();
    }


    protected boolean putOrGet = true;

    public Anon() {
        this(1);
    }


    Anon(int estSize) {
        this.map = new ByteAnonMap(estSize);
    }

    public int uniques() {
        return map.termCount();
    }



    @Override
    public final @Nullable Term applyAtomic(Atomic atomic) {
        return putOrGet ? putAtomic(atomic) : getAtomic(atomic);
    }

    public final Term put(Term x) {
        if (x instanceof Compound) {
            return putCompound((Compound) x);
        } else {
            return putAtomic((Atomic)x);
        }
    }

    /** determines what Atomics are considered intrinsic (and thus not internable) */
    public boolean intrin(Atomic x) {
        return Intrin.intrin(x);
    }

    final Term putAtomic(Atomic x) {
        if (x instanceof UnnormalizedVariable || x instanceof Interval /* HACK */) {
            return x; //HACK is this necessary?
        }

        if (intrin(x))
            return putIntrin(x);
        else if (intern(x))
            return putIntern(x);
        else
            return x; //uninterned
    }

    private Anom putIntern(Term x) {
        return Anom.the(map.intern(x));
    }

    /** default implementation: anonymize atoms, but also could be a Compound -> Atom anonymize */
    protected boolean intern(Atomic x) {
        return true;
    }

    /** anon filter in which subclasses can implement variable shifting */
    Term putIntrin(Term x) {
        return x;
    }

    public final Term get(Term x) {
        if (x instanceof Compound) {
//            switch (map.termCount()) {
//                case 1:
//                    //optimized
//                    return x.replace(Anom.the(1), map.interned((byte)1));
//                default:
            return getCompound((Compound) x);
//            }
        } else {
            return getAtomic((Atomic)x);
        }
    }

    final Term getAtomic(Atomic x) {
        return x instanceof Anom ? map.interned(((Anom) x).id()) : x;
    }

    protected Term getCompound(Compound x) {
        putOrGet = false;
        return applyCompound(x);

//      Term y = transformCompoundLazily((Compound)x);
////      if (!y.equals(y0)) {
////          transformCompoundLazily((Compound)x);  throw new WTF(); //TEMPORARY
////      }
//      return y;
    }

    protected Term putCompound(Compound x) {
        putOrGet = true;
        return applyCompound(x);

        //Term x1 = applyCompoundLazy(x);
////        if (!x0.equals(x1)) {
////           transformCompoundLazily((Compound)x);  throw new WTF(); //TEMPORARY
////        }
        //return x1;
    }

    void clear() {
        map.clear();
    }

}

