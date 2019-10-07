package nars.term.atom;

import nars.$;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Compound;
import nars.term.Neg;
import nars.term.Term;
import nars.term.Termlike;
import nars.term.util.transform.TermTransform;
import org.eclipse.collections.api.block.function.primitive.IntObjectToIntFunction;
import org.eclipse.collections.api.list.primitive.ByteList;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static nars.Op.EmptySubterms;
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
    default boolean containsRecursively(Term t, boolean root, Predicate<Term> inSubtermsOf) {
        return false;
    }

    @Override
    default boolean hasXternal() {
        return false;
    }


    @Override
    default boolean equalsRoot(Term x) {
        return x instanceof Atomic && equals(x);
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
        return t instanceof Neg && equals(t.unneg());
    }

    @Override
    default /* final */ Subterms subterms() { return EmptySubterms; }

    @Override
    default Term normalize(byte offset) {
        return this;
    }

    @Override
    default boolean hasAny(int structuralVector) {
        return isAny(structuralVector);
    }
    @Override
    default boolean hasAll(int structuralVector) {
        return opBit() == structuralVector;
    }

    @Override
    default Term concept() {
        //return Op.terms.concept(this);
        return this;
    }

    @Override
    default Term root() { return this; }


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
        return intifyShallow(reduce, v);
    }

    static Atom atom(String id) {
        return (Atom)the(id);
    }

    @Nullable static Atomic the(char c) {
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

        return Atom.AtomChar.chars[c];
    }

    /*@NotNull*/
    static Atomic the(String id) {
        int l = id.length();
        if (l <= 0)
            throw new RuntimeException("attempted construction of zero-length Atomic id");

        if (l == 1) {
            char c = id.charAt(0);
            Atomic ac = the(c);
            /*if (ac!=null)*/ return ac;
        }

        switch (id) {
            case "true":
                return Bool.True;
            case "false":
                return Bool.False;
            case "null":
                return Null;
            default: {
                if (quoteable(id, l)) {

//                    if (l > 1 /* already handled single digit cases in the above switch */ && Character.isDigit(id.charAt(0))) {
//
//                        int i = Texts.i(id, MIN_VALUE);
//                        if (i != MIN_VALUE)
//                            return Int.the(i);
//                    }

                    return $.quote(id);
                } else {
                    Atom.validateAtomID(id);
                    return Op.terms.atom(id);
                }
            }
        }

    }

    @Override
    String toString();

    /** byte[] representation */
    byte[] bytes();


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

    @Override default /* final */ void recurseTerms(Consumer<Term> each) {
        each.accept(this);
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
    default int count(Op matchingOp) {
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
    default boolean containsAny(Subterms ofThese) { return false; }
    @Override
    default boolean containsAll(Subterms ofThese) { return false; }

    @Override
    default boolean containsInstance(Term t) { return false; }

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
    default /* final */ Term sub(int i, Term ifOutOfBounds) {
        return ifOutOfBounds;
    }

    @Override
    default /* final */ Term sub(int i) {
        throw new ArrayIndexOutOfBoundsException();
    }

    @Override
    @Nullable
    default Term subPath(int start, int end, byte... path) {
        if (start==0 && start==end) return this;
        else return null;
    }

    @Override
    @Nullable
    default Term subPath(ByteList path, int start, int end) {
        if (start==0 && start==end) return this;
        else return null;
    }

    @Override
    default /* final */ int structure() {
        return opBit();
    }

    @Override
    default /* final */ int structureSurface() {
        return opBit();
    }

    /**
     * determines if the string is invalid as an unquoted target according to the characters present
     * assumes len > 0
     */
    private static boolean quoteable(CharSequence t, int len) {

        char t0 = t.charAt(0);
        
        if (Character.isDigit(t0))
            return true;

        if ((t0 == '\"') && (t.charAt(len - 1) == '\"'))
            return false;

        for (int i = 0; i < len; i++) {
            if (!Atom.isValidAtomChar(t.charAt(i)))
                return true;
        }

        return false;
    }


    default int height() { return 1; }

    default Term transform(TermTransform t) {
        return t.applyAtomic(this); //force unbuffered transform
    }
//    default Term transform(TermTransform t, TermBuffer b, int volMax) {
//        return t.apply(this); //force unbuffered transform
//    }

    /** returns non-zero, positive value if this term has an INTrinsic representation */
	default short intrin() {
        return 0;
    }


}
