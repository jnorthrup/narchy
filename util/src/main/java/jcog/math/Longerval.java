package jcog.math;

import jcog.WTF;
import org.eclipse.collections.impl.block.factory.Comparators;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;

import static java.lang.Math.max;
import static java.lang.Math.min;

/** An immutable inclusive longerval a..b implementation of LongInterval */
public class Longerval implements LongInterval, Comparable<Longerval> {



	public final long a;
	public final long b;

	public Longerval(long a) {
		this(a,a);
	}

	public Longerval(long a, long b) {

		if (b >= a) {
			this.a = a;
			this.b = b;
		} else {
			throw new RuntimeException("wrong Interval ordering; b >= a but attempted creation of: [" + a + ".." + b + "]");
		}
	}




	@Override
	public long start() {
		return a;
	}

	@Override
	public long end() {
		return b;
	}



	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		Longerval other = (Longerval)o;
		return this.a==other.a && this.b==other.b;
	}

	@Override
	public int hashCode() {
		return Long.hashCode((31 + a) * 31 + b); //TODO is this good?
	}

	/** Does this start completely before other? Disjoint */
	public boolean startsBeforeDisjoint(Longerval other) {
		return this.a<other.a && this.b<other.a;
	}

	/** Does this start at or before other? Nondisjoint */
	public boolean startsBeforeNonDisjoint(Longerval other) {
		return this.a<=other.a && this.b>=other.a;
	}

	/** Does this.a start after other.b? May or may not be disjoint */
	public boolean startsAfter(Longerval other) { return this.a>other.a; }

	/** Does this start completely after other? Disjoint */
	public boolean startsAfterDisjoint(Longerval other) {
		return this.a>other.b;
	}

	/** Does this start after other? NonDisjoint */
	public boolean startsAfterNonDisjoint(Longerval other) {
		return this.a>other.a && this.a<=other.b; 
	}

	/** Are both ranges disjoint? I.e., no overlap? */
	public boolean disjoint(Longerval other) {
		return startsBeforeDisjoint(other) || startsAfterDisjoint(other);
	}

	/** Are two longervals adjacent such as 0..41 and 42..42? */
	public boolean adjacent(Longerval other) {
		return this.a == other.b+1 || this.b == other.a-1;
	}

	public boolean properlyContains(Longerval other) {
		return other.a >= this.a && other.b <= this.b;
	}

	/** Return the longerval computed from combining this and other */
	public Longerval union(Longerval other) {
		//return new Longerval(min(a, other.a), max(b, other.b));
		return union(other.a, other.b);
	}
	public Longerval union(long oa, long ob) {
		return new Longerval(min(a, oa), max(b, ob));
	}

	/** Return the longerval in common between this and o */
	@Nullable
	public Longerval intersection(Longerval other) {
		return intersection(other.a, other.b);
//		if (equals(other)) return this;
//		long a = max(this.a, other.a);
//		long b = min(this.b, other.b);
//		return a > b ? null : new Longerval(a, b);
	}

	@Nullable
	public Longerval intersection(long otherA, long otherB) {
		long x = this.a, y = this.b;
		long a = max(x, otherA), b = min(y, otherB);
		return a > b ? null : ((a == x && b == y) ?  this : new Longerval(a, b));
	}

	@Nullable public static Longerval intersection(long myA, long myB, long otherA, long otherB) {
		return new Longerval(myA, myB).intersection(otherA, otherB);
	}
	@Nullable public static long[] intersectionArray(long myA, long myB, long otherA, long otherB) {
		return intersectionArray(myA, myB, otherA, otherB, null);
	}

	@Nullable public static long[] intersectionArray(long myA, long myB, long otherA, long otherB, @Nullable long[] target) {
		@Nullable Longerval x = intersection(myA, myB, otherA, otherB);
		return x== null ? null : x.intervalArray(target);
	}

	private long[] intervalArray() {
		return intervalArray(null);
	}
	private long[] intervalArray(@Nullable long[] target) {
		if (target==null)
			target = new long[2];
		target[0] = a;
		target[1] = b;
		return target;
	}

	public static long[] unionArray(long myA, long myB, long otherA, long otherB) {
		return union(myA, myB, otherA, otherB).intervalArray();
	}


	/** Return the longerval with elements from this not in other;
	 *  other must not be totally enclosed (properly contained)
	 *  within this, which would result in two disjoint longervals
	 *  instead of the single one returned by this method.
	 */
	public Longerval differenceNotProperlyContained(Longerval other) {
		Longerval diff = null;
		
		if ( other.startsBeforeNonDisjoint(this) ) {
			diff = new Longerval(max(this.a, other.b + 1),
							   this.b);
		}

		
		else if ( other.startsAfterNonDisjoint(this) ) {
			diff = new Longerval(this.a, other.a - 1);
		}
		return diff;
	}

	@Override
	public String toString() {
		return a+".."+b;
	}

	/** returns -1 if no intersection; 0 = adjacent, > 0 = non-zero interval in common */
	public static long intersectLength(long x1, long y1, long x2, long y2) {
		if (x1 == ETERNAL || x1 == TIMELESS || x2 == ETERNAL || x2 == TIMELESS)
			throw new WTF();
		long a = max(x1, x2);
		long b = min(y1, y2);
		return a <= b ? b - a : -1;
	}
	/** true if [as..ae] intersects [bs..be] */
	public static boolean intersects(long as, long ae, long bs, long be) {
		assert(as!=TIMELESS && bs!=TIMELESS);
		return intersectsSafe(as, ae, bs, be);
	}

	public static boolean intersectsSafe(long as, long ae, long bs, long be) {
		return (as == ETERNAL) || (bs == ETERNAL) || (max(as, bs) <= min(ae, be));
	}

	/** true if [os..oe] contains [is..ie] */
	public static boolean contains(long os, long oe, long is, long ie) {
		assert(os!=TIMELESS && is!=TIMELESS);
		return containsSafe(os, oe, is, ie);
	}

	protected static boolean containsSafe(long os, long oe, long is, long ie) {
		return os == ETERNAL && is != ETERNAL || os <= is && oe >= ie;
	}


//	@Nullable public static Longerval intersect(long x1, long x2, long y1, long y2) {

//		return internew Longerval(x1, x2).intersection(y1, y2);
//	}
	public static Longerval union(long x1, long x2, long y1, long y2) {
		assert(x1!=TIMELESS && x1!=ETERNAL && y1!=TIMELESS && y1!=ETERNAL);
		return new Longerval(x1, x2).union(y1, y2);
	}

	public static long unionLength(long x1, long x2, long y1, long y2) {
		return max(x2, y2) - min(x1, y1);
	}


	/** returns -1 if no intersection; 0 = adjacent, > 0 = non-zero interval in common */
	public static int intersectLength(int x1, int x2, int y1, int y2) {
		int a = max(x1, x2);
		int b = min(y1, y2);
		return a <= b ? b - a : -1;
	}


    public long[] toArray() {
		return new long[] { a, b };
    }

	final static Comparator<Longerval> comparator = Comparators.byLongFunction((Longerval l)->l.a).thenComparing((Longerval l) -> l.b);

	@Override
	public final int compareTo(Longerval x) {
		return this.equals(x) ?
				0
				:
				comparator.compare(this, x);
	}

}