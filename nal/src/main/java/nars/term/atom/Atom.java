package nars.term.atom;

import nars.Op;
import nars.The;
import nars.subterm.util.TermMetadata;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;

import static nars.Op.ATOM;

/**
 * default Atom implementation: wraps a String instance as closely as possible.
 * ideally this string is stored encoded in UTF8 byte[]'s
 */
public class Atom extends AtomicConst implements The {


    public Atom(byte[] b) {
        super(b);
    }

    protected Atom(String id) {
        super(ATOM, validateAtomID(id));
    }

    @NotNull
    private static String validateAtomID(String id) {
        if (id.isEmpty())
            throw new UnsupportedOperationException("Empty Atom ID");


        char c = id.charAt(0);
        switch (c) {
            case '+':
            case '-':
            case '0':
            case '1':
            case '2':
            case '3':
            case '4':
            case '5':
            case '6':
            case '7':
            case '8':
            case '9':

            case '^':
            case '?':
            case '%':
            case '#':
            case '$':
                throw new RuntimeException("invalid " + Atom.class + " name \"" + id + "\": leading character imitates another operation type");
        }

        //return id.intern();
        return id;
    }

    public final static int AtomString = Term.opX(ATOM, 1);

    @Override public int opX() {
        return AtomString;
    }

    @Override
    public final Op op() {
        return Op.ATOM;
    }

    @Override
    public final int structure() {
        return ATOM.bit;
    }



    @Override
    public void collectMetadata(TermMetadata.SubtermMetadataCollector s) {
       s.collectNonVar(ATOM, hash);
    }
}

