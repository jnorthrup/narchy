package nars.term.atom;

import nars.$;
import nars.Op;
import nars.subterm.Subterms;
import nars.term.Compound;
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

import static nars.term.atom.IdempotentBool.Null;

public interface Atomic extends Term {

    static @Nullable Atomic the(char c) {
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

    static Atom atom(String id) {
        return (Atom)the(id);
    }

    /**
     * determines if the string is invalid as an unquoted target according to the characters present
     * assumes len > 0
     */
    static boolean quoteable(CharSequence t, int len) {

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

    static Atomic the(String id) {
        int l = id.length();
        if (l <= 0)
            throw new RuntimeException("attempted construction of zero-length Atomic id");

        if (l == 1)
            return Atomic.the(id.charAt(0));

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

                    return $.INSTANCE.quote(id);
                } else {
                    Atom.validateAtomID(id);
                    return Op.terms.atom(id);
                }
            }
        }

    }

    @Override
    boolean containsRecursively(Term t, boolean root, Predicate<Term> inSubtermsOf);

    @Override
    boolean hasXternal();

    @Override
    boolean equalsRoot(Term x);

    @Override
    int vars();

    @Override
    int varDep();

    @Override
    int varIndep();

    @Override
    int varQuery();

    @Override
    int varPattern();

    @Override
    boolean equalsNeg(Term t);

    @Override
    Subterms subterms();

    @Override
    Term normalize(byte offset);

    @Override
    boolean hasAny(int structuralVector);

    @Override
    boolean hasAll(int structuralVector);

    @Override
    Term concept();

    @Override
    Term root();

    @Override
    Term replace(Term from, Term to);

    @Override
    int intifyShallow(int v, IntObjectToIntFunction<Term> reduce);

    @Override
    int intifyRecurse(int v, IntObjectToIntFunction<Term> reduce);

    @Override
    String toString();

    /** byte[] representation */
    byte[] bytes();

    boolean recurseTerms(Predicate<Term> inSuperCompound, Predicate<Term> whileTrue, @Nullable Compound superterm);

    @Override
    boolean recurseTerms(Predicate<Compound> aSuperCompoundMust, BiPredicate<Term, Compound> whileTrue, @Nullable Compound superterm);

    @Override
    boolean recurseTermsOrdered(Predicate<Term> inSuperCompound, Predicate<Term> whileTrue, Compound parent);

    @Override
    boolean recurseTermsOrdered(Predicate<Term> whileTrue);

    @Override
    void recurseTerms(BiConsumer<Term, Compound> each);

    @Override
    void recurseTerms(Consumer<Term> each);

    @Override
    void appendTo(Appendable w) throws IOException;

    @Override
    int subs();

    @Override
    boolean impossibleSubVolume(int otherTermVolume);

    @Override
    boolean impossibleSubStructure(int structure);

    @Override
    boolean impossibleSubTerm(Termlike target);

    @Override
    boolean contains(Term t);

    @Override
    boolean containsInstance(Term t);

    @Override
    boolean isCommutative();

    @Override
    int complexity();

    @Override
    int volume();

    @Override
    float voluplexity();

    @Override
    Term sub(int i, Term ifOutOfBounds);

    @Override
    Term sub(int i);

    @Override
    @Nullable Term subPath(int start, int end, byte... path);

    @Override
    @Nullable Term subPath(ByteList path, int start, int end);

    @Override
    int structure();

    @Override
    int structureSurface();

    int height();

    Term transform(TermTransform t);

    abstract short intrin();
}
