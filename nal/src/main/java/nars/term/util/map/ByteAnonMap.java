package nars.term.util.map;

import jcog.TODO;
import jcog.data.list.FasterList;
import nars.term.Term;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectByteHashMap;

import java.util.function.Function;
import java.util.function.UnaryOperator;

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


    public ByteAnonMap() {
        this(0);
    }

    public ByteAnonMap(int estSize) {
        this(new ObjectByteHashMap<>(estSize),new FasterList<>(estSize));

    }

    public boolean isEmpty() {
        return idToTerm.isEmpty(); // && termToId.isEmpty();
    }

    public void clear() {
        var s = termToId.size();
        if (s > 0) {
            termToId.clear();
            idToTerm.clear();
        }
        var compactThesh = 4;
        if (s > compactThesh) {
            termToId.compact();
            idToTerm.clearCapacity(compactThesh);
        }
    }

    /** put: returns in range 1..Byte.MAX_VALUE (does not issue 0) */
    public final byte intern(Term x) {
        return termToId.getIfAbsentPutWithKey(x, idToTerm::addAndGetSizeAsByte);
        //assert (b >= 0);
    }

    /** returns Byte.MIN_VALUE if missing */
    public final byte interned(Term x) {
        return termToId.getIfAbsent(x, Byte.MIN_VALUE);
    }

    /** get: accepts in range 1..Byte.MAX_VALUE (does not accept 0) */
    public final Term interned(byte id) {
        //assert(id > 0);
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

    public boolean updateMap(UnaryOperator<Term> m) {
        var changed = false;
        for (int i = 0, idToTermSize = idToTerm.size(); i < idToTermSize; i++) {
            var x = idToTerm.get(i);
            var y = m.apply(x);
            if (x!=y) {
                idToTerm.setFast(i, y);
                changed = true;
            }
        }
        return changed;
    }
}
