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
import java.util.stream.IntStream;

import static nars.Op.EmptySubterms;
import static nars.term.atom.Bool.Null;


/**
 * Base class for Atomic types.
 */
public abstract class Atomic implements Term {


    @Override
    public final boolean containsRecursively(Term t, boolean root, Predicate<Term> inSubtermsOf) {
        return false;
    }

    @Override
    public final boolean hasXternal() {
        return false;
    }


    @Override
    public boolean equalsRoot(Term x) {
        return this==x || (x instanceof Atomic && equals(x));
    }


    @Override
    public int vars() {
        return 0;
    }

    @Override
    public int varDep() {
        return 0;
    }

    @Override
    public int varIndep() {
        return 0;
    }

    @Override
    public int varQuery() {
        return 0;
    }

    @Override
    public int varPattern() {
        return 0;
    }

    @Override
    public boolean equalsNeg(Term t) {
        return t instanceof Neg && equals(t.unneg());
    }

    @Override
    public /* final */ Subterms subterms() { return EmptySubterms; }

    @Override
    public Term normalize(byte offset) {
        return this;
    }

    @Override
    public boolean hasAny(int structuralVector) {
        return isAny(structuralVector);
    }
    @Override
    public boolean hasAll(int structuralVector) {
        return opBit() == structuralVector;
    }

    @Override
    public Term concept() {
        //return Op.terms.concept(this);
        return this;
    }

    @Override
    public Term root() { return this; }


    @Override
    public Term replace(Term from, Term to) {
        return equals(from) ? to : this; 
    }


    @Override
    public int intifyShallow(IntObjectToIntFunction<Term> reduce, int v) {
        return reduce.intValueOf(v, this);
    }

    @Override
    public int intifyRecurse(IntObjectToIntFunction<Term> reduce, int v) {
        return intifyShallow(reduce, v);
    }

    public static Atom atom(String id) {
        return (Atom)the(id);
    }

    public static @Nullable Atomic the(char c) {
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

        return AtomChar.chars[c];
    }

    /*@NotNull*/
    public static Atomic the(String id) {
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
    public abstract String toString();

    /** byte[] representation */
    public abstract byte[] bytes();


    public boolean recurseTerms(Predicate<Term> inSuperCompound, Predicate<Term> whileTrue, @Nullable Compound superterm) {
        return whileTrue.test(this);
    }

    @Override
    public boolean recurseTerms(Predicate<Compound> aSuperCompoundMust, BiPredicate<Term, Compound> whileTrue, @Nullable Compound superterm) {
        return whileTrue.test(this, superterm);
    }

    @Override
    public boolean recurseTermsOrdered(Predicate<Term> inSuperCompound, Predicate<Term> whileTrue, Compound parent) {
        return whileTrue.test(this);
    }

    @Override
    public boolean recurseTermsOrdered(Predicate<Term> whileTrue) {
        return whileTrue.test(this);
    }

    /** convenience, do not override */
    @Override
    public void recurseTerms(BiConsumer<Term, Compound> each) {
        each.accept(this, null);
    }

    @Override
    public /* final */ void recurseTerms(Consumer<Term> each) {
        each.accept(this);
    }


    @Override
    public void appendTo(Appendable w) throws IOException {
        w.append(toString());
    }

    /**
     * number of subterms; for atoms this must be zero
     */
    @Override
    public int subs() {
        return 0;
    }


    /**
     * atoms contain no subterms so impossible for anything to fit "inside" it
     */
    @Override
    public boolean impossibleSubVolume(int otherTermVolume) {
        return true;
    }

    @Override
    public boolean impossibleSubStructure(int structure) { return true; }

    @Override
    public boolean impossibleSubTerm(Termlike target) {
        return true;
    }

//    @Override
//    default boolean impossibleSubTermOrEqualityVolume(int otherTermsVolume) {
//        return otherTermsVolume != 1;
//    }

    @Override
    public boolean contains(Term t) {
        return false;
    }

    @Override
    public boolean containsInstance(Term t) { return false; }

    @Override
    public boolean isCommutative() {
        return false;
    }

    @Override
    public int complexity() { return 1; }

    @Override
    public int volume() {
        return 1;
    }

    @Override
    public float voluplexity() {
        return 1;
    }

    @Override
    public /* final */ Term sub(int i, Term ifOutOfBounds) {
        return ifOutOfBounds;
    }

    @Override
    public /* final */ Term sub(int i) {
        throw new ArrayIndexOutOfBoundsException();
    }

    @Override
    public @Nullable Term subPath(int start, int end, byte... path) {
        if (start==0 && start==end) return this;
        else return null;
    }

    @Override
    public @Nullable Term subPath(ByteList path, int start, int end) {
        if (start==0 && start==end) return this;
        else return null;
    }

    @Override
    public /* final */ int structure() {
        return opBit();
    }

    @Override
    public /* final */ int structureSurface() {
        return opBit();
    }

    /**
     * determines if the string is invalid as an unquoted target according to the characters present
     * assumes len > 0
     */
    public static boolean quoteable(CharSequence t, int len) {

        char t0 = t.charAt(0);
        
        if (Character.isDigit(t0))
            return true;

        if ((t0 == '\"') && (t.charAt(len - 1) == '\"'))
            return false;

        return IntStream.range(0, len).anyMatch(i -> !Atom.isValidAtomChar(t.charAt(i)));
    }


    public int height() { return 1; }

    public /* final */ Term transform(TermTransform t) {
        return t.applyAtomic(this); //force unbuffered transform
    }

//    default Term transform(TermTransform t, TermBuffer b, int volMax) {
//        return t.apply(this); //force unbuffered transform
//    }

    /** returns non-zero, positive value if this term has an INTrinsic representation */
	public short intrin() {
        return 0;
    }


}
