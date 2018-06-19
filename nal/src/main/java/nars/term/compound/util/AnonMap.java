package nars.term.compound.util;

import jcog.list.FasterList;
import nars.term.Term;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectByteHashMap;

public class AnonMap {

    /** term -> id */
    protected final ObjectByteHashMap<Term> termToId;

    /** id -> term */
    protected final FasterList<Term> idToTerm;

    public AnonMap(int estSize) {
        termToId = new ObjectByteHashMap<>(estSize);
        idToTerm = new FasterList(estSize);
    }

    public void clear() {
        termToId.clear();
        idToTerm.clear();
    }

    /** put: returns in range 1..Byte.MAX_VALUE (does not issue 0) */
    private final byte intern_(Term x) {
        int s = idToTerm.addAndGetSize(x);
        assert (s < Byte.MAX_VALUE);
        return (byte) s;
    }

    public final byte intern(Term x) {
        return termToId.getIfAbsentPutWithKey(x, this::intern_);
    }

    /** get: accepts in range 1..Byte.MAX_VALUE (does not accept 0) */
    public Term interned(byte id) {
        assert(id > 0);
        return idToTerm.get(id-1);
    }

    /** use when you're sure that you are only going to read from this afterward */
    public void readonly() {
        termToId.clear();
    }
}
