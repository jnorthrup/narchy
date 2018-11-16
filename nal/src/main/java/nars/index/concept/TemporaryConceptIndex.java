package nars.index.concept;

import nars.concept.Concept;
import nars.link.Activate;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/** uses only the attention bag
 * TODO not completely functional yet
 * */
public class TemporaryConceptIndex extends AbstractConceptIndex {

    final Map<Term, Termed> permanent = new ConcurrentHashMap<>();

    @Override
    public @Nullable Termed get(Term key, boolean createIfMissing) {
        @Nullable Activate a = active.get(key);
        if (a != null)
            return a.get();

        Termed p = permanent.get(key);
        if (p!=null)
            return p;

        if (createIfMissing)
            return nar.conceptBuilder.apply(key);
        else
            return null;
    }

    @Override
    public void set(Term src, Termed target) {
        @Nullable Activate a = active.remove(src);
        permanent.put(src, target);
        if (a!=null)
            active.put(new Activate((Concept)target, a.pri()));
    }

    @Override
    public void clear() {
        permanent.clear();
    }

    @Override
    public int size() {
        return active.size();
    }

    @Override
    public String summary() {
        return "";
    }

    @Override
    public @Nullable Termed remove(Term entry) {
        Termed c = permanent.remove(entry);
        @Nullable Activate a = active.remove(entry);
        return c!=null ? c : (a!=null ? a.get() : null);
    }

    @Override
    public Stream<Termed> stream() {
        return Stream.concat(active.stream(), permanent.values().stream()).distinct();
    }
}
