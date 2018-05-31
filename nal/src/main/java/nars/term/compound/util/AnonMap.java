package nars.term.compound.util;

import jcog.list.FasterList;
import nars.term.Term;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectByteHashMap;

public class AnonMap {

    /** term -> id */
    public final ObjectByteHashMap<Term> termToId;

    /** id -> term */
    public final FasterList<Term> idToTerm;

    public AnonMap(int estSize) {
        termToId = new ObjectByteHashMap<>(estSize);
        idToTerm = new FasterList(estSize/2 /* HEURISTIC */);
    }

    public void clear() {
        termToId.clear();
        idToTerm.clear();
    }
}
