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

//    default int varPattern() {
//        return term().varPattern();
//    }
//
//    default int varQuery() {
//        return term().varQuery();
//    }
//
//    default int varIndep() {
//        return term().varIndep();
//    }
//
//    default int varDep() {
//        return term().varDep();
//    }
//    default boolean levelValid(int nal) {
//        return term().levelValid(nal);
//    }

    default boolean isNormalized() {
        return term().isNormalized();
    }

    @Nullable
    static Term termOrNull(@Nullable Termed x) {
        return x == null ? null : x.term();
    }

    @Override
    default int volume() {
        Term t = term(); if (this!=t) return t.volume(); else return Termlike.super.volume();
    }

    @Override
    default int complexity() {
        Term t = term(); if (this!=t) return t.complexity(); else return Termlike.super.complexity();
    }

    @Override
    default int structure() {
        Term t = term(); if (this!=t) return t.structure(); else return Termlike.super.structure();
    }
    @Override
    default int vars() {
        Term t = term(); if (this!=t) return t.vars(); else return Termlike.super.vars();
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
    default void recurseTerms(Consumer<Term> v) {
        term().recurseTerms(v);
    }


    default Term conceptualizableOrNull() {
        if (!op().conceptualizable)
            return Null;
        return term();
    }

}
