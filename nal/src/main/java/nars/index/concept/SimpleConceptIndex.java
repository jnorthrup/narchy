package nars.index.concept;

import jcog.data.map.MRUMap;
import nars.concept.Concept;
import nars.concept.PermanentConcept;
import nars.term.Term;
import nars.term.Termed;

import java.util.Collections;
import java.util.Map;

/** simple concept index that uses a LRU-evicting LinkedHashMap */
public class SimpleConceptIndex extends MapConceptIndex {

    public SimpleConceptIndex(int capacity) {
        this(capacity,  false);
    }

    public SimpleConceptIndex(int capacity, boolean threadSafe) {
        this(capacity*2, 0.99f, threadSafe);
    }

    protected SimpleConceptIndex(int capacity, float loadFactor, boolean threadSafe) {
        super(synchronizedIf(new MyMRUMap(capacity, loadFactor), threadSafe));
    }

    private static <X,Y> Map<X,Y> synchronizedIf(Map<X,Y> m, boolean threadSafe) {
        return threadSafe ? Collections.synchronizedMap(m) : m;
    }

    private static final class MyMRUMap extends MRUMap<Term, Termed> {
        public MyMRUMap(int capacity, float loadFactor) {
            super(capacity, loadFactor);
        }

        @Override
        protected void onEvict(Map.Entry<Term, Termed> entry) {
            Termed c = entry.getValue();
            if (c instanceof PermanentConcept) {
                //throw new TODO("Should not evict " + c);
                put(entry.getKey(), c);
            } else {
                ((Concept) c).delete(null /* HACK */);
            }
        }
    }
}
