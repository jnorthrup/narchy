package nars.index.concept;

import nars.concept.PermanentConcept;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

public class ProxyConceptIndex extends MaplikeConceptIndex {
    final ConceptIndex ref;

    public ProxyConceptIndex(ConceptIndex ref) {
        this.ref = ref;
    }

    @Override
    public @Nullable Termed get(Term key, boolean createIfMissing) {
        return ref.get(key, createIfMissing);
    }

    @Override
    public void set(Term src, Termed target) {
        if (target instanceof PermanentConcept) {
            
            return;
        }
        ref.set(src, target);
    }


    @Override
    public int size() {
        return ref.size();
    }

    @Override
    public String summary() {
        return ref.summary();
    }

    @Override
    public Termed remove(Term entry) {
        return ref.remove(entry);
    }

    @Override
    public Stream<Termed> stream() {
        return ref.stream();
    }
}
