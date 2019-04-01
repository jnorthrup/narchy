package nars.term;

import jcog.TODO;
import nars.Op;
import nars.term.atom.Bool;
import org.eclipse.collections.api.block.function.primitive.IntObjectToIntFunction;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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
        return sub(i, Bool.Null);
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
        if (subs() == 0) return 1;
        else return 1 + max(Term::height);
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
        return intifyShallow((x, t) -> x + value.applyAsInt(t), 0);
    }

    /**
     * only 1-layer (shallow, non-recursive)
     */
    default int max(ToIntFunction<Term> value) {
        return intifyShallow((x, t) -> Math.max(value.applyAsInt(t), x), Integer.MIN_VALUE);
    }


    /**
     * non-recursive, visits only 1 layer deep, and not the current if compound
     */
    default int intifyShallow(IntObjectToIntFunction<Term> reduce, int v) {
        int n = subs();
        for (int i = 0; i < n; i++)
            v = reduce.intValueOf(v, sub(i));
        return v;
    }

    default int intifyRecurse(IntObjectToIntFunction<Term> reduce, int _v) {
        return intifyShallow((v, s) -> s.intifyRecurse(reduce, v), _v);
//        int n = subs();
//        for (int i = 0; i < n; i++)
//            v = sub(i).intifyRecurse(reduce, v);
//        return v;
    }

    boolean recurseTerms(Predicate<Term> inSuperCompound, Predicate<Term> whileTrue, Compound parent);

    boolean recurseTerms(Predicate<Compound> aSuperCompoundMust, BiPredicate<Term, Compound> whileTrue, Compound parent);


    /**
     * counts subterms matching the predicate
     */
    default int count(Predicate<Term> match) {
        return intifyShallow((c, sub) -> match.test(sub) ? c + 1 : c, 0);
    }

    /**
     * return the index of the first subterm matching the predicate, or -1 if none match
     */
    default int subIndexFirst(Predicate<Term> match) {
        int s = subs();
        for (int i = 0; i < s; i++) {
            Term si = sub(i);
            if (match.test(si))
                return i;
        }
        return -1;
    }

    /**
     * return the first subterm matching the predicate, or null if none match
     */
    @Nullable
    default Term subFirst(Predicate<Term> match) {
        int i = subIndexFirst(match);
        return i != -1 ? sub(i) : null;
    }

    /**
     * counts subterms matching the supplied op
     */
    default int count(Op matchingOp) {
        return count(x -> x.op() == matchingOp);
    }

    /**
     * structure hash bitvector
     */
    int structure();

    /**
     * average of complexity and volume
     */
    default float voluplexity() {
        return (complexity() + volume()) / 2f;
    }


    /**
     * (first-level only, non-recursive)
     * if contained within; doesnt match this target (if it's a target);
     * false if target is atomic since it can contain nothing
     */
    boolean contains(Term t);

    default boolean containsNeg(Term x) {
        return contains(x.neg());
    }

    boolean hasXternal();

    default /* final */boolean containsRecursively(Term t) {
        return containsRecursively(t, (x) -> true);
    }

    default /* final */boolean containsRecursively(Term t, Predicate<Term> inSubtermsOf) {
        return !impossibleSubTerm(t) && containsRecursively(t, false, inSubtermsOf);
    }

    /**
     * if root is true, the root()'s of the terms will be compared
     */
    boolean containsRecursively(Term t, boolean root, Predicate<Term> inSubtermsOf);


    default boolean hasAll(int structuralVector) {
        return Op.has(structure(), structuralVector, true);
    }

    default boolean hasAny(int structuralVector) {
        return Op.has(structure(), structuralVector, false);
    }

//    default boolean hasAny(Op... oo) {
//        if (oo.length == 0)
//            return false;
//
//        int checkStruct = 0;
//        for (Op o : oo)
//            checkStruct |= o.bit;
//
//        return hasAny(checkStruct);
//    }

    default /* final */ boolean has(/*@NotNull*/ Op op) {
        return hasAny(op.bit);
    }

    default boolean hasVarIndep() { return hasAny(Op.VAR_INDEP.bit); }

    default boolean hasVarDep() {
        return hasAny(Op.VAR_DEP.bit);
    }

    default boolean hasVarQuery() {
        return hasAny(Op.VAR_QUERY.bit);
    }

    default boolean hasVarPattern() {
        return hasAny(Op.VAR_PATTERN.bit);
    }


    default boolean impossibleSubTerm(Termlike target) {
        return impossibleSubStructure(target.structure()) || impossibleSubVolume(target.volume());
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


    /**
     * stream of each subterm
     */
    default Stream<Term> subStream() {
        int subs = subs();
        switch (subs) {
            case 0:
                return Stream.empty();
            case 1:
                return Stream.of(sub(0));
            case 2:
                return Stream.of(sub(0), sub(1));
            case 3:
                return Stream.of(sub(0), sub(1), sub(2));
            default:
                return IntStream.range(0, subs).mapToObj(this::sub);
        }
    }


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
     * an array of the subterms, which an implementation may allow
     * direct access to its internal array which if modified will
     * lead to disaster. by default, it will call 'toArray' which
     * guarantees a clone. override with caution
     */
    default Term[] arrayShared() {
        return arrayClone();
    }

    /**
     * an array of the subterms
     * this is meant to be a clone always
     */
    default Term[] arrayClone() {
        int s = subs();
        switch (s) {
            case 0:
                return Op.EmptyTermArray;
            case 1:
                return new Term[]{sub(0)};
            case 2:
                return new Term[]{sub(0), sub(1)};
            default:
                return arrayClone(new Term[s], 0, s);
        }
    }

    default Term[] arrayClone(Term[] target) {
        return arrayClone(target, 0, subs());
    }

    default Term[] arrayClone(Term[] target, int from, int to) {


        for (int i = from, j = 0; i < to; i++, j++)
            target[j] = this.sub(i);

        return target;
    }


    /**
     * return whether a subterm op at an index is an operator.
     */
    default boolean subIs(int i, Op o) {
        return sub(i).op() == o;
    }

    /**
     * return whether a subterm op at an index is an operator.
     * if there is no subterm or the index is out of bounds, returns false.
     */
    default boolean subIsOrOOB(int i, Op o) {
        Term x = sub(i, null);
        return x != null && x.op() == o;
    }


    /**
     * structure of the first layer (surface) only
     */
    default int structureSurface() {
        return intifyShallow((s, x) -> s | x.opBit(), 0);
    }

    /**
     * immutable internability
     */
    default boolean these() {
        throw new TODO();
    }

}

