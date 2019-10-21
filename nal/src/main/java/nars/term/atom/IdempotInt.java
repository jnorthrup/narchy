package nars.term.atom;

import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Range;
import jcog.data.byt.util.IntCoding;
import nars.Op;
import nars.Idempotent;
import nars.term.Term;
import nars.term.anon.Intrin;

import static nars.NAL.term.ANON_INT_MAX;
import static nars.Op.INT;

/**
 * 32-bit signed integer
 */
public final class IdempotInt extends Atomic implements Idempotent {

	static final int INT_CACHE_SIZE = ANON_INT_MAX * 8;
	static final IdempotInt[] pos = new IdempotInt[INT_CACHE_SIZE];
	private static final IdempotInt[] neg = new IdempotInt[INT_CACHE_SIZE];

	static {
		for (int i = 0; i < pos.length; i++) {
			pos[i] = new IdempotInt(i);
		}
		for (int i = 1; i < neg.length; i++) {
			neg[i] = new IdempotInt(-i);
		}
	}

	public static final Term ZERO = IdempotInt.the(0);
	public static final Term ONE = IdempotInt.the(1);
	public static final Term TWO = IdempotInt.the(2);
	public static final Term NEG_ONE = IdempotInt.the(-1);

	/*@Stable*/
	private final byte[] bytesCached;

	//    protected Int(int id, byte[] bytes) {
//        this.id = id;
//        this.bytesCached = bytes;
//    }
	public final int i;

	private IdempotInt(int i) {
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
	            return (short) (((int) Intrin.INT_POSs <<8)|i);
            } else if ( i >= -ANON_INT_MAX) {
                return (short) (((int) Intrin.INT_NEGs <<8)|(-i));
            }
        }
        return (short) 0;
    }

    public static IdempotInt the(int i) {
		if (i >= 0 && i < pos.length) {
			return pos[i];
		} else {
			return i < 0 && i > -neg.length ? neg[-i] : new IdempotInt(i);
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
		return (this == obj) || ((obj instanceof IdempotInt) && (i == ((IdempotInt) obj).i));
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













































































































































































































































































































































































































































