package nars.index.concept;

import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

/** additionally caches subterm vectors */
public class MapConceptIndex extends MaplikeConceptIndex {

    protected final Map<Term,Termed> concepts;

    public MapConceptIndex(Map<Term, Termed> map) {
        super();
        this.concepts = map;
    }

    @Override public Stream<Termed> stream() {
        return concepts.values().stream();
    }


    @Nullable
    @Override
    public Termed get(Term x, boolean createIfMissing) {
        if (createIfMissing) {
            return concepts.compute(x, nar.conceptBuilder);
        } else {
            return concepts.get(x);
        }
    }

    @Override
    public String summary() {
        return concepts.size() + " concepts";
    }


    @Override
    public Termed remove(Term entry) {
        return concepts.remove(entry);
    }

    @Override
    public void set(@NotNull Term src, @NotNull Termed target) {
        concepts.merge(src, target, setOrReplaceNonPermanent);
    }

    @Override
    public void clear() {
        super.clear();
        concepts.clear();
    }

    @Override
    public void forEach(@NotNull Consumer<? super Termed> c) {
        concepts.forEach((k, v)-> c.accept(v));
    }

    @Override
    public int size() {
        return concepts.size() /* + atoms.size? */;
    }












}
























































































































































































































