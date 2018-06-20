package nars.term.anon;

import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.atom.Int;
import nars.term.compound.util.AnonMap;
import nars.term.var.UnnormalizedVariable;
import nars.util.term.transform.DirectTermTransform;
import nars.util.term.transform.TermTransform;
import org.jetbrains.annotations.Nullable;

/**
 * term anonymization context, for canonicalization and generification of compounds
 */
public class Anon extends AnonMap {

    private final TermTransform PUT, GET;

    public Anon() {
        this(1);
    }

    public Anon(int estSize) {
        super(estSize);
        this.PUT = newPut();
        this.GET = newGet();
    }

    public int uniques() {
        return idToTerm.size();
    }

    /** returns true if anything changed */
    public boolean rollback(int uniques) {
        if (uniques == 0) {
            clear();
            return true;
        }

        int max;
        if (uniques < (max = uniques())) {
            for (int i = uniques; i < max; i++)
                termToId.removeKey(idToTerm.get(i));
            idToTerm.removeAbove(uniques);
            return true;
        }
        return false;
    }

    TermTransform newPut() {

        return new DirectTermTransform() {
            @Override
            public final @Nullable Term transformAtomic(Atomic atomic) {
                return put(atomic);
            }
            @Override
            public boolean eval() {
                return false;
            }
        };
    }

    TermTransform newGet() {

        return new TermTransform.NegObliviousTermTransform() {
            @Override
            public final @Nullable Term transformAtomic(Atomic atomic) {
                return get(atomic);
            }
        };
    }


    public Term put(Term x) {
        if (x instanceof AnonID) {
            return x;
        } else if (x instanceof Atomic) {

            if (x instanceof UnnormalizedVariable || x instanceof Int.IntRange)
                return x;

            return Anom.the[intern(x)];

        } else {
            return PUT.transformCompound((Compound)x);
        }
    }

    public Term get(Term x) {
        if (x instanceof Anom) {
            return interned((byte) ((Int) x).id);
        } else if (x instanceof Compound) {
            return GET.transformCompound((Compound) x);
        } else {
            return x; 
        }
    }




}
