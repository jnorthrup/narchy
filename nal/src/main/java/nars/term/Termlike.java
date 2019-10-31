package nars.term;

import nars.Op;
import org.eclipse.collections.api.block.function.primitive.IntObjectToIntFunction;

import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

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
    Term subSafe(int i);

    /**
     * tries to get the ith subterm (if this is a TermContainer),
     * or of is out of bounds or not a container,
     * returns the provided ifOutOfBounds
     */
    Term sub(int i, Term ifOutOfBounds);


    /**
     * number of subterms. if atomic, size=0
     */
    int subs();

    /**
     * recursion height; atomic=1, compound>1
     */
    int height();

    /**
     * syntactic volume = 1 + total volume of terms = complexity of subterms - # variable instances
     */
    int volume();


    /**
     * syntactic complexity 1 + total complexity number of leaf terms, excluding variables which have a complexity of zero
     */
    int complexity();

    /**
     * only 1-layer (shallow, non-recursive)
     */
    int sum(ToIntFunction<Term> value);

    /**
     * only 1-layer (shallow, non-recursive)
     */
    int max(ToIntFunction<Term> value);


    /**
     * non-recursive, visits only 1 layer deep, and not the current if compound
     */
    int intifyShallow(int v, IntObjectToIntFunction<Term> reduce);

    int intifyRecurse(int _v, IntObjectToIntFunction<Term> reduce);

    boolean recurseTerms(Predicate<Term> inSuperCompound, Predicate<Term> whileTrue, Compound parent);

    boolean recurseTerms(Predicate<Compound> aSuperCompoundMust, BiPredicate<Term, Compound> whileTrue, Compound parent);


    /**
     * structure hash bitvector
     */
    int structure();

    /**
     * average of complexity and volume
     */
    float voluplexity();


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


    boolean hasAll(int structuralVector);

    boolean hasAny(int structuralVector);

    /* final */ boolean hasAny(/*@NotNull*/ Op op);

    /* final */ boolean hasAllAny(/*@NotNull*/ int all, int any);

    boolean hasVarIndep();

    boolean hasVarDep();

    boolean hasVarQuery();

    boolean hasVarPattern();


    boolean impossibleSubStructure(int structure);

    boolean impossibleSubVolume(int otherTermVolume);

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


    int vars();

    boolean hasVars();

    /**
     * # of contained dependent variables in subterms (1st layer only)
     */
    int varDep();

    int varIndep();

    int varQuery();

    int varPattern();


    /**
     * structure of the first layer (surface) only
     */
    int structureSurface();

    /**
     * immutable internability
     */
    boolean these();

    int addAllTo(Term[] t, int offset);

    int subStructure();
}

