package nars.memory;

import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ConcurrentHashMap;

/**
 * acts as a pass-through. only holds permanent concepts and explicit set values
 * UNTESTED not quite right yet
 */
public class NullMemory extends MapMemory {

    public NullMemory() {
        super(new ConcurrentHashMap(1024));
    }

    @Override public @Nullable Termed get(Term x, boolean createIfMissing) {
        Termed exist = super.get(x, false);
        if (exist!=null)
            return exist;
        else if (createIfMissing)
            return nar.conceptBuilder.apply(x, null);
        else
            return null;
    }


}
