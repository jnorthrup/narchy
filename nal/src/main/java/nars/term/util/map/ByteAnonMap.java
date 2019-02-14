package nars.term.util.map;

import jcog.TODO;
import jcog.data.list.FasterList;
import nars.term.Term;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectByteHashMap;

/** this assumes < 127 unique elements */
public class ByteAnonMap {

    /** target -> id */
    public final ObjectByteHashMap<Term> termToId;

    /** id -> target */
    public final FasterList<Term> idToTerm;

    public ByteAnonMap(ObjectByteHashMap<Term> termToId, FasterList<Term> idtoTerm) {
        this.termToId = termToId;
        this.idToTerm = idtoTerm;
    }

    public ByteAnonMap(int estSize) {
        this(new ObjectByteHashMap<>(estSize),new FasterList(estSize));

    }

    public void clear() {
        if (!termToId.isEmpty()) termToId.clear();
        idToTerm.clear();
    }

    /** put: returns in range 1..Byte.MAX_VALUE (does not issue 0) */
    private byte intern_(Term x) {
        int s = idToTerm.addAndGetSize(x);
        assert (s <= Byte.MAX_VALUE);
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

    public int termCount() {
        return idToTerm.size();
    }

    @Override
    public boolean equals(Object obj) {
        throw new TODO();
    }

    @Override
    public int hashCode() {
        throw new TODO();
    }
}
