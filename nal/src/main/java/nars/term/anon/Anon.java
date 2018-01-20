package nars.term.anon;

import jcog.list.FasterList;
import nars.Op;
import nars.Task;
import nars.The;
import nars.subterm.Subterms;
import nars.subterm.TermList;
import nars.subterm.TermVector;
import nars.task.TaskProxy;
import nars.term.Compound;
import nars.term.CompoundDTLight;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.atom.Int;
import nars.term.transform.TermTransform;
import org.eclipse.collections.api.block.function.primitive.ByteFunction;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectByteHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static nars.term.anon.Anom.MAX_ANOM;
import static nars.time.Tense.DTERNAL;

/**
 * term anonymization context, for canonicalization and generification of compounds
 */
public class Anon {


    final ObjectByteHashMap<Term> fwd;
    final List<Term> rev = new FasterList<>(0);
    private final TermTransform PUT, GET;

    public Anon() {
        this(1);
    }

    public Anon(int estSize) {
        fwd = new ObjectByteHashMap(estSize);
        this.PUT = newPut();
        this.GET = newGet();
    }


    final ByteFunction<Term> nextUniqueAtom = (Term next) -> {
        int s = rev.size() + 1;
        assert (s < MAX_ANOM);
        //assert (!(next instanceof Bool));
        rev.add(next);
        return (byte) s;
    };

    static class DirectTermTransform implements TermTransform {
        @Override
        public Term the(Op op, int dt, TermList t) {
            Term x = The.rawCompoundBuilder.apply(op, t.arraySharedSafe());
            if (dt!=DTERNAL && x.op().temporal) {
                return new CompoundDTLight((Compound)x, dt);
            } else {
                return x;
            }
        }
    }

    protected TermTransform newPut() {
        return new DirectTermTransform() {
            @Override
            public final @Nullable Termed transformAtomic(Term atomic) {
                return put(atomic);
            }
        };
    }

    protected TermTransform newGet() {
        return new DirectTermTransform() {
            @Override
            public final @Nullable Termed transformAtomic(Term atomic) {
                return get(atomic);
            }
        };
    }

    public void clear() {
        fwd.clear();
        rev.clear();
    }

    public Term put(Term x) {
        if (x instanceof AnonID) {
            //assert (!(x instanceof UnnormalizedVariable));
//            if (x instanceof UnnormalizedVariable)
//                throw new RuntimeException("unnormalized variable for Anon: " + x);
            return x; //ignore normalized variables since they are AnonID
        } else if (x instanceof Atomic) {
            if (x instanceof Int.IntRange)
                return x; //HACK
            return Anom.the[fwd.getIfAbsentPutWithKey(x, nextUniqueAtom)];
        } else {
            return PUT.transformCompound((Compound)x);
        }
    }

    public Term get(Term x) {
        if (x instanceof Anom) {
            return rev.get(((Anom) x).id - 1); //assume it is an int
        } else if (x instanceof Compound) {
            return GET.transformCompound((Compound) x);
        } else {
            return x; //ignore variables, ints
        }
    }

    public Task put(Task t) {
        Term x = t.term();
        assert (x.isNormalized()) : t + " has non-normalized Term content";
        Term y = put(x);
        if (y == null || y instanceof Bool) {
            throw new RuntimeException("Anon fail for term: " + t);
        }
        if (y instanceof Compound) {
            Subterms yy = y.subterms();
            if (yy instanceof TermVector)
                ((TermVector) yy).setNormalized();
        }

        return new TaskProxy.WithTerm(y, t);
    }


}
