package nars.term.anon;

import jcog.Skill;
import nars.Op;
import nars.The;
import nars.subterm.util.SubtermMetadataCollector;
import nars.term.Neg;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.var.NormalizedVariable;

import static nars.Op.ImgExt;
import static nars.Op.ImgInt;

/**
 * INTrinsic terms
 *   a finite set of terms canonically addressable by an integer value
 *   used by the system in critical parts to improve space and time efficiency
 *
 * indicates the target has an anonymous, canonical integer identifier
 */
@Skill({"G%C3%B6del_numbering_for_sequences"}) public abstract class Intrin implements Atomic, The {

    /** TODO these dont need to be their own bits.
     * these are categories of 8-bit numerically indexable items.
     * use some flexible code page mapping */
    public static final short ANOMs = 0;
    public static final short VARDEPs = 1 << 8;
    public static final short VARINDEPs = 2 << 8;
    public static final short VARQUERYs = 3 << 8;
    public static final short VARPATTERNs = 4 << 8;
    public static final short IMGs = 5 << 8; // TODO make this a misc category
//    public static final short INTs = 1 << 8;

    /** meant to be a perfect hash among all normalized variables */
    public final int i;

    protected Intrin(Op type, byte num) {
        this(Intrin.termToId(type, num));
    }

    protected Intrin(int id) {
        this.i = id;
    }

    private static short termToId(Op o, byte id) {
        return (short)(group(o) | id);
    }

    private static short group(Op o) {

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


    public static int isVariable(short i, int ifNot) {
        int m = group(i);
        return (m == VARDEPs || m== VARINDEPs || m == VARPATTERNs || m == VARQUERYs) ?
                (i & 0xff) : ifNot;
    }

    /** @param i positive values only  */
    public static Term _term(short /* short */ i) {
        byte num = (byte) (i & 0xff);
        switch (group(i)) {
            case ANOMs:
                return Anom.the(num);
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

        return neg ? neg(i) : _term(i);
    }

    static Neg.NegIntrin neg(short i) {
        return new Neg.NegIntrin(i);
    }

    public static int group(int i) {
        return i & 0xff00;
    }
    public static int group(Intrin a) {
        return group(a.i);
    }

    public static boolean intrinsic(Term t0) {
        return t0 instanceof Intrin || (t0 instanceof Neg && t0.unneg() instanceof Intrin);
    }

    /** returns 0 if the target is not anon ID or a negation of one */
    public static short id(Term t) {
//
//        if (t instanceof Int) {
//            int i = ((AnonID)t).i;
//            if (!Int.isAnon(i))
//                return 0;
//        }

        if (t instanceof Intrin) {
            return (short) ((Intrin)t).i;
        }
        if (t instanceof Neg.NegIntrin) {
            return (short) -((Neg.NegIntrin)t).sub;
        }
        if (t instanceof Neg) {
            t = t.unneg();
            if (t instanceof Intrin)
                return (short) -((Intrin)t).i;
        }
        return 0;
    }

    public static SubtermMetadataCollector subtermMetadata(short[] s) {
        SubtermMetadataCollector c = new SubtermMetadataCollector();
        for (short x : s)
            c.collectMetadata(term(x));
        return c;
    }

    public static boolean intrinsic(Term[] t) {
        for (Term x : t) {
            //assert (!(x instanceof EllipsisMatch)) : "ellipsis match should not be a subterm of ANYTHING";
            if (x instanceof Intrin)
                continue;

            if (!(x instanceof Neg /*Compound*/))
                return false; //not a NEG
            Term y = x.unneg();
            if (!(y instanceof Intrin))
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

    @Override
    public Term neg() {
        return new Neg.NegIntrin(this.i);
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
