package nars.term;

import nars.Op;
import org.eclipse.collections.api.block.function.primitive.IntObjectToIntFunction;
import org.eclipse.collections.api.block.predicate.primitive.ObjectIntPredicate;

import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static nars.Op.*;

/**
 * something which is like a term but isnt quite,
 * like a subterm container
 * <p>
 * Features exhibited by, and which can classify terms
 * and termlike productions
 */
public interface Termlike {


    Term sub(int i);


    /**
     * number of subterms. if atomic, size=0
     */
    int subs();

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

      /** only 1-layer (shallow, non-recursive) */
    default int sum(ToIntFunction<Term> value) {
        int x = 0;
        int s = subs();
        for (int i = 0; i < s; i++) {
            x += value.applyAsInt(sub(i));
        }
        return x;
    }

    /**
     * structure hash bitvector
     */
    default int structure() {
        return intifyShallow((s, x) -> x == null ? 0 : s | x.structure(), 0);
    }

    /**
     * average of complexity and volume
     */
    default float voluplexity() {
        return (complexity() + volume()) / 2f;
    }


    /**
     * (first-level only, non-recursive)
     * if contained within; doesnt match this term (if it's a term);
     * false if term is atomic since it can contain nothing
     */
    default boolean contains(Term t) {
        return !impossibleSubTerm(t) && OR(t::equals);
    }
    default boolean containsNeg(Term t) {
        if (t.op()==NEG)
            return contains(t.unneg());
        else
            return hasAny(NEG) && contains(t.neg());
    }

    /**
     * shouldnt need overrridden
     */
    default boolean containsRecursively(Term t) {
        return containsRecursively(t, (x) -> true);
    }

    default boolean containsRecursively(Term t, Predicate<Term> inSubtermsOf) {
        return !impossibleSubTerm(t) && containsRecursively(t, false, inSubtermsOf);
    }

    /**
     * if root is true, the root()'s of the terms will be compared
     */
    boolean containsRecursively(Term t, boolean root, Predicate<Term> inSubtermsOf);


    default boolean hasAll(int structuralVector) {
        return Op.hasAll(structure(), structuralVector)
                &&
               (((structuralVector & VAR_PATTERN.bit) == 0) || varPattern() > 0); //HACK since VAR_PATTERN will not appear structure vectors
    }

    default boolean hasAny(int structuralVector) {
        return Op.hasAny(structure(), structuralVector)
                ||
               (((structuralVector & VAR_PATTERN.bit) == 1) && varPattern() > 0); //HACK since VAR_PATTERN will not appear structure vectors
    }

    /** has special handling for VAR_PATTERN */
    default boolean hasAny(Op... oo) {
        boolean checkVarPattern = false;
        int checkStruct = 0;
        for (Op o : oo) {
            if (o == VAR_PATTERN)
                checkVarPattern = true; //check last
            else
                checkStruct |= o.bit;
        }
        return (checkStruct != 0 && hasAny(checkStruct)) || (checkVarPattern && varPattern() > 0);
    }

    /**
     * tests if contains a term in the structural hash
     * WARNING currently this does not detect presence of pattern variables
     */
    default boolean hasAny(/*@NotNull*/ Op op) {
        return op != VAR_PATTERN ? hasAny(op.bit) : varPattern() > 0;
    }

    default boolean hasVarIndep() {
        return varIndep() > 0;
    }

    default boolean hasVarDep() {
        return varDep() > 0;
    }

    default boolean hasVarQuery() {
        return varQuery() > 0;
    }

    default boolean impossibleSubTerm(/*@NotNull*/Termlike target) {
        //if the OR produces a different result compared to subterms,
        // it means there is some component of the other term which is not found
        //return ((possibleSubtermStructure | existingStructure) != existingStructure);
        return !hasAll(target.structure()) || impossibleSubTermVolume(target.volume());
    }

    /**
     * if it's larger than this term it can not be equal to this.
     * if it's larger than some number less than that, it can't be a subterm.
     */
    default boolean impossibleSubTermOrEqualityVolume(int otherTermsVolume) {
        return otherTermsVolume > volume();
    }

//    default boolean levelValid(int nal) {
//
//        if (nal >= 8) return true;
//
//        int mask = Op.NALLevelEqualAndAbove[nal];
//        return (structure() | mask) == mask;
//    }


    /**
     * tries to get the ith subterm (if this is a TermContainer),
     * or of is out of bounds or not a container,
     * returns the provided ifOutOfBounds
     */
    Term sub(int i, Term ifOutOfBounds);

    default boolean impossibleSubTermVolume(int otherTermVolume) {
        return otherTermVolume > volume() - subs();
    }


    default boolean impossibleSubTermOrEquality(/*@NotNull*/Term target) {
        return ((!hasAll(target.structure())) ||
                (impossibleSubTermOrEqualityVolume(target.volume())));
    }

