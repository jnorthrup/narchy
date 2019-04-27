package nars.index.concept;

import nars.concept.Concept;
import nars.concept.PermanentConcept;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Stream;

public class ProxyMemory extends Memory {
    final Memory ref;

    public ProxyMemory(Memory ref) {
        this.ref = ref;
    }

    @Override
    public @Nullable Termed get(Term key, boolean createIfMissing) {
        return ref.get(key, createIfMissing);
    }

    @Override
    public void set(Term src, Concept target) {
        if (target instanceof PermanentConcept) {
            
            return;
        }
        ref.set(src, target);
    }

    @Override
    public void clear() {

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
    public @Nullable Concept remove(Term entry) {
        return ref.remove(entry);
    }

    @Override
    public Stream<Concept> stream() {
        return ref.stream();
    }
}
