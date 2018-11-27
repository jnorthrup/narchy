package nars.term.anon;

import nars.Op;
import nars.The;
import nars.subterm.util.SubtermMetadataCollector;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.var.NormalizedVariable;

import static nars.Op.*;

/**
 * indicates the term has an anonymous, canonical identifier (16-bit short)
 */
public abstract class AnonID implements Atomic, The {

    
    public static final short ATOM_MASK = 0;
    public static final short VARDEP_MASK = 1 << 8;
    public static final short VARINDEP_MASK = 2 << 8;
    public static final short VARQUERY_MASK = 3 << 8;
    public static final short VARPATTERN_MASK = 4 << 8;
    private static final short IMG_MASK = 5 << 8;

    /** meant to be a perfect hash among all normalized variables */
    public final short anonID;

    /** variable name */
    public final byte id;

    protected AnonID(Op type, byte num) {
        this(AnonID.termToId(type, num));
    }

    AnonID(short id) {
        this.anonID = id;
        this.id = (byte) (id & 0xff);
    }

    private static short termToId(Op o, byte id) {
        return (short)(opToMask(o) | id);
    }

    private static short opToMask(Op o) {

        switch (o) {
            case ATOM:
                return ATOM_MASK;
            case VAR_DEP:
                return VARDEP_MASK;
            case VAR_INDEP:
                return VARINDEP_MASK;
            case VAR_QUERY:
                return VARQUERY_MASK;
            case VAR_PATTERN:
                return VARPATTERN_MASK;
            case IMG:
                return IMG_MASK;
            default:
                throw new UnsupportedOperationException();
        }

    }

//    /** fast Anom (non-Var) test. works for either positive or negative */
//    static boolean isAnonPosOrNeg(short i) {
//        return isAnom(Math.abs(i));
//    }

//    /** fast Anom (non-Var) test. assumes positive */
//    public static boolean isAnom(int i) {
//        return (i & 0xff00) == ATOM_MASK;
//    }

    public static int isVariable(short i, int ifNot) {
        int m = mask(i);
        return (m ==VARDEP_MASK || m==VARINDEP_MASK || m == VARPATTERN_MASK || m == VARQUERY_MASK) ?
                (i & 0xff) : ifNot;
    }

    /** POS ONLY assumes non-negative; if the input is negative use idToTermPosOrNeg */
    public static Term termPos(short /* short */ i) {
        byte num = (byte) (i & 0xff);
        switch (mask(i)) {
            case ATOM_MASK:
                return Anom.the[num];
            case IMG_MASK:
                return num == '/' ? ImgExt : ImgInt;
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

    public static Term term(int /* short */ i) {
        return term((short)i);
    }

    public static Term term(short /* short */ i) {
        boolean neg = i < 0;
        if (neg)
            i = (short) -i;

        Term t = termPos(i);

        return neg ? t.negIf(neg) : t;

//        byte num = (byte) (i & 0xff);
//        byte varType;
//        switch (idToMask(i)) {
//            case ATOM_MASK:
//                return neg ? Anom.theNeg[num] : Anom.the[num];
//            case VARDEP_MASK:
//                varType = (Op.VAR_DEP.id); break;
//            case VARINDEP_MASK:
//                varType = (Op.VAR_INDEP.id); break;
//            case VARQUERY_MASK:
//                varType = (Op.VAR_QUERY.id); break;
//            case VARPATTERN_MASK:
//                varType = (Op.VAR_PATTERN.id); break;
//            case IMG_MASK:
//                return (num == '/' ? ImgExt : ImgInt).negIf(neg);
//            default:
//                throw new UnsupportedOperationException();
//        }
//        return NormalizedVariable.the(varType, num).negIf(neg);
    }

    public static int mask(short i) {
        return i & 0xff00;
    }
    public static int mask(AnonID a) {
        return mask(a.anonID);
    }

    public static boolean isAnonPosOrNeg(Term t0) {
        return t0 instanceof AnonID || t0.unneg() instanceof AnonID;
    }

    /** returns 0 if the term is not anon ID or a negation of one */
    public static short id(Term t) {
        if (t instanceof AnonID) {
            return ((AnonID)t).anonID;
        }
        if (t.op()==NEG) {
            t = t.unneg();
            if (t instanceof AnonID)
                return (short) -((AnonID)t).anonID;
        }
        return 0;
    }

    public static SubtermMetadataCollector subtermMetadata(short[] s) {
        SubtermMetadataCollector c = new SubtermMetadataCollector();
        for (short x : s)
            c.collectMetadata(term(x));
        return c;
    }

    @Override
    public final Term anon() {
        return this;
    }


    public final short anonID(boolean neg) {
        return neg ? (short) (-anonID) : anonID;
    }

    @Override
    public final int hashCode() {
        return anonID;
    }

    @Override
    public final boolean equals(Object obj) {
        return obj == this;
//                   ||
//              (obj instanceof AnonID) && id==((AnonID)obj).id;
    }

}
