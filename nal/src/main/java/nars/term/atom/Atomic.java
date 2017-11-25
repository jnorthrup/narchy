package nars.term.atom;

import com.google.common.io.ByteArrayDataOutput;
import jcog.Texts;
import nars.$;
import nars.Narsese;
import nars.Op;
import nars.index.term.TermContext;
import nars.term.Term;
import nars.term.Termlike;
import nars.term.transform.Retemporalize;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static java.lang.Integer.MIN_VALUE;


/**
 * Base class for Atomic types.
 */
public interface Atomic extends Term {

    @Override
    default boolean OR(Predicate<Term> v) {
        return v.test(this);
    }

    @Override
    default boolean AND(Predicate<Term> v) {
        return v.test(this);
    }

    @Override
    default boolean containsRecursively(Term t) {
        return false;
    }

    @Override
    default boolean containsRecursively(Term t, boolean root, Predicate<Term> inSubtermsOf) {
        return false;
    }

    @Override
    default boolean containsRecursively(Term t, Predicate<Term> inSubtermsOf) {
        return false;
    }

//    @Override
//    default boolean isDynamic() {
//        return false;
//    }

    @Override
    default boolean isTemporal() {
        return false;
    }

    @Override
    default boolean hasXternal() {
        return false;
    }

    @Override
    default @Nullable Term temporalize(Retemporalize r) {
        return this;
    }

    @Override
    default int vars() {
        return 0;
    }

    @Override
    default int varDep() {
        return 0;
    }

    @Override
    default int varIndep() {
        return 0;
    }

    @Override
    default int varQuery() {
        return 0;
    }

    @Override
    default int varPattern() {
        return 0;
    }

    @Override
    default Term replace(Map<Term, Term> m) {
        Term y = m.get(this); //atom substitutions
        return y != null ? y : this;
    }

    @Override
    default Term replace(Term from, Term to) {
        return equals(from) ? to : this; //atom substitution
    }



    /*@NotNull*/
    static Atomic the(String id) {
        int l = id.length();
        assert (l > 0) : "attempted zero-length Atomic id";

        //special cases
        if (l == 1) {
            char c = id.charAt(0);
            switch (c) {
                case Op.ImdexSym:
                    return Op.Imdex;
                case Op.NullSym:
                    return Op.Null;
                case Op.TrueSym:
                    return Op.True;
                case Op.FalseSym:
                    return Op.False;
                case '0':
                    return Int.digits[0];
                case '1':
                    return Int.digits[1];
                case '2':
                    return Int.digits[2];
                case '3':
                    return Int.digits[3];
                case '4':
                    return Int.digits[4];
                case '5':
                    return Int.digits[5];
                case '6':
                    return Int.digits[6];
                case '7':
                    return Int.digits[7];
                case '8':
                    return Int.digits[8];
                case '9':
                    return Int.digits[9];
            }
        }

        //TODO handle negative ints prefixed with '-'
        if (l > 1 /* already handled single digit cases in the above switch */ && Character.isDigit(id.charAt(0))) {
            //try to parse int
            int i = Texts.i(id, MIN_VALUE);
            if (i != MIN_VALUE)
                return Int.the(i); //parsed as integer, so
        }

        if (isQuoteNecessary(id))
            return $.quote(id);


        return new Atom(id);
    }

    @Override
    default Term evalSafe(TermContext context, int remain) {
        return remain <= 0 ? null : context.applyTermIfPossible(this);
    }

    @Override
    String toString();

    /** byte[] representation */
    byte[] bytes();

    @Override
    default void append(ByteArrayDataOutput out) {
        out.write(bytes());
    }


    @Override
    default void recurseTerms(Consumer<Term> v) {
        v.accept(this);
    }

    @Override
    default boolean ANDrecurse(Predicate<Term> v) {
        return AND(v);
    }

    @Override
    default boolean ORrecurse(Predicate<Term> v) {
        return AND(v);
    }

    @Override
    default void append(Appendable w) throws IOException {
        w.append(toString());
    }

    /**
     * number of subterms; for atoms this must be zero
     */
    @Override
    default int subs() {
        return 0;
    }

    /**
     * atoms contain no subterms so impossible for anything to fit "inside" it
     */
    @Override
    default boolean impossibleSubTermVolume(int otherTermVolume) {
        return true;
    }

    @Override
    default boolean impossibleSubTermOrEquality(Term target) {
        return !equals(target);
    }

    @Override
    default boolean impossibleSubTerm(Termlike target) {
        return true;
    }

    @Override
    default boolean impossibleSubTermOrEqualityVolume(int otherTermsVolume) {
        return otherTermsVolume != 1;
    }

    @Override
    default boolean contains(Term t) {
        return false;
    }

    @Override
    default boolean isCommutative() {
        return false;
    }


    /**
     * default volume = 1
     */
    @Override
    default int volume() {
        return 1;
    }

    @Override
    default Term sub(int i, Term ifOutOfBounds) {
        //no superterms to select
        return ifOutOfBounds;
    }

    @Override
    default int structure() {
        return op().bit;
    }

    @Override
    default boolean subIs(int i, Op o) {
        return false;
    }

    /**
     * determines if the string is invalid as an unquoted term according to the characters present
     */
    static boolean isQuoteNecessary(CharSequence t) {
        int len = t.length();

        if (len > 1 && (t.charAt(0) == '\"') && (t.charAt(len - 1) == '\"'))
            return false; //already quoted


        for (int i = 0; i < len; i++) {
            if (!Narsese.isValidAtomChar(t.charAt(i)))
                return true;
        }

        return false;
    }


    static boolean equals(Atomic x, Atomic y) {
        if (x.hashCode() != y.hashCode())
            return false;

        return Arrays.equals(x.bytes(), y.bytes());
    }
    
}
