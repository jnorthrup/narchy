package nars.term;

import nars.Op;
import nars.subterm.Subterms;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Predicate;

import static nars.Op.Null;

/**
 * has, or is associated with a specific term
 */
public interface Termed extends Termlike {

    /*@NotNull*/ Term term();

    @Override default Term sub(int i) {
        return subterms().sub(i);
    }

    default Subterms subterms() {
        return term().subterms();
    }

    @Override
    default int subs() {
        return term().subs();
    }

    /*@NotNull*/
    default Op op() {
        return term().op();
    }


    default boolean isNormalized() {
        return term().isNormalized();
    }

    @Nullable
    static Term termOrNull(@Nullable Termed x) {
        return x == null ? null : x.term();
    }

    @Override
    default int volume() {
        Term t = term();
        return this != t ? t.volume() : Termlike.super.volume();
    }

    @Override
    default int complexity() {
        Term t = term();
        return this != t ? t.complexity() : Termlike.super.complexity();
    }

    @Override
    default int structure() {
        Term t = term();
        return this != t ? t.structure() : Termlike.super.structure();
    }
    @Override
    default int vars() {
        Term t = term();
        return this != t ? t.vars() : Termlike.super.vars();
    }

    /*@NotNull*/
    default Term unneg() {
        return term().unneg();
    }
    default Term neg() {
        return term().neg();
    }

    @Override
    default boolean containsRecursively(Term t, boolean root, Predicate<Term> inSubtermsOf) {
        return term().containsRecursively(t, root, inSubtermsOf);
    }

    @Override
    default Term sub(int i, Term ifOutOfBounds) {
        return term().sub(i, ifOutOfBounds);
    }

    @Override
    default boolean ORrecurse(Predicate<Term> v) {
        return term().ORrecurse(v);
    }

    @Override
    default boolean ANDrecurse(Predicate<Term> v) {
        return term().ANDrecurse(v);
    }

    @Override
    default boolean isTemporal() {
        return term().isTemporal();
    }

    @Override
    default void recurseTerms(Consumer<Term> v) {
        term().recurseTerms(v);
    }


    default Term conceptualizableOrNull() {
        if (!op().conceptualizable)
            return Null;
        return term();
    }

}
