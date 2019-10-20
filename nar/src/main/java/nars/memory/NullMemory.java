package nars.memory;

import nars.concept.Concept;
import nars.term.Term;

import java.util.concurrent.ConcurrentHashMap;

/**
 * acts as a pass-through. only holds permanent concepts and explicit set values
 * UNTESTED not quite right yet
 */
public class NullMemory extends MapMemory {

    public NullMemory() {
        super(new ConcurrentHashMap(1024));
    }

    @Override public Concept get(Term x, boolean createIfMissing) {
        var exist = super.get(x, false);
        if (exist!=null)
            return exist;
        else if (createIfMissing)
            return nar.conceptBuilder.apply(x, null);
        else
            return null;
    }


}
