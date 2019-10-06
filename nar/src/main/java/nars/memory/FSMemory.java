package nars.memory;

import nars.concept.Concept;
import nars.term.Term;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * read/write from native filesystem hierarchy
 * TODO */
public class FSMemory extends Memory {

    final Path root;

    public FSMemory(Path root) {
        this.root = root;
    }

    @Override
    public Concept get(Term key, boolean createIfMissing) {
        return null;
    }

    @Override
    public void set(Term src, Concept target) {

    }

    @Override
    public void clear() {

    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public String summary() {
        return null;
    }

    @Override
    public @Nullable Concept remove(Term entry) {
        return null;
    }

    @Override
    public Stream<Concept> stream() {
        return null;
    }
}
