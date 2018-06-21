package nars.term.anon;

import nars.Op;
import nars.The;
import nars.subterm.util.SubtermMetadataCollector;
import nars.term.Term;
import nars.term.var.NormalizedVariable;

import static nars.Op.NEG;

/**
 * indicates the term has an anonymous, canonical identifier (16-bit short)
 */
public interface AnonID extends Term, The {

    
    short ATOM_MASK = 0;
    short VARDEP_MASK = 1 << 8;
    short VARINDEP_MASK = 2 << 8;
    short VARQUERY_MASK = 3 << 8;
    short VARPATTERN_MASK = 4 << 8;

    static short termToId(Op o, byte id) {

        short mask;
        switch (o) {
            case ATOM:
                mask = ATOM_MASK;
                break;
            case VAR_DEP:
                mask = VARDEP_MASK;
                break;
            case VAR_INDEP:
                mask = VARINDEP_MASK;
                break;
            case VAR_QUERY:
                mask = VARQUERY_MASK;
                break;
            case VAR_PATTERN:
                mask = VARPATTERN_MASK;
                break;
            default:
                throw new UnsupportedOperationException();
        }

        return (short) (mask | id);
    }

    /** fast Anom (non-Var) test. works for either positive or negative */
    static boolean isAnonPosOrNeg(short i) {
        return isAnom(Math.abs(i));
    }

    /** fast Anom (non-Var) test. assumes positive */
    static boolean isAnom(int i) {
        return (i & 0xff00) == ATOM_MASK;
    }

    static int isVariable(short i, int ifNot) {
        return ((i & 0xff00) != ATOM_MASK) ? (i & 0xff) : ifNot;
    }

    /** POS ONLY assumes non-negative; if the input is negative use idToTermPosOrNeg */
    static Term idToTermPos(short /* short */ i) {
        byte num = (byte) (i & 0xff);
        int m = idToMask(i);
        switch (m) {
            case ATOM_MASK:
                return Anom.the[num];
            case VARDEP_MASK:
                return NormalizedVariable.the(Op.VAR_DEP.id, num);
            case VARINDEP_MASK:
                return NormalizedVariable.the(Op.VAR_INDEP.id, num);
            case VARQUERY_MASK:
                return NormalizedVariable.the(Op.VAR_QUERY.id, num);
            case VARPATTERN_MASK:
                return NormalizedVariable.the(Op.VAR_PATTERN.id, num);
            default:
                throw new UnsupportedOperationException();
        }
    }
    static Term idToTerm(short /* short */ i) {
        boolean neg = i < 0;
        if (neg)
            i = (short) -i;

        byte num = (byte) (i & 0xff);
        int m = idToMask(i);
        byte varType;
        switch (m) {
            case ATOM_MASK:
                return neg ? Anom.theNeg[num] : Anom.the[num];
            case VARDEP_MASK:
                varType = (Op.VAR_DEP.id); break;
            case VARINDEP_MASK:
                varType = (Op.VAR_INDEP.id); break;
            case VARQUERY_MASK:
                varType = (Op.VAR_QUERY.id); break;
            case VARPATTERN_MASK:
                varType = (Op.VAR_PATTERN.id); break;
            default:
                throw new UnsupportedOperationException();
        }
        return NormalizedVariable.the(varType, num).negIf(neg);
    }

    static int idToMask(short i) {
        return i & 0xff00;
    }

    public static boolean isAnonPosOrNeg(Term t0) {
        return t0 instanceof AnonID || t0.unneg() instanceof AnonID;
    }

    /** returns 0 if the term is not anon ID */
    public static short id(Term t) {
        if (t instanceof AnonID) {
            return ((AnonID)t).anonID();
        }
        if (t.op()==NEG) {
            t = t.unneg();
            if (t instanceof AnonID)
                return (short) -((AnonID)t).anonID();
        }
        return 0;
    }

    static SubtermMetadataCollector subtermMetadata(short[] s) {
        SubtermMetadataCollector c = new SubtermMetadataCollector();
        for (short x : s)
            idToTerm(x).collectMetadata(c);
        return c;
    }

    @Override
    default Term anon() {
        return this;
    }

    short anonID();

    default byte anonNum() {
        return (byte) (anonID() & 0xff);
    }


    default short anonID(boolean neg) {
        short id = anonID();
        return neg ? (short) (-id) : id;
    }
}
