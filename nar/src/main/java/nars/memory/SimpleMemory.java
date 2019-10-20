package nars.memory;

import jcog.data.map.MRUMap;
import nars.concept.Concept;
import nars.concept.PermanentConcept;
import nars.term.Term;

import java.util.Collections;
import java.util.Map;

/** simple concept index that uses a LRU-evicting LinkedHashMap */
public class SimpleMemory extends MapMemory {

    public SimpleMemory(int capacity) {
        this(capacity,  false);
    }

    public SimpleMemory(int capacity, boolean threadSafe) {
        this(capacity, 0.5f, threadSafe);
    }

    protected SimpleMemory(int capacity, float loadFactor, boolean threadSafe) {
        super();
        map(synchronizedIf(new MyMRUMap(capacity, loadFactor), threadSafe));
    }

    private static <X,Y> Map<X,Y> synchronizedIf(Map<X,Y> m, boolean threadSafe) {
        return threadSafe ? Collections.synchronizedMap(m) : m;
    }

    private final class MyMRUMap extends MRUMap<Term, Concept> {
        public MyMRUMap(int capacity, float loadFactor) {
            super(capacity, loadFactor);
        }

        @Override
        protected void onEvict(Map.Entry<Term, Concept> entry) {
            var c = entry.getValue();
            if (c instanceof PermanentConcept) {
                //throw new TODO("Should not evict " + c);
                put(entry.getKey(), c);
            } else {
                onRemove(c);
            }
        }
    }
}
