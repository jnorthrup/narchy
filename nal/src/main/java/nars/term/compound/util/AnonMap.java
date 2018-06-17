package nars.term.compound.util;

import jcog.list.FasterList;
import nars.term.Term;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectByteHashMap;

public class AnonMap {

    /** term -> id */
    protected final ObjectByteHashMap<Term> termToId;

    /** id -> term */
    protected final FasterList<Term> idToTerm;

    protected AnonMap(int estSize) {
        termToId = new ObjectByteHashMap<>(estSize);
        idToTerm = new FasterList(estSize);
    }

    public void clear() {
        termToId.clear();
        idToTerm.clear();
    }
}
