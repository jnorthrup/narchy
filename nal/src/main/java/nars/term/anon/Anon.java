package nars.term.anon;

import jcog.WTF;
import nars.Param;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.atom.Int;
import nars.term.util.ByteAnonMap;
import nars.term.util.transform.TermTransform;
import nars.term.var.UnnormalizedVariable;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * term anonymization context, for canonicalization and generification of compounds
 * //return new DirectTermTransform() {
 * //return new TermTransform.NegObliviousTermTransform() {
 */
public class Anon extends ByteAnonMap implements TermTransform.NegObliviousTermTransform {

    private boolean putOrGet = true;

    public Anon() {
        this(1);
    }

    public Anon(int estSize) {
        super(estSize);
    }

    public int uniques() {
        return idToTerm.size();
    }

    /**
     * returns true if anything changed
     */
    public boolean rollback(int toUniques) {
        if (toUniques == 0) {
            clear();
            return true;
        }

        int max;
        if (toUniques < (max = uniques())) {
            for (int i = toUniques; i < max; i++)
                termToId.removeKey(idToTerm.get(i));
            idToTerm.removeAbove(toUniques);
            return true;
        }
        return false;
    }

    @Override
    public final @Nullable Term transformAtomic(Atomic atomic) {
        return putOrGet ? put(atomic) : get(atomic);
    }

    public final Term put(Term x) {

        if (x instanceof Atomic) {

            if (x instanceof AnonID)
                return x;

            if (x instanceof UnnormalizedVariable) {
                return x; //HACK
            }

            return Anom.the[intern(x)];

        } else {
            return putCompound((Compound) x);
        }
    }


    private static final AtomicBoolean validateLock = new AtomicBoolean();

    void validate(Term x, Term y, boolean putOrGet) {

        if (Param.DEBUG) {
            if (!validateLock.compareAndSet(false, true))
                return;
            try {

//            if (termToId.isEmpty() || idToTerm.isEmpty())
//                throw new WTF("termToId is empty: " + x + " -> " + y);

                if (y.op() != x.op())
                    throw new WTF("anon changed op: " + x + " -> " + y);
                if (y.volume() != x.volume())
                    throw new WTF("anon changed vol: " + x + " -> " + y + " <- " + get(y));


//            if (putOrGet) {
//                Term z = get(y);
//                if (!z.equals(x)) {
//                    /* temporary for debug: */ get(y);
//                    throw new WTF("invalid put:\n\t" + x + "\n\t" + y + "\n\t" + z);
//                }
//            } else {
//                Term z = put(y);
//                if (!z.equals(x))
//                    throw new WTF("invalid get:\n\t" + x + "\n\t" + y + "\n\t" + z);
//
//            }
            } finally {
                validateLock.set(false);
            }
        }
    }


    public final Term get(Term x) {
        if (x instanceof Compound) {
            return getCompound((Compound) x);
        } else {
            if (x instanceof Anom) {
                return interned((byte) ((Int) x).id);
            } else {
                return x;
            }
        }
    }

    protected final Term getCompound(Compound y) {
        putOrGet = false;
        Term x = transformCompound(y);
//        validate(y, x, false);
        return x;
    }

    protected final Term putCompound(Compound x) {
        putOrGet = true;

        Term y = transformCompound(x);

//        validate(x, y, true);
        return y;
    }
}
