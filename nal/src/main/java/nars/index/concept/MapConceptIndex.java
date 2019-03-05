package nars.index.concept;

import nars.term.Term;
import nars.term.Termed;
import nars.term.TermedDelegate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

/** additionally caches subterm vectors */
public class MapConceptIndex extends MaplikeConceptIndex {

    protected final Map<Term,Termed> intern;

    public MapConceptIndex(Map<Term, Termed> map) {
        super();
        this.intern = map;
    }

    @Override public Stream<Termed> stream() {
        return intern.values().stream();
    }


    @Nullable
    @Override
    public Termed get(Term x, boolean createIfMissing) {
        if (createIfMissing) {
            return intern.compute(x, nar.conceptBuilder);
        } else {
            return intern.get(x);
        }
    }

    @Override
    public String summary() {
        return intern.size() + " concepts";
    }


    @Override
    public Termed remove(Term entry) {
        return intern.remove(entry);
    }

    @Override
    public void set(@NotNull Term src, @NotNull Termed target) {
        intern.merge(src, target, setOrReplaceNonPermanent);
    }

    @Override
    public void clear() {
        super.clear();
        intern.clear();
    }

    @Override
    public void forEach(@NotNull Consumer<? super Termed> c) {
        intern.forEach((k, v)-> c.accept(v));
    }

    @Override
    public int size() {
        return intern.size() /* + atoms.size? */;
    }












}
























































































































































































































