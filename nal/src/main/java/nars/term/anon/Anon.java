package nars.term.anon;

import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atomic;
import nars.term.atom.Int;
import nars.term.compound.util.AnonMap;
import nars.term.var.UnnormalizedVariable;
import nars.util.term.transform.DirectTermTransform;
import nars.util.term.transform.TermTransform;
import org.eclipse.collections.api.block.function.primitive.ByteFunction;
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

    final ByteFunction<Term> nextUniqueAtom = (Term next) -> {
        int s = idToTerm.addAndGetSize(next);
        assert (s < Byte.MAX_VALUE);
        return (byte) s;
    };

    protected TermTransform newPut() {

        return new DirectTermTransform() {
            @Override
            public final @Nullable Termed transformAtomic(Term atomic) {
                return put(atomic);
            }
        };
    }

    protected TermTransform newGet() {

        return new TermTransform() {
            @Override
            public final @Nullable Termed transformAtomic(Term atomic) {
                return get(atomic);
            }
        };
    }


    public Term put(Term x) {
        if (x instanceof AnonID) {



            return x;
        } else if (x instanceof Atomic) {

            if (x instanceof UnnormalizedVariable) return x;
            if (x instanceof Int.IntRange) return x;

            return Anom.the[termToId.getIfAbsentPutWithKey(x, nextUniqueAtom)];

        } else {
            return PUT.transformCompound((Compound)x);
        }
    }

    public Term get(Term x) {
        if (x instanceof Anom) {
            return idToTerm.get(((Int) x).id - 1);
        } else if (x instanceof Compound) {
            return GET.transformCompound((Compound) x);
        } else {
            return x; 
        }
    }




}
