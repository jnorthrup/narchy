package nars.term.atom;

import com.google.common.io.ByteArrayDataOutput;
import jcog.Texts;
import nars.$;
import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termlike;
import nars.term.util.transform.Retemporalize;
import org.eclipse.collections.api.block.function.primitive.IntObjectToIntFunction;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.lang.Integer.MIN_VALUE;
import static nars.Op.NEG;
import static nars.term.atom.Bool.Null;


/**
 * Base class for Atomic types.
 */
public interface Atomic extends Term {


    @Override
    default boolean containsRecursively(Term t) {
        return false;
    }

    @Override
    default boolean containsRecursively(Term t, Predicate<Term> inSubtermsOf) {
        return false;
    }

    @Override
    default boolean containsNeg(Term x) {
        return false;
    }

    @Override
    default boolean containsRecursively(Term t, boolean root, Predicate<Term> inSubtermsOf) {
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
    default boolean equalsRoot(Term x) {
        return equals(x);
    }

    @Override
    default Term[] arrayShared() {
        return Op.EmptyTermArray;
    }

    @Override
    default Term[] arrayClone() {
        return Op.EmptyTermArray;
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
    default boolean equalsNeg(Term t) {
        return t.op() == NEG && equals(t.unneg());
    }



    @Override
    default Term normalize(byte offset) {
        return this;
    }

    //    @Override
//    default boolean equalsNegRoot(Term t) {
//        return equalsNeg(t.root());
//    }

    @Override
    default Term replace(Map<? extends Term, Term> m) {
        Term y = m.get(this); 
        return y != null ? y : this;
    }

    @Override
    default Term replace(Term from, Term to) {
        return equals(from) ? to : this; 
    }


    @Override
    default int intifyShallow(IntObjectToIntFunction<Term> reduce, int v) {
        return reduce.intValueOf(v, this);
    }

    @Override
    default int intifyRecurse(IntObjectToIntFunction<Term> reduce, int v) {
        return reduce.intValueOf(v, this);
    }

    /*@NotNull*/
    static Atomic the(String id) {
        int l = id.length();
        assert (l > 0) : "attempted zero-length Atomic id";

        
        if (l == 1) {
            char c = id.charAt(0);
            switch (c) {
                case Op.VarAutoSym:
                    return Op.VarAuto;
                case Op.NullSym:
                    return Null;

                case Op.imIntSym:
                    return Op.ImgInt;
                case Op.imExtSym:
                    return Op.ImgExt;
                case '0':
                    return Int.pos[0];
                case '1':
                    return Int.pos[1];
                case '2':
                    return Int.pos[2];
                case '3':
                    return Int.pos[3];
                case '4':
                    return Int.pos[4];
                case '5':
                    return Int.pos[5];
                case '6':
                    return Int.pos[6];
                case '7':
                    return Int.pos[7];
                case '8':
                    return Int.pos[8];
                case '9':
                    return Int.pos[9];
            }
        }

        switch (id) {
            case "true":
                return Bool.True;
            case "false":
                return Bool.False;
            case "null":
                return Null;
            default: {
                if (isQuoteNecessary(id, l)) {

                    if (l > 1 /* already handled single digit cases in the above switch */ && Character.isDigit(id.charAt(0))) {

                        int i = Texts.i(id, MIN_VALUE);
                        if (i != MIN_VALUE)
                            return Int.the(i);
                    }

                    return $.quote(id);
                } else
                    return new Atom(id);
            }
        }

    }

    @Override
    String toString();

    /** byte[] representation */
    byte[] bytes();

    @Override
    default void appendTo(ByteArrayDataOutput out) {
        out.write(bytes());
    }

    default boolean recurseTerms(Predicate<Term> inSuperCompound, Predicate<Term> whileTrue, @Nullable Compound superterm) {
        return whileTrue.test(this);
    }

    @Override
    default boolean recurseTerms(Predicate<Compound> aSuperCompoundMust, BiPredicate<Term, Compound> whileTrue, @Nullable Compound superterm) {
        return whileTrue.test(this, superterm);
    }

    @Override
    default boolean recurseTermsOrdered(Predicate<Term> inSuperCompound, Predicate<Term> whileTrue, Compound parent) {
        return whileTrue.test(this);
    }

    @Override
    default boolean recurseTermsOrdered(Predicate<Term> whileTrue) {
        return whileTrue.test(this);
    }

    /** convenience, do not override */
    @Override default void recurseTerms(BiConsumer<Term, Compound> each) {
        each.accept(this, null);
    }

    /** convenience, do not override */
    @Override default void recurseTerms(Consumer<Term> each) {
        each.accept(this);
    }

    @Override
    default boolean ANDrecurse(Predicate<Term> v) {
        return v.test(this);
    }

    @Override
    default boolean ORrecurse(Predicate<Term> v) {
        return v.test(this);
    }

    @Override
    default boolean OR(Predicate<Term> v) {
        return v.test(this);
    }

    @Override
    default boolean AND(Predicate<Term> v) {
        return v.test(this);
    }


    @Override
    default void appendTo(Appendable w) throws IOException {
        w.append(toString());
    }

    /**
     * number of subterms; for atoms this must be zero
     */
    @Override
    default int subs() {
        return 0;
    }
    @Override
    default int subs(Op matchingOp) {
        return 0;
    }

    /**
     * atoms contain no subterms so impossible for anything to fit "inside" it
     */
    @Override
    default boolean impossibleSubVolume(int otherTermVolume) {
        return true;
    }

    @Override
    default boolean impossibleSubStructure(int structure) { return true; }

    @Override
    default boolean impossibleSubTerm(Termlike target) {
        return true;
    }

//    @Override
//    default boolean impossibleSubTermOrEqualityVolume(int otherTermsVolume) {
//        return otherTermsVolume != 1;
//    }

    @Override
    default boolean contains(Term t) {
        return false;
    }


    @Override
    default boolean isCommutative() {
        return false;
    }

    @Override
    default int complexity() { return 1; }

    @Override
    default int volume() {
        return 1;
    }

    @Override
    default float voluplexity() {
        return 1;
    }

    @Override
    default Term sub(int i, Term ifOutOfBounds) {
        return ifOutOfBounds;
    }

    @Override
    default Term sub(int i) {
        throw new ArrayIndexOutOfBoundsException();
    }

    @Override
    default int structure() {
        return opBit();
    }

    @Override
    default int structureSurface() {
        return structure();
    }

    @Override
    default Stream<Term> subStream() {
        return Stream.empty();
    }

    @Override
    default boolean these() {
        return the();
    }

    @Override
    default boolean subIs(int i, Op o) {
        return false;
    }


    /**
     * determines if the string is invalid as an unquoted target according to the characters present
     * assumes len > 0
     */
    private static boolean isQuoteNecessary(CharSequence t, int len) {

        if ((t.charAt(0) == '\"') && (t.charAt(len - 1) == '\"'))
            return false;

        for (int i = 0; i < len; i++) {
            if (!Atom.isValidAtomChar(t.charAt(i)))
                return true;
        }

        return false;
    }


    static boolean equals(Atomic x, Object y) {
        return //(x == y) ||
               ((y instanceof Atomic) &&
               (x.hashCode() == y.hashCode()) &&
               Arrays.equals(x.bytes(), ((Atomic)y).bytes()));
    }

    default int height() { return 1; }


}
