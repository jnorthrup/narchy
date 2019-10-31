package nars.term.util;

import jcog.TODO;
import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.Termlike;
import nars.term.atom.Bool;
import org.eclipse.collections.api.block.function.primitive.IntObjectToIntFunction;

import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

import static nars.Op.*;

/**
 * has, or is associated with a specific target
 * use if the implementation is not a term but references a specific term instance.
 * this allows batch operations to occurr at the target, not through this instance which acts as an intermediary
 */
@FunctionalInterface
public interface TermedDelegate extends Termlike, Termed {

    @Override default int volume() {
        return 1 + term().sum(Term::volume);
    }
    @Override default int complexity() {
        return 1 + term().sum(Term::complexity);
    }

    @Override default int structure() {
        return term().structure();
    }

    @Override
    default boolean impossibleSubTerm(Termlike target) {
        return term().impossibleSubTerm(target);
    }

    @Override
    default int structureSurface() {
        return term().intifyShallow(0, (s, x) -> s | x.opBit());
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
    default int intifyShallow(int v, IntObjectToIntFunction<Term> reduce) {
        Termlike termlike = term();
        int n = termlike.subs();
        for (int i = 0; i < n; i++)
            v = reduce.intValueOf(v, termlike.sub(i));
        return v;
    }

    @Override
    default int intifyRecurse(int v, IntObjectToIntFunction<Term> reduce) {
        return term().intifyShallow(v, (v1, s) -> s.intifyRecurse(v1, reduce));
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
    default boolean containsInstance(Term t) {
        return term().containsInstance(t);
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

    @Override
    default Term subSafe(int i) {
        return sub(i, Bool.Null);
    }

    @Override
    default Term sub(int i, Term ifOutOfBounds) {
        return i >= subs() ? ifOutOfBounds : sub(i);
    }

    @Override
    default int height() {
        return subs() == 0 ? 1 : 1 + max(Term::height);
    }

    @Override
    default int sum(ToIntFunction<Term> value) {
//        int x = 0;
//        int s = subs();
//        for (int i = 0; i < s; i++)
//            x += value.applyAsInt(sub(i));
//
//        return x;
        return intifyShallow(0, (x, t) -> x + value.applyAsInt(t));
    }

    @Override
    default int max(ToIntFunction<Term> value) {
        return intifyShallow(Integer.MIN_VALUE, (x, t) -> Math.max(value.applyAsInt(t), x));
    }

    @Override
    default float voluplexity() {
        return (complexity() + volume()) / 2f;
    }

    @Override
    default boolean hasAll(int structuralVector) {
        return Op.has(structure(), structuralVector, true);
    }

    @Override
    default boolean hasAny(int structuralVector) {
        return Op.has(structure(), structuralVector, false);
    }

    @Override
    default /* final */ boolean hasAny(/*@NotNull*/ Op op) {
        return hasAny(op.bit);
    }

    @Override
    default /* final */ boolean hasAllAny(/*@NotNull*/ int all, int any) {
        int s = structure();
        return Op.has(s, all, true) && Op.has(s, any, false);
    }

    @Override
    default boolean hasVarIndep() {
        return hasAny(Op.VAR_INDEP.bit);
    }

    @Override
    default boolean hasVarDep() {
        return hasAny(Op.VAR_DEP.bit);
    }

    @Override
    default boolean hasVarQuery() {
        return hasAny(Op.VAR_QUERY.bit);
    }

    @Override
    default boolean hasVarPattern() {
        return hasAny(Op.VAR_PATTERN.bit);
    }

    @Override
    default boolean impossibleSubVolume(int otherTermVolume) {
        return otherTermVolume > volume() - subs();
    }

    @Override
    default int vars() {
        return hasVars() ? sum(Term::vars) : 0;
    }

    @Override
    default boolean hasVars() {
        return hasAny(VAR_INDEP.bit | VAR_DEP.bit | VAR_QUERY.bit | VAR_PATTERN.bit);
    }

    @Override
    default int varDep() {
        return sum(Term::varDep);
    }

    @Override
    default int varIndep() {
        return sum(Term::varIndep);
    }

    @Override
    default int varQuery() {
        return sum(Term::varQuery);
    }

    @Override
    default int varPattern() {
        return sum(Term::varPattern);
    }

    @Override
    default boolean these() {
        throw new TODO();
    }

    @Override
    default int addAllTo(Term[] t, int offset) {
        int s = subs();
        for (int i = 0; i < s; )
            t[offset++] = sub(i++);
        return s;
    }

    @Override
    default int subStructure() {
        return 0;
    }
}
