package nars.term.atom;

import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Range;
import jcog.data.byt.util.IntCoding;
import nars.Op;
import nars.The;
import nars.term.Term;
import nars.term.anon.Intrin;

import static nars.NAL.term.ANON_INT_MAX;
import static nars.Op.INT;

/**
 * 32-bit signed integer
 */
public final class Int extends Atomic implements The {

	static final int INT_CACHE_SIZE = ANON_INT_MAX * 8;
	static final Int[] pos = new Int[INT_CACHE_SIZE];
	private static final Int[] neg = new Int[INT_CACHE_SIZE];

	static {
		for (int i = 0; i < pos.length; i++) {
			pos[i] = new Int(i);
		}
		for (int i = 1; i < neg.length; i++) {
			neg[i] = new Int(-i);
		}
	}

	public static final Term ZERO = Int.the(0);
	public static final Term ONE = Int.the(1);
	public static final Term TWO = Int.the(2);
	public static final Term NEG_ONE = Int.the(-1);

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
		bytesCached[0] = Op.INT.id;
		IntCoding.encodeZigZagVariableInt(i, bytesCached, 1);
	}

    @Override
    public final short intrin() {
	    if (i <= ANON_INT_MAX) {
	        if (i >= 0) {
	            return (short) ((Intrin.INT_POSs<<8)|i);
            } else if ( i >= -ANON_INT_MAX) {
                return (short) ((Intrin.INT_NEGs<<8)|(-i));
            }
        }
        return 0;
    }

    public static Int the(int i) {
		if (i >= 0 && i < pos.length) {
			return pos[i];
		} else {
			if (i < 0 && i > -neg.length) {
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

	public Range range() {
		return Range.singleton(i).canonical(DiscreteDomain.integers());
	}

	@Override
	public /**/ Op op() {
		return INT;
	}

	@Override
	public final int hashCode() {
		//return Atom.hashCode(i, INT);
		return i;
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














































































































































































































































































































































































































































