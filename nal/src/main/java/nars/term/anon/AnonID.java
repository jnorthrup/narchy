package nars.term.anon;

import nars.Op;
import nars.The;
import nars.subterm.util.SubtermMetadataCollector;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.var.NormalizedVariable;

import static nars.Op.*;

/**
 * indicates the target has an anonymous, canonical identifier (16-bit short)
 */
public abstract class AnonID implements Atomic, The {

    /** TODO these dont need to be their own bits.
     * these are categories of 8-bit numerically indexable items.
     * use some flexible code page mapping */
//    public static final short INTs = 1 << 8;
    public static final short ANOMs = 0;
    public static final short VARDEPs = 1 << 8;
    public static final short VARINDEPs = 2 << 8;
    public static final short VARQUERYs = 3 << 8;
    public static final short VARPATTERNs = 4 << 8;
    public static final short IMGs = 5 << 8; // TODO make this a misc category

    /** meant to be a perfect hash among all normalized variables */
    public final int i;

    protected AnonID(Op type, byte num) {
        this(AnonID.termToId(type, num));
    }

    protected AnonID(int id) {
        this.i = id;
    }

    private static short termToId(Op o, byte id) {
        return (short)(opToMask(o) | id);
    }

    private static short opToMask(Op o) {

        switch (o) {
            case ATOM:
                return ANOMs;
            case VAR_DEP:
                return VARDEPs;
            case VAR_INDEP:
                return VARINDEPs;
            case VAR_QUERY:
                return VARQUERYs;
            case VAR_PATTERN:
                return VARPATTERNs;
            case IMG:
                return IMGs;
//            case INT:
//                return INTs;
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
        return (m == VARDEPs || m== VARINDEPs || m == VARPATTERNs || m == VARQUERYs) ?
                (i & 0xff) : ifNot;
    }

    /** POS ONLY assumes non-negative; if the input is negative use idToTermPosOrNeg */
    public static Term termPos(short /* short */ i) {
        byte num = (byte) (i & 0xff);
        switch (mask(i)) {
            case ANOMs:
                return Anom.the[num];
            case IMGs:
                return num == '/' ? ImgExt : ImgInt;
            case VARDEPs:
                return NormalizedVariable.the(Op.VAR_DEP.id, num);
            case VARINDEPs:
                return NormalizedVariable.the(Op.VAR_INDEP.id, num);
            case VARQUERYs:
                return NormalizedVariable.the(Op.VAR_QUERY.id, num);
            case VARPATTERNs:
                return NormalizedVariable.the(Op.VAR_PATTERN.id, num);
//            case INTs:
//                return Int.the(num);
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
    }

    public static int mask(int i) {
        return i & 0xff00;
    }
    public static int mask(AnonID a) {
        return mask(a.i);
    }

    public static boolean isAnonPosOrNeg(Term t0) {
        return t0 instanceof AnonID || t0.unneg() instanceof AnonID;
    }

    /** returns 0 if the target is not anon ID or a negation of one */
    public static short id(Term t) {
//
//        if (t instanceof Int) {
//            int i = ((AnonID)t).i;
//            if (!Int.isAnon(i))
//                return 0;
//        }

        if (t instanceof AnonID) {
            return (short) ((AnonID)t).i;
        }
        if (t.op()==NEG) {
            t = t.unneg();
            if (t instanceof AnonID)
                return (short) -((AnonID)t).i;
        }
        return 0;
    }

    public static SubtermMetadataCollector subtermMetadata(short[] s) {
        SubtermMetadataCollector c = new SubtermMetadataCollector();
        for (short x : s)
            c.collectMetadata(term(x));
        return c;
    }

    public static boolean isAnon(Term[] t) {
        for (Term x : t) {
            //assert (!(x instanceof EllipsisMatch)) : "ellipsis match should not be a subterm of ANYTHING";
            Term y = x.unneg();
            if (!(y instanceof AnonID))
                return false;
//            //HACK
//            if (y instanceof Int) {
//                if (!Int.isAnon(((Int) y).i))
//                    return false;
//            }
        }
        return true;
    }

    @Override
    public final Term anon() {
        return this;
    }


    public final short anonID(boolean neg) {
        short s = (short) (i);
        return neg ? (short) -s : s;
    }

    @Override
    public final int hashCode() {
        return i;
    }

    @Override
    public final boolean equals(Object x) {
        return x == this;
//                   ||
//              (obj instanceof AnonID) && id==((AnonID)obj).id;
    }

    @Override
    public final boolean equalsRoot(Term x) {
        return x == this;
    }

    /** variable name */
    public final byte id() {
        return (byte) (i & 0xff);
    }
}
