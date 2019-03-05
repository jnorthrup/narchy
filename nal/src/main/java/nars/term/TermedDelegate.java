package nars.term;

import nars.Op;
import org.eclipse.collections.api.block.function.primitive.IntObjectToIntFunction;

import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * has, or is associated with a specific target
 * use if the implementation is not a term but references a specific term instance.
 * this allows batch operations to occurr at the target, not through this instance which acts as an intermediary
 */
public interface TermedDelegate extends Termlike, Termed {

    @Override default int volume() {
        return term().volume();
    }
    @Override default int complexity() { return term().complexity(); }

    @Override default int structure() {
        return term().structure();
    }


    @Override
    default int subs(Predicate<Term> match) {
        return term().subs(match);
    }

    default Op op() {
        return term().op();
    }
    @Override default int subs() {
        return term().subs();
    }
    @Override default Term sub(int i) {
        return term().sub(i);
    }

    @Override
    default boolean subIs(int i, Op o) {
        return term().subIs(i, o);
    }

    @Override
    default boolean subIsOrOOB(int i, Op o) {
        return term().subIsOrOOB(i, o);
    }

    @Override
    default int intifyShallow(IntObjectToIntFunction<Term> reduce, int v) {
        return term().intifyShallow(reduce, v);
    }

    @Override
    default int intifyRecurse(IntObjectToIntFunction<Term> reduce, int v) {
        return term().intifyRecurse(reduce, v);
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
