package nars.term.atom;

import nars.Op;
import nars.The;

import java.util.stream.IntStream;

import static nars.Op.*;

/**
 * default Atom implementation: wraps a String instance as closely as possible.
 * ideally this string is stored encoded in UTF8 byte[]'s
 */
public class Atom extends AbstractAtomic implements The {



//    /** use with caution */
//    public Atom(byte[] b) {
//        super(b);
//    }

    public Atom(String id) {
        super((byte)0 /* = ATOM.id */, id);
    }

    static void validateAtomID(String id) {
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

        

    }


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

//    public static int hashCode(int id, Op o) {
//        return (id + o.id) * 31;
//    }

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


    public boolean startsWith(byte... prefix) {
        byte[] b = bytes();
        int o = 3; //skip op byte + 2 len bytes
        int P = prefix.length;
        if (b.length - o >= P) {
            for (int i = 0; i < P; i++) {
                if (b[i + o] != prefix[i]) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }


}

