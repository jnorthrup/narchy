package nars.term.util.map;

import jcog.TODO;
import jcog.data.list.FasterList;
import nars.term.Term;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectByteHashMap;

import java.util.function.Function;

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
        this(new ObjectByteHashMap<>(estSize),new FasterList<>(estSize));

    }

    public void clear() {
        if (!termToId.isEmpty()) termToId.clear();
        idToTerm.clear();
    }

    /** put: returns in range 1..Byte.MAX_VALUE (does not issue 0) */
    public final byte intern(Term x) {
        byte b = termToId.getIfAbsentPutWithKey(x, idToTerm::addAndGetSizeAsByte);
        assert (b >= 0);
        return b;
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

    @Override
    public String toString() {
        return idToTerm.toString();
    }

    public boolean updateMap(Function<Term, Term> m) {
        boolean changed = false;
        for (int i = 0, idToTermSize = idToTerm.size(); i < idToTermSize; i++) {
            Term x = idToTerm.get(i);
            Term y = m.apply(x);
            if (x!=y) {
                idToTerm.set(i, y);
                changed = true;
            }
        }
        return changed;
    }
}
