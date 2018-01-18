package nars.term.anon;

import jcog.list.FasterList;
import nars.Task;
import nars.task.TaskProxy;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.atom.Int;
import nars.subterm.Subterms;
import nars.subterm.TermVector;
import nars.term.transform.TermTransform;
import org.eclipse.collections.api.block.function.primitive.ByteFunction;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectByteHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static nars.term.anon.Anom.MAX_ANOM;

/**
 * term anonymization context, for canonicalization and generification of compounds
 */
public class Anon {


    final ObjectByteHashMap<Term> fwd;
    final List<Term> rev = new FasterList<>(0);

    public Anon() {
        fwd = new ObjectByteHashMap();
    }

    public Anon(int estSize) {
        fwd = new ObjectByteHashMap(estSize);
    }

    final ByteFunction<Term> nextUniqueAtom = (Term next) -> {
        int s = rev.size()+1;
        assert (s < MAX_ANOM);
        //assert (!(next instanceof Bool));
        rev.add(next);
        return (byte) s;
    };

    final TermTransform PUT = new /*Direct*/TermTransform() {

        @Override
        public final @Nullable Termed transform(Term nonCompound) {
            return put(nonCompound);
        }
    };

    final TermTransform GET = new /*Direct*/TermTransform() {

        @Override
        public final @Nullable Termed transform(Term nonCompound) {
            return get(nonCompound);
        }
    };

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
            return x.transform(PUT);
        }
    }

    public Term get(Term x) {
        if (x instanceof Anom) {
            return rev.get(((Anom) x).id-1); //assume it is an int
        } else if (x instanceof Atomic) {
            return x; //ignore variables, ints
        } else {
            return x.transform(GET);
        }
    }

    public Task put(Task t) {
        Term x = t.term();
        assert(x.isNormalized()): t + " has non-normalized Term content";
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
