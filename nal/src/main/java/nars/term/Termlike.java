package nars.term;

import jcog.TODO;
import nars.Op;
import nars.term.atom.IdempotentBool;
import org.eclipse.collections.api.block.function.primitive.IntObjectToIntFunction;

import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

import static nars.Op.*;

/**
 * something which is like a target but isnt quite,
 * like a subterm container
 * <p>
 * Features exhibited by, and which can classify terms
 * and termlike productions
 */
public interface Termlike {


    /**
     * fast subterm access;
     * should not return Null (unless it really is Null) or null;
     * throw runtime exception if there is a problem
     */
    Term sub(int i);


    /**
     * returns Null if the index is out of bounds;
     * also dont expect a negative index, just >=0
     */
    default Term subSafe(int i) {
        return sub(i, IdempotentBool.Null);
    }

    /**
     * tries to get the ith subterm (if this is a TermContainer),
     * or of is out of bounds or not a container,
     * returns the provided ifOutOfBounds
     */
    default Term sub(int i, Term ifOutOfBounds) {
        return i >= subs() ? ifOutOfBounds : sub(i);
    }


    /**
     * number of subterms. if atomic, size=0
     */
    int subs();

    /**
     * recursion height; atomic=1, compound>1
     */
    default int height() {
        return subs() == 0 ? 1 : 1 + max(Term::height);
    }

    /**
     * syntactic volume = 1 + total volume of terms = complexity of subterms - # variable instances
     */
    default int volume() {
        return 1 + sum(Term::volume);
    }


    /**
     * syntactic complexity 1 + total complexity number of leaf terms, excluding variables which have a complexity of zero
     */
    default int complexity() {
        return 1 + sum(Term::complexity);
    }

    /**
     * only 1-layer (shallow, non-recursive)
     */
    default int sum(ToIntFunction<Term> value) {
//        int x = 0;
//        int s = subs();
//        for (int i = 0; i < s; i++)
//            x += value.applyAsInt(sub(i));
//
//        return x;
        return intifyShallow(0, (x, t) -> x + value.applyAsInt(t));
    }

    /**
     * only 1-layer (shallow, non-recursive)
     */
    default int max(ToIntFunction<Term> value) {
        return intifyShallow(Integer.MIN_VALUE, (x, t) -> Math.max(value.applyAsInt(t), x));
    }


    /**
     * non-recursive, visits only 1 layer deep, and not the current if compound
     */
    default int intifyShallow(int v, IntObjectToIntFunction<Term> reduce) {
        int n = subs();
        for (int i = 0; i < n; i++)
            v = reduce.intValueOf(v, sub(i));
        return v;
    }

    default int intifyRecurse(int _v, IntObjectToIntFunction<Term> reduce) {
        return intifyShallow(_v, (v, s) -> s.intifyRecurse(v, reduce));
    }

    boolean recurseTerms(Predicate<Term> inSuperCompound, Predicate<Term> whileTrue, Compound parent);

    boolean recurseTerms(Predicate<Compound> aSuperCompoundMust, BiPredicate<Term, Compound> whileTrue, Compound parent);


    /**
     * structure hash bitvector
     */
    int structure();

    /**
     * average of complexity and volume
     */
    default float voluplexity() {
        return (float) (complexity() + volume()) / 2f;
    }


    /**
     * (first-level only, non-recursive)
     * if contained within; doesnt match this target (if it's a target);
     * false if target is atomic since it can contain nothing
     * TODO move to Subterms
     */
    @Deprecated
    boolean contains(Term t);

    /**
     * TODO move to Subterms
     */
    @Deprecated
    boolean containsInstance(Term t);

    boolean hasXternal();


    boolean impossibleSubTerm(Termlike target);


    default boolean hasAll(int structuralVector) {
        return Op.has(structure(), structuralVector, true);
    }

    default boolean hasAny(int structuralVector) {
        return Op.has(structure(), structuralVector, false);
    }

    default   boolean hasAny(/*@NotNull*/ Op op) {
        return hasAny(op.bit);
    }

    default   boolean hasAllAny(/*@NotNull*/ int all, int any) {
        int s = structure();
        return Op.has(s, all, true) && Op.has(s, any, false);
    }

    default boolean hasVarIndep() {
        return hasAny(Op.VAR_INDEP.bit);
    }

    default boolean hasVarDep() {
        return hasAny(Op.VAR_DEP.bit);
    }

    default boolean hasVarQuery() {
        return hasAny(Op.VAR_QUERY.bit);
    }

    default boolean hasVarPattern() {
        return hasAny(Op.VAR_PATTERN.bit);
    }


    boolean impossibleSubStructure(int structure);

    default boolean impossibleSubVolume(int otherTermVolume) {
        return otherTermVolume > volume() - subs();
    }

//    /**
//     * if it's larger than this target it can not be equal to this.
//     * if it's larger than some number less than that, it can't be a subterm.
//     */
//    default boolean impossibleSubTermOrEqualityVolume(int otherTermsVolume) {
//        return otherTermsVolume > volume();
//    }

//    default boolean impossibleSubTermOrEquality(/*@NotNull*/Term target) {
//        return ((!hasAll(target.structure())) ||
//                (impossibleSubTermOrEqualityVolume(target.volume())));
//    }


    default int vars() {
        return hasVars() ? sum(Term::vars) : 0;
    }

    default boolean hasVars() {
        return hasAny(VAR_INDEP.bit | VAR_DEP.bit | VAR_QUERY.bit | VAR_PATTERN.bit);
    }

    /**
     * # of contained dependent variables in subterms (1st layer only)
     */
    default int varDep() {
        return sum(Term::varDep);
    }

    default int varIndep() {
        return sum(Term::varIndep);
    }

    default int varQuery() {
        return sum(Term::varQuery);
    }

    default int varPattern() {
        return sum(Term::varPattern);
    }


    /**
     * structure of the first layer (surface) only
     */
    default int structureSurface() {
        return intifyShallow(0, (s, x) -> s | x.opBit());
    }

    /**
     * immutable internability
     */
    default boolean these() {
        throw new TODO();
    }

    default int addAllTo(Term[] t, int offset) {
        int s = subs();
        for (int i = 0; i < s; )
            t[offset++] = sub(i++);
        return s;
    }

    default int subStructure() {
        return 0;
    }
}

