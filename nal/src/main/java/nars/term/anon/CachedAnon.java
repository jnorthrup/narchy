package nars.term.anon;

import jcog.memoize.LinkedMRUMemoize;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atom;

public class CachedAnon extends Anon {

    protected final LinkedMRUMemoize.LinkedMRUMemoizeRecurseable<Term, Term> cache;

    public CachedAnon() {
        this(16);
    }

    public CachedAnon(int capacity) {
        cache = new LinkedMRUMemoize.LinkedMRUMemoizeRecurseable<>(super::get, capacity);
    }

    @Override
    public void clear() {
        super.clear();
        cache.clear();
    }

    @Override
    public Term get(Term x) {
        if ((x instanceof Compound)) {
            return cache.apply(x);
        } else {
            return super.get(x);
        }
    }

//    @Override
//    protected Term putTransformed(Term x) {
//        Term y = super.putTransformed(x);
//        if (y instanceof Compound) {
//            cache.putIfAbsent(y, x);
//        }
//        return y;
//    }
}
