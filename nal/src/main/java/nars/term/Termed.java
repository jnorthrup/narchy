package nars.term;

import nars.Op;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * has, or is associated with a specific term
 */
public interface Termed extends Termlike {

    /*@NotNull*/ Term term();
//

//
    /*@NotNull*/
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
    default void recurseTerms(Consumer<Term> v) {
        term().recurseTerms(v);
    }

    //
//    default boolean isNormalized() {
//        return term().isNormalized();
//    }
//
//    @Nullable
//    static Term termOrNull(@Nullable Termed x) {
//        return x == null ? null : x.term();
//    }
//
//    @Override
//    default int volume() {
//        Term t = term();
//        return this != t ? t.volume() : Termlike.super.volume();
//    }
//
//    @Override
//    default int complexity() {
//        Term t = term();
//        return this != t ? t.complexity() : Termlike.super.complexity();
//    }
//
//    @Override
//    default int structure() {
//        Term t = term();
//        return this != t ? t.structure() : Termlike.super.structure();
//    }
//    @Override
//    default int vars() {
//        Term t = term();
//        return this != t ? t.vars() : Termlike.super.vars();
//    }
//
//    /*@NotNull*/
//    default Term unneg() {
//        return term().unneg();
//    }
//    default Term neg() {
//        return term().neg();
//    }
//
//    @Override
//    default boolean containsRecursively(Term t, boolean root, Predicate<Term> inSubtermsOf) {
//        return term().containsRecursively(t, root, inSubtermsOf);
//    }
//
//    @Override
//    default Term sub(int i, Term ifOutOfBounds) {
//        return term().sub(i, ifOutOfBounds);
//    }
//
//    @Override
//    default boolean OR(Predicate<Term> p) {
//        return term().OR(p);
//    }
//
//    @Override
//    default boolean AND(Predicate<Term> p) {
//        return term().AND(p);
//    }
//
//    @Override
//    default boolean ORwith(ObjectIntPredicate<Term> p) {
//        return term().ORwith(p);
//    }
//
//    @Override
//    default boolean ANDwith(ObjectIntPredicate<Term> p) {
//        return term().ANDwith(p);
//    }
//
//    @Override
//    default int intifyShallow(IntObjectToIntFunction<Term> reduce, int v) {
//        return term().intifyShallow(reduce, v);
//    }
//
//    @Override
//    default boolean ORrecurse(Predicate<Term> v) {
//        return term().ORrecurse(v);
//    }
//
//    @Override
//    default boolean ANDrecurse(Predicate<Term> v) {
//        return term().ANDrecurse(v);
//    }
//
//    @Override
//    default boolean isTemporal() {
//        return term().isTemporal();
//    }
//
//    @Override
//    default void recurseTerms(Consumer<Term> v) {
//        term().recurseTerms(v);
//    }


}
