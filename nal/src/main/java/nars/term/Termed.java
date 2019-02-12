package nars.term;

import nars.Op;

import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * has, or is associated with a specific target
 */
public interface Termed extends Termlike {

    Term term();

    @Override
    default int volume() {
        return term().volume();
    }

//    @Override
//    default int complexity() {
//        return target().complexity();
//    }

    @Override
    default int structure() {
        return term().structure();
    }


    @Override
    default int subs(Predicate<Term> match) {
        return term().subs(match);
    }

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

    @Override
    default boolean contains(Term t) {
        return term().contains(t);
    }

    @Override
    default boolean hasXternal() {
        return term().hasXternal();
    }

    @Override
    default boolean recurseTerms(Predicate<Term> inSuperCompound, Predicate<Term> whileTrue, Compound parent) {
        return term().recurseTerms(inSuperCompound, whileTrue, parent);
    }

    @Override
    default boolean recurseTerms(Predicate<Compound> aSuperCompoundMust, BiPredicate<Term, Compound> whileTrue, Compound parent) {
        return term().recurseTerms(aSuperCompoundMust, whileTrue, parent);
    }
}
