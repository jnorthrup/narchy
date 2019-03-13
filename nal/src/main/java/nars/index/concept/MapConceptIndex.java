package nars.index.concept;

import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

/** additionally caches subterm vectors */
public class MapConceptIndex extends ConceptIndex {

    protected Map<Term,Termed> map;

    protected MapConceptIndex() {
        this(null);
    }

    public MapConceptIndex(Map<Term, Termed> map) {
        super();
        this.map = map;
    }

    public MapConceptIndex map(Map<Term, Termed> map) {
        this.map = map;
        return this;
    }

    @Override public Stream<Termed> stream() {
        return map.values().stream();
    }

    @Nullable
    @Override
    public Termed get(Term x, boolean createIfMissing) {
        if (createIfMissing) {
            return map.compute(x, nar.conceptBuilder);
        } else {
            return map.get(x);
        }
    }

    @Override
    public String summary() {
        return map.size() + " concepts";
    }


    @Override
    public Termed remove(Term entry) {
        return map.remove(entry);
    }

    @Override
    public void set(@NotNull Term src, @NotNull Termed target) {
        map.merge(src, target, setOrReplaceNonPermanent);
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public void forEach(@NotNull Consumer<? super Termed> c) {
        map.forEach((k, v)-> c.accept(v));
    }

    @Override
    public int size() {
        return map.size() /* + atoms.size? */;
    }


}
























































































































































































































