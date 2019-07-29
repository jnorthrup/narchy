package nars.term.anon;

import jcog.Skill;
import nars.Op;
import nars.term.Neg;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.atom.Int;
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
@Skill({"G%C3%B6del_numbering_for_sequences"}) public enum Intrin  { ;

    /** code pages: categories of 8-bit numerically indexable items. */
    public static final short ANOMs = 0;

    public static final short VARDEPs = 1;
    public static final short VARINDEPs = 2;
    public static final short VARQUERYs = 3;
    public static final short VARPATTERNs = 4;

    public static final short IMGs = 5; // TODO make this a misc category

    /** 0..255 */
    public static final short INT_POSs = 6;

    /** -1..-255 */
    public static final short INT_NEGs = 7;

    /** ASCII 0..255 */
    public static final short CHARs = 8;



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
            case INT_POSs:
                return Int.the(num);
            case INT_NEGs:
                return Int.the(-num);
            case CHARs:
                return Atomic.the((char)num);
            default:
                throw new UnsupportedOperationException();
        }
    }

    public static Term term(int /* short */ i) {
        return term((short)i);
    }

    public static Term term(short i) {
        boolean neg = i < 0;
        if (neg)
            i = (short) -i;

        return neg ? neg(i) : _term(i);
    }

    static Neg.NegIntrin neg(short i) {
        return new Neg.NegIntrin(i);
    }

    public static int group(int i) {
        return (i & 0xff00)>>8;
    }


    /** returns 0 if the target is not anon ID or a negation of one */
    public static short id(Term t) {
//
//        if (t instanceof Int) {
//            int i = ((AnonID)t).i;
//            if (!Int.isAnon(i))
//                return 0;
//        }
        if (t instanceof Atomic) {
            return ((Atomic)t).intrin();
        }
        if (t instanceof Neg.NegIntrin) {
            return (short) -((Neg.NegIntrin)t).sub;
        }

//        if (t instanceof Neg) {
//
//
//            t = t.unneg();
//            if (t instanceof Intrin)
//                return (short) -((Intrin)t).i;
//            //else: continue
//        }

        return 0;
    }

    public static boolean intrinsic(Term[] t) {
        for (Term x : t) {
            //assert (!(x instanceof EllipsisMatch)) : "ellipsis match should not be a subterm of ANYTHING";
            if (Intrin.intrin(x))
                continue;

            if (!(x instanceof Neg /*Compound*/))
                return false; //not a NEG
            if (!Intrin.intrin(x.unneg()))
                return false;
//            //HACK
//            if (y instanceof Int) {
//                if (!Int.isAnon(((Int) y).i))
//                    return false;
//            }
        }
        return true;
    }

    public static boolean intrin(Term x) {
        return x instanceof IntrinAtomic || id(x)!=0;
    }

    public static int isVariable(short i, int ifNot) {
        int m = group(i);
        return (m == VARDEPs || m== VARINDEPs || m == VARPATTERNs || m == VARQUERYs) ?
            (i & 0xff) : ifNot;
    }
}
