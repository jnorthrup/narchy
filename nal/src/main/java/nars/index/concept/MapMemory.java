package nars.index.concept;

import nars.concept.Concept;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

/** additionally caches subterm vectors */
public class MapMemory extends Memory {

    protected Map<Term,Concept> map;

    protected MapMemory() {
        this(null);
    }

    public MapMemory(Map<Term, Concept> map) {
        super();
        this.map = map;
    }

    public MapMemory map(Map<Term, Concept> map) {
        this.map = map;
        return this;
    }

    @Override public Stream<Concept> stream() {
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
    public @Nullable Concept remove(Term entry) {
        return map.remove(entry);
    }

    @Override
    public void set(@NotNull Term src, Concept target) {
        map.merge(src, target, setOrReplaceNonPermanent);
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public void forEach(Consumer<? super Concept> c) {
        map.forEach((k, v)-> c.accept(v));
    }

    @Override
    public int size() {
        return map.size() /* + atoms.size? */;
    }


}
























































































































































































































