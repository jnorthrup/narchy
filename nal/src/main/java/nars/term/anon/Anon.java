package nars.term.anon;

import jcog.data.list.FasterList;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.util.map.ByteAnonMap;
import nars.term.util.transform.AbstractTermTransform;
import nars.term.var.UnnormalizedVariable;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectByteHashMap;
import org.jetbrains.annotations.Nullable;

/**
 * target anonymization context, for canonicalization and generification of compounds
 * //return new DirectTermTransform() {
 * //return new TermTransform.NegObliviousTermTransform() {
 */
public class Anon extends AbstractTermTransform.NegObliviousTermTransform {

//    final static TermBuilder localBuilder =
//            new InterningTermBuilder(Anon.class.getSimpleName(), 8*1024);
            //HeapTermBuilder.the;

    private final ByteAnonMap map;


    @Override
    public String toString() {
        return map.toString();
    }


    boolean putOrGet = true;

    public Anon() {
        this(1);
    }

    Anon(int estSize) {
        this.map = new ByteAnonMap(estSize);
    }

    public int uniques() {
        return map.termCount();
    }

    /**
     * returns true if anything changed
     */
    boolean rollback(int toUniques) {
        if (toUniques == 0) {
            clear();
            return true;
        }

        int max;
        if (toUniques < (max = uniques())) {
            ObjectByteHashMap<Term> termToId = map.termToId;
            FasterList<Term> idToTerm = map.idToTerm;
            for (int i = toUniques; i < max; i++)
                termToId.removeKey(idToTerm.get(i));
            idToTerm.removeAbove(toUniques);
            return true;
        }
        return false;
    }

    @Override
    public final @Nullable Term applyAtomic(Atomic atomic) {
        return putOrGet ? put(atomic) : get(atomic);
    }

    public final Term put(Term x) {

        if (x instanceof Atomic) {

            if (x instanceof AnonID) {
                return putAnon(x);
            }

            if (x instanceof UnnormalizedVariable)
                return x; //HACK

            return Anom.the[map.intern(x)];

        } else {
            return putCompound((Compound) x);
        }
    }

    /** anon filter in which subclasses can implement variable shifting */
    Term putAnon(Term x) {
        return x;
    }




    public final Term get(Term x) {
        if (x instanceof Compound) {
//            switch (map.termCount()) {
//                case 1:
//                    //optimized
//                    return x.replace(Anom.the(1), map.interned((byte)1));
//                default:
                    putOrGet = false;

                    Term y0 = applyCompound((Compound) x);

//                    Term y = transformCompoundLazily((Compound)x);
//                    if (!y.equals(y0)) {
//                        transformCompoundLazily((Compound)x);  throw new WTF(); //TEMPORARY
//                    }
//                    return y;
                    return y0;
//            }
        } else {
            if (x instanceof Anom) {
                return map.interned((byte) ((AnonID) x).anonID);
            }
        }
        return x;
    }

    private Term putCompound(Compound x) {
        putOrGet = true;
//        Term x0 = transformCompound(x);
        Term x1 = applyCompoundLazy(x);
//        if (!x0.equals(x1)) {
//            transformCompoundLazily((Compound)x);  throw new WTF(); //TEMPORARY
//        }
//        return x0;
        return x1;
    }

    void clear() {
        map.clear();
    }

}
//    private static final AtomicBoolean validateLock = new AtomicBoolean();
//
//    void validate(Term x, Term y, boolean putOrGet) {
//
//        if (Param.DEBUG) {
//            if (!validateLock.compareAndSet(false, true))
//                return;
//            try {
//
////            if (termToId.isEmpty() || idToTerm.isEmpty())
////                throw new WTF("termToId is empty: " + x + " -> " + y);
//
//                if (y.op() != x.op())
//                    throw new WTF("anon changed op: " + x + " -> " + y);
//                if (y.volume() != x.volume())
//                    throw new WTF("anon changed vol: " + x + " -> " + y + " <- " + get(y));
//
//
////            if (putOrGet) {
////                Term z = get(y);
////                if (!z.equals(x)) {
////                    /* temporary for debug: */ get(y);
////                    throw new WTF("invalid put:\n\t" + x + "\n\t" + y + "\n\t" + z);
////                }
////            } else {
////                Term z = put(y);
////                if (!z.equals(x))
////                    throw new WTF("invalid get:\n\t" + x + "\n\t" + y + "\n\t" + z);
////
////            }
//            } finally {
//                validateLock.setAt(false);
//            }
//        }
//    }
