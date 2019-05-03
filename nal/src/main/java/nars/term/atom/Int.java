package nars.term.atom;

import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Range;
import jcog.data.byt.util.IntCoding;
import nars.NAL;
import nars.Op;
import nars.The;
import nars.io.IO;
import nars.term.Term;

import static nars.Op.INT;

/**
 * 32-bit signed integer
 */
public class Int implements Intlike, The {

    static final Int[] pos = new Int[NAL.term.ANON_INT_MAX];
    private static final Int[] neg = new Int[NAL.term.ANON_INT_MAX];
    static {
        for (int i = 0; i < NAL.term.ANON_INT_MAX; i++) {
            pos[i] = new Int(i);
            neg[i] = new Int(-i);
        }
    }

    public static final Term ZERO = Int.the(0);
    public static final Term ONE = Int.the(1);
    public static final Term TWO = Int.the(2);
    public static final Term NEG_ONE = Int.the(-1);
    private final static int INT_ATOM = Term.opX(INT, 0);
//    private final static int INT_RANGE = Term.opX(INT, 1);



    /*@Stable*/
    private final byte[] bytesCached;

    //    protected Int(int id, byte[] bytes) {
//        this.id = id;
//        this.bytesCached = bytes;
//    }
    public final int i;
    private Int(int i) {
        this.i = i;

        int intLen = IntCoding.variableByteLengthOfZigZagInt(i); //1 to 4 bytes
        this.bytesCached = new byte[1 + intLen];
        bytesCached[0] = IO.opAndSubType(op(), (byte) (((opX() & 0xffff) & 0b111) >> 5));
        IntCoding.encodeZigZagVariableInt(i, bytesCached, 1);
    }

    public static Int the(int i) {
        if (i >= 0 && i < NAL.term.ANON_INT_MAX) {
            return pos[i];
        } else {
            if (i < 0 && i > -NAL.term.ANON_INT_MAX) {
                return neg[-i];
            } else {
                return new Int(i);
            }
        }
    }

//    /** because only a subset of the integers are Anon encodable */
//    public static boolean isAnon(int i) {
//        i = Math.abs(i);
//        return (i >= -Byte.MAX_VALUE && i <= Byte.MAX_VALUE);
//    }

    @Override
    public final byte[] bytes() {
        return bytesCached;
    }

    @Override
    public Range range() {
        return Range.singleton(i).canonical(DiscreteDomain.integers());
    }

    @Override
    public int opX() {
        return INT_ATOM;
    }

    @Override
    public /**/ Op op() {
        return INT;
    }

    @Override
    public int hashCode() {
        return Atom.hashCode(i, INT);
    }

    @Override
    public boolean equals(Object obj) {
        return (this == obj) || ((obj instanceof Int) && (i == ((Int) obj).i));
    }

    @Override
    public String toString() {
        return Integer.toString(i);
    }


//    public static class RotatedInt implements Termed {
//
//        private final int min, max;
//        private Int i;
//
//        public RotatedInt(int min /* inclusive */, int max /* exclusive */) {
//            this.min = min;
//            this.max = max;
//            this.i = Int.the((min + max) / 2);
//        }
//
//        @Override
//        public Term target() {
//            Term cur = i;
//            int next = this.i.id + 1;
//            if (next >= max)
//                next = min;
//            this.i = Int.the(next);
//            return cur;
//        }
//    }

}














































































































































































































































































































































































































































