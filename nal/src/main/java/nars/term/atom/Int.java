package nars.term.atom;

import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Range;
import jcog.Util;
import nars.IO;
import nars.Op;
import nars.Param;
import nars.The;
import nars.term.Term;

import java.util.function.IntConsumer;

import static nars.Op.INT;

/**
 * 32-bit signed integer
 */
public class Int implements Intlike, The {

    static final Int[] pos = new Int[Param.MAX_INTERNED_INTS];
    private static final Int[] neg = new Int[Param.MAX_INTERNED_INTS];
    static {
        for (int i = 0; i < Param.MAX_INTERNED_INTS; i++) {
            pos[i] = new Int(i);
            neg[i] = new Int(-i);
        }
    }

    public static final Term ZERO = Int.the(0);
    public static final Term ONE = Int.the(1);
    public static final Term TWO = Int.the(2);
    public static final Term NEG_ONE = Int.the(-1);
    private final static int INT_ATOM = Term.opX(INT, 0);
    private final static int INT_RANGE = Term.opX(INT, 1);


    public final int id;
    /*@Stable*/
    private final byte[] bytesCached;

    protected Int(int id, byte[] bytes) {
        this.id = id;
        this.bytesCached = bytes;
    }
    private Int(int i) {
        this.id = i;
        this.bytesCached = Util.bytePlusIntToBytes(
                IO.opAndSubType(op(), (byte) (((opX() & 0xffff) & 0b111) >> 5)),
                id);
    }

    public static Int the(int i) {
        if (i >= 0 && i < Param.MAX_INTERNED_INTS) {
            return pos[i];
        } else {
            if (i < 0 && i > -Param.MAX_INTERNED_INTS) {
                return neg[-i];
            } else {
                return new Int(i);
            }
        }
    }

    @Override
    public final byte[] bytes() {
        return bytesCached;
    }

    @Override
    public Range range() {
        return Range.singleton(id).canonical(DiscreteDomain.integers());
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
        return Atom.hashCode(id, INT);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (id >= Param.MAX_INTERNED_INTS && obj instanceof Int) {
            Int o = (Int) obj;
            return (id == o.id);
        }
        return false;
    }

    @Override
    public String toString() {
        return Integer.toString(id);
    }


    @Override
    public void forEachInt(IntConsumer c) {
        c.accept(id);
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
//        public Term term() {
//            Term cur = i;
//            int next = this.i.id + 1;
//            if (next >= max)
//                next = min;
//            this.i = Int.the(next);
//            return cur;
//        }
//    }

}














































































































































































































































































































































































































































