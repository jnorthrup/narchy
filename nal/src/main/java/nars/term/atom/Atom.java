package nars.term.atom;

import nars.Op;
import nars.The;
import nars.term.Term;

import static nars.Op.*;

/**
 * default Atom implementation: wraps a String instance as closely as possible.
 * ideally this string is stored encoded in UTF8 byte[]'s
 */
public class Atom extends AbstractAtomic implements The {


    public Atom(byte[] b) {
        super(b);
    }

    protected Atom(String id) {
        super(ATOM, validateAtomID(id));
    }

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

        
        return id;
    }

    public final static int AtomString = Term.opX(ATOM, 1);

    public static boolean isValidAtomChar(char x) {

        
        switch (x) {
            case ' ':
            case ARGUMENT_SEPARATOR:
            case BELIEF:
            case GOAL:
            case QUESTION:
            case QUEST:
            case COMMAND:

            case '^':

            case '<':
            case '>':

            case '~':
            case '=':

            case '+':
            case '-':
            case '*':

            case '|':
            case '&':
            case '(':
            case ')':
            case '[':
            case ']':
            case '{':
            case '}':
            case '%':
            case '#':
            case '$':
            case ':':
            case '`':
            case '/':
            case '\\':
            case '\"':
            case '\'':

            case '\t':
            case '\n':
            case '\r':
            case 0:
                return false;
        }
        return true;
    }

    public static int hashCode(int id, Op o) {
        return (id + o.id) * 31;
    }

    @Override public int opX() {
        return AtomString;
    }

    @Override
    public final Op op() {
        return Op.ATOM;
    }

    @Override
    public final int vars() {
        return 0;
    }

    @Override
    public final boolean hasVars() {
        return false;
    }
    @Override
    public final boolean hasVarDep() {
        return false;
    }
    @Override
    public final boolean hasVarQuery() {
        return false;
    }
    @Override
    public final boolean hasVarIndep() {
        return false;
    }
    @Override
    public final boolean hasVarPattern() {
        return false;
    }


}

