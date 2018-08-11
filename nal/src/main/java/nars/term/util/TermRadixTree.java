package nars.term.util;

import jcog.data.byt.AbstractBytes;
import jcog.data.byt.ArrayBytes;
import jcog.tree.radix.MyConcurrentRadixTree;
import nars.IO;
import nars.term.Term;
import nars.term.Termed;

/**
 * String interner that maps strings to integers and resolves them
 * bidirectionally with a globally shared Atomic concept
 */
public class TermRadixTree<X> extends MyConcurrentRadixTree<X> {

    @Override
    public X put(X value) {
        return put(key(value), value);
    }


    public X put(Term key, X value) {
        return put(key(key), value);
    }

    /** must override if X is not instanceof Termed */
    public AbstractBytes key(Object k) {
        return new ArrayBytes(IO.termToBytes(((Termed) k).term()));
    }

    public X get(AbstractBytes term) {
        return getValueForExactKey(term);
    }

}
