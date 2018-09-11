package nars.term;

import nars.Op;

import java.util.function.Predicate;

/**
 * has, or is associated with a specific term
 */
public interface Termed extends Termlike {

    Term term();

    default Op op() {
        return term().op();
    }
    default int subs() {
        return term().subs();
    }
    default Term sub(int i) {
        return term().sub(i);
    }

    @Override
    default boolean containsRecursively(Term t, boolean root, Predicate<Term> inSubtermsOf) {
        return term().containsRecursively(t, root, inSubtermsOf);
    }

    @Override
    default boolean impossibleSubStructure(int structure) {
        return term().impossibleSubStructure(structure);
    }
}
