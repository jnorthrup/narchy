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
            //ignore HACK. assumes the ref already has it
            return;
        }
        ref.set(src, target);
    }

    @Override
    public void clear() {
        //ignore
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
    public void remove(Term entry) {
        //ignore
    }

    @Override
    public Stream<? extends Termed> stream() {
        return ref.stream();
    }
}
