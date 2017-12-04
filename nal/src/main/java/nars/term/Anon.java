package nars.term;

import jcog.list.FasterList;
import nars.term.atom.Atomic;
import nars.term.atom.Int;
import nars.term.transform.CompoundTransform;
import nars.term.var.UnnormalizedVariable;
import nars.term.var.Variable;
import org.eclipse.collections.api.block.function.primitive.ByteFunction;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectByteHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** term anonymization context, for canonicalization and generification of compounds */
public class Anon  {

    final ObjectByteHashMap<Term> fwd = new ObjectByteHashMap();
    final List<Term> rev = new FasterList<>();

    final ByteFunction<Term> nextUniqueAtom = (Term next) -> {
        int s = rev.size();
        assert(s<127);
        rev.add(next);
        return (byte) s;
    };

    final CompoundTransform PUT = new CompoundTransform() {
        @Override public @Nullable Termed apply(Term t) {
            return put(t);
        }
        @Override
        public @Nullable Term applyTermOrNull(Term t) {
            return put(t); //may be called more directly
        }
    };
    
    final CompoundTransform get = new CompoundTransform() {
        @Override public @Nullable Termed apply(Term t) {
            return get(t);
        }

        @Override
        public @Nullable Term applyTermOrNull(Term t) {
            return get(t); //may be called more directly
        }
    };

    public void clear() {
        fwd.clear();
        rev.clear();
    }

    public Term put(Term x) {
        if (x instanceof Variable) {
            assert(!(x instanceof UnnormalizedVariable));
            return x; //ignore normalized variables
        } else if (x instanceof Atomic) {
            return Int.the(fwd.getIfAbsentPutWithKey(x, nextUniqueAtom));
        } else {
            return x.transform(PUT);
        }
    }

    public Term get(Term x) {
        if (x instanceof Variable) {
            return x; //ignore variables
        } else if (x instanceof Atomic) {
            return rev.get(((Int)x).id); //assume it is an int
        } else {
            return x.transform(get);
        }
    }

}