    /**
     * returns true if evaluates true for any terms
     *
     * @param p
     */
    default boolean OR(/*@NotNull*/ Predicate<Term> p) {
        int s = subs();
        for (int i = 0; i < s; i++) {
            if (p.test(sub(i)))
                return true;
        }
        return false;
    }

    /**
     * returns true if evaluates true for all terms
     *
     * @param p
     */
    default boolean AND(/*@NotNull*/ Predicate<Term> p) {
        int s = subs();
        for (int i = 0; i < s; i++)
            if (!p.test(sub(i)))
                return false;
        return true;
    }

    /** supplies the current index as 2nd lambda argument */
    default boolean ANDwith(/*@NotNull*/ ObjectIntPredicate<Term> p) {
        int s = subs();
        for (int i = 0; i < s; i++)
            if (!p.accept(sub(i), i))
                return false;
        return true;
    }

    /** supplies the current index as 2nd lambda argument */
    default boolean ORwith(/*@NotNull*/ ObjectIntPredicate<Term> p) {
        int s = subs();
        for (int i = 0; i < s; i++)
            if (p.accept(sub(i), i))
                return true;
        return false;
    }

    default boolean ANDrecurse(/*@NotNull*/ Predicate<Term> p) {
        int s = subs();
        for (int i = 0; i < s; i++)
            if (!sub(i).ANDrecurse(p))
                return false;
        return true;
    }

    default boolean ORrecurse(/*@NotNull*/ Predicate<Term> p) {
        int s = subs();
        for (int i = 0; i < s; i++)
            if (sub(i).ORrecurse(p))
                return true;
        return false;
    }

    default boolean isSorted() {
        int s = subs();
        if (s < 2) return true;

        //TODO save the last in a variable to avoid sub(i) call
        //Term prev = sub(0);
        for (int i = 1; i < s; i++) {
            if (sub(i - 1).compareTo(sub(i)) >= 0)
                return false;
        }
        return true;
    }

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

    void recurseTerms(/*@NotNull*/Consumer<Term> v);

//    /**
//     * note: if the function returns null, null will not be added to the result set
//     */
//    /*@NotNull*/
//    default Set<Term> subsUnique(/*@NotNull*/ Function<Term, Term> each) {
//        Set<Term> r = new HashSet(subs());
//        int s = subs();
//        for (int i = 0; i < s; i++) {
//            Term e = each.apply(sub(i));
//            if (e != null)
//                r.add(e);
//        }
//        return r;
//    }

    /**
     * total # of variables, excluding pattern variables
     * preferably use hasVar... methods to test presence
     */
    default int vars() {
        return hasVars() ? sum(Term::vars) : 0;
    }

    /**
     * whether contains any variables, excluding pattern variables
     */
    default boolean hasVars() {
        return hasAny(VAR_INDEP.bit | VAR_DEP.bit | VAR_QUERY.bit);
    }

    /**
     * # of contained dependent variables in subterms (1st layer only)
     */
    default int varDep() {
        return hasAny(VAR_DEP) ? sum(Term::varDep) : 0;
    }

    default int varIndep() {
        return hasAny(VAR_INDEP) ? sum(Term::varIndep) : 0;
    }

    default int varQuery() {
        return hasAny(VAR_QUERY) ? sum(Term::varQuery) : 0;
    }

    default int varPattern() {
        return /*hasAny(Op.VAR_PATTERN) ? */sum(Term::varPattern); //since pattern vars are not included in structure, dont check it
    }


    /**
     * counts subterms matching the predicate
     */
    default int subs(Predicate<Term> match) {
        return intifyShallow((c, sub) -> match.test(sub) ? c + 1 : c, 0);
    }

    /** recursive, visits each component */
    default int intifyRecurse(IntObjectToIntFunction<Term> reduce, int v) {
        int n = subs();
        for (int i = 0; i < n; i++)
            v = sub(i).intifyRecurse(reduce, v); //recurse
        return v;
    }
   /** recursive, visits only 1 layer deep, and not the current superterm if compound */
    default int intifyShallow(IntObjectToIntFunction<Term> reduce, int v) {
        int n = subs();
        for (int i = 0; i < n; i++)
            v = reduce.intValueOf(v, sub(i)); //non-recurse
        return v;
    }
//    default <X> X objectify(BiFunction<X, Term, X> reduce) {
//        return objectify(reduce, null);
//    }
//
//    default <X> X objectify(BiFunction<X, Term, X> reduce, X v) {
//        int n = subs();
//        for (int i = 0; i < n; i++)
//            v = reduce.apply(v, sub(i, Null));
//        return v;
//    }

    /**
     * counts subterms matching the supplied op
     */
    default int subs(Op matchingOp) {
        return subs(x -> x.op() == matchingOp);
    }

    /**
     * return whether a subterm op at an index is an operator.
     * if there is no subterm or the index is out of bounds, returns false.
     */
    default boolean subIs(int i, Op o) {
        Term x = sub(i, null);
        return x != null && x.op() == o;
    }



}

