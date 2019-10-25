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
public final class IdempotInt extends AtomicImpl implements Idempotent {

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





	public final int i;

	private IdempotInt(int i) {
		this.i = i;

        int intLen = IntCoding.variableByteLengthOfZigZagInt(i);
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
























}














































































































































































































































































































































































































































