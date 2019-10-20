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
import static nars.term.atom.IdempotentBool.Null;


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
    public   Subterms subterms() { return EmptySubterms; }

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
    public int intifyShallow(int v, IntObjectToIntFunction<Term> reduce) {
        return reduce.intValueOf(v, this);
    }

    @Override
    public int intifyRecurse(int v, IntObjectToIntFunction<Term> reduce) {
        return intifyShallow(v, reduce);
    }

    public static Atom atom(String id) {
        return (Atom)the(id);
    }

    public static @Nullable Atomic the(char c) {
        @Nullable Atomic result = Op.VarAuto;
        boolean finished = false;
        switch (c) {
            case Op.VarAutoSym:
                finished = true;
                break;
            case Op.NullSym:
                result = Null;
                finished = true;
                break;

            case Op.imIntSym:
                result = Op.ImgInt;
                finished = true;
                break;
            case Op.imExtSym:
                result = Op.ImgExt;
                finished = true;
                break;
            case '0':
                result = IdempotInt.pos[0];
                finished = true;
                break;
            case '1':
                result = IdempotInt.pos[1];
                finished = true;
                break;
            case '2':
                result = IdempotInt.pos[2];
                finished = true;
                break;
            case '3':
                result = IdempotInt.pos[3];
                finished = true;
                break;
            case '4':
                result = IdempotInt.pos[4];
                finished = true;
                break;
            case '5':
                result = IdempotInt.pos[5];
                finished = true;
                break;
            case '6':
                result = IdempotInt.pos[6];
                finished = true;
                break;
            case '7':
                result = IdempotInt.pos[7];
                finished = true;
                break;
            case '8':
                result = IdempotInt.pos[8];
                finished = true;
                break;
            case '9':
                result = IdempotInt.pos[9];
                finished = true;
                break;
        }
        if (!finished) {
            result = AtomChar.chars[(int) c];
        }

        return result;
    }

    /*@NotNull*/
    public static Atomic the(String id) {
        int l = id.length();
        if (l <= 0)
            throw new RuntimeException("attempted construction of zero-length Atomic id");

        if (l == 1)
            return the(id.charAt(0));

        switch (id) {
            case "true":
                return IdempotentBool.True;
            case "false":
                return IdempotentBool.False;
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
    public   void recurseTerms(Consumer<Term> each) {
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
        return 1.0F;
    }

    @Override
    public   Term sub(int i, Term ifOutOfBounds) {
        return ifOutOfBounds;
    }

    @Override
    public   Term sub(int i) {
        throw new ArrayIndexOutOfBoundsException();
    }

    @Override
    public @Nullable Term subPath(int start, int end, byte... path) {
        return start == 0 && start == end ? this : null;
    }

    @Override
    public @Nullable Term subPath(ByteList path, int start, int end) {
        return start == 0 && start == end ? this : null;
    }

    @Override
    public   int structure() {
        return opBit();
    }

    @Override
    public   int structureSurface() {
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

        if (((int) t0 == (int) '\"') && ((int) t.charAt(len - 1) == (int) '\"'))
            return false;

        for (int i = 0; i < len; i++) {
            if (!Atom.isValidAtomChar(t.charAt(i))) {
                return true;
            }
        }
        return false;
    }


    public int height() { return 1; }

    public   Term transform(TermTransform t) {
        return t.applyAtomic(this); //force unbuffered transform
    }

//    default Term transform(TermTransform t, TermBuffer b, int volMax) {
//        return t.apply(this); //force unbuffered transform
//    }

    /** returns non-zero, positive value if this term has an INTrinsic representation */
	public short intrin() {
        return (short) 0;
    }


}
