package nars.term.compound;

import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import org.eclipse.collections.api.block.function.primitive.IntObjectToIntFunction;

import java.util.function.Consumer;
import java.util.function.Predicate;

import static nars.time.Tense.DTERNAL;

public interface SeparateSubtermsCompound extends Compound {

    /*@NotNull*/
    @Override
    default Term[] arrayClone() {
        return subterms().arrayClone();
    }

    @Override
    default Term[] arrayShared() {
        return subterms().arrayShared();
    }

    @Override
    default Term[] arrayClone(Term[] x, int from, int to) {
        return subterms().arrayClone(x, from, to);
    }

    @Override
    default int subs() {
        return subterms().subs();
    }

    @Override
    default Term sub(int i) {
        return subterms().sub(i);
    }

    @Override
    default int subs(Op matchingOp) {
        return subterms().subs(matchingOp);
    }

    @Override
    default int subs(Predicate<Term> match) {
        return subterms().subs(match);
    }


    @Override
    default void forEach(/*@NotNull*/ Consumer<? super Term> c) {
        subterms().forEach(c);
    }

    @Override
    default boolean contains(Term t) {
        return subterms().contains(t);
    }

    @Override
    default boolean containsNeg(Term x) {
        return subterms().containsNeg(x);
    }

    @Override
    default int structure() {
        return subterms().structure() | op().bit;
    }

    @Override
    default int complexity() {
        return subterms().complexity();
    }

    @Override
    default int volume() {
        return subterms().volume();
    }

    @Override
    default int varQuery() {
        return subterms().varQuery();
    }

    @Override
    default int varPattern() {
        return subterms().varPattern();
    }
    @Override
    default int varDep() {
        return subterms().varDep();
    }

    @Override
    default int varIndep() {
        return subterms().varIndep();
    }

    @Override
    default int vars() {
        return subterms().vars();
    }

    @Override
    default int intifyRecurse(IntObjectToIntFunction<Term> reduce, int v) {
        return subterms().intifyRecurse(reduce, v);
    }

    @Override
    default int intifyShallow(IntObjectToIntFunction<Term> reduce, int v) {
        return subterms().intifyShallow(reduce, v);
    }

    @Override
    default boolean isTemporal() {
        return (dt() != DTERNAL && op().temporal)
                ||
                (subterms().isTemporal());
    }

    @Override
    default boolean OR(/*@NotNull*/ Predicate<Term> p) {
        return subterms().OR(p);
    }

    @Override
    default boolean AND(/*@NotNull*/ Predicate<Term> p) {
        return subterms().AND(p);
    }

}
