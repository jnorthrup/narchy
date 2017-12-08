package nars.term.anon;

import jcog.Util;
import jcog.list.FasterList;
import jcog.memoize.LinkedMRUMemoize;
import nars.Op;
import nars.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atomic;
import nars.term.atom.Bool;
import nars.term.compound.CachedCompound;
import nars.term.container.Subterms;
import nars.term.container.TermVector;
import nars.term.transform.CompoundTransform;
import nars.term.var.UnnormalizedVariable;
import nars.term.var.Variable;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.api.block.function.primitive.ByteFunction;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectByteHashMap;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static nars.term.anon.Anom.MAX_ANOM;

/**
 * term anonymization context, for canonicalization and generification of compounds
 */
public class Anon {


    final ObjectByteHashMap<Term> fwd = new ObjectByteHashMap();
    final List<Term> rev = new FasterList<>();

    final ByteFunction<Term> nextUniqueAtom = (Term next) -> {
        int s = rev.size();
        assert (s < MAX_ANOM);
        assert (!(next instanceof Bool));
        rev.add(next);
        return (byte) s;
    };

    ;

    final CompoundTransform PUT = new DirectCompoundTransform() {

//        @Override
//        protected @Nullable Term transformDirect(Op o, Term[] yy) {
//            if (AnomVector.valid(yy)) {
//                return new CachedCompound(o, new AnomVector(yy));
//            } else {
//                return super.transformDirect(o, yy);
//            }
//        }

        @Override
        public @Nullable Term applyTermOrNull(Term t) {
            return put(t); //may be called more directly
        }

        @Override
        public final @Nullable Termed apply(Term t) {
            return applyTermOrNull(t);
        }
    };

    final CompoundTransform GET = new DirectCompoundTransform() {

//        @Override
//        protected Term transformDirect(Op o, Compound x) {
//            Subterms ss = x.subterms();
//            if (ss instanceof AnomVector) {
//                return ((AnomVector)ss).reverse(o,Anon.this);
//            } else {
//                return super.transformDirect(o, x);
//            }
//        }

        @Override
        public @Nullable Term applyTermOrNull(Term t) {
            return _get(t); //may be called more directly
        }
        @Override
        public final @Nullable Termed apply(Term t) {
            return applyTermOrNull(t);
        }
    };

    public void clear() {
        fwd.clear();
        rev.clear();
    }

    public Term put(Term x) {
        if (x instanceof Variable) {
            //assert (!(x instanceof UnnormalizedVariable));
            if (x instanceof UnnormalizedVariable)
                throw new RuntimeException("unnormalized variable in Anon");
            return x; //ignore normalized variables
        } else if (x instanceof Atomic) {
            return Anom.the[fwd.getIfAbsentPutWithKey(x, nextUniqueAtom)];
        } else {
            return x.transform(PUT);
        }
    }

    public Term get(Term x) {
        return _get(x);
    }

    protected Term _get(Term x) {
        if (x instanceof Anom) {
            return rev.get(((Anom) x).id); //assume it is an int
        } else if (x instanceof Atomic) {
            return x; //ignore variables, ints
        } else {
            return x.transform(GET);
        }
    }

    public Task put(Task t) {
        Term x = t.term();
        assert (x.isNormalized());
        Term y = put(x);
        if (y == null || y instanceof Bool) {
            put(x); //temporary
            throw new RuntimeException("Anon fail for term: " + t);
        }
        if (y instanceof Compound)
            ((TermVector) y.subterms()).setNormalized();

        return Task.clone(t, y);
    }


    public static class CachingAnon extends Anon {

        final LinkedMRUMemoize<Term, Term> cache;

        public CachingAnon() {
            this(16);
        }

        public CachingAnon(int capacity) {
            cache = new LinkedMRUMemoize.LinkedMRUMemoizeRecurseable<>(super::_get, capacity);
        }

        @Override
        public void clear() {
            super.clear();
            cache.clear();
        }

        @Override
        public Term _get(Term x) {
            if (x instanceof Anom)
                return super._get(x);
            else
                return cache.apply(x);
        }
    }

}
