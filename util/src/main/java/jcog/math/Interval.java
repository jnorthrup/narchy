package jcog.math;

import org.jetbrains.annotations.Nullable;

import static java.lang.Math.max;
import static java.lang.Math.min;

/** An immutable inclusive longerval a..b */
public class Interval {

	//public static final Interval INVALID = new Interval(-1,-2);

	public final long a;
	public final long b;

	public Interval(long a) {
		this(a,a);
	}

	public Interval(long a, long b) {
		if (b >= a) {
			this.a = a;
			this.b = b;
		} else {
			throw new RuntimeException("wrong Interval ordering; b >= a but attempted creation of: [" + a + ".." + b + "]");
		}
	}

//	/** Interval objects are used readonly so share all with the
//	 *  same single value a==b up to some max size.  Use an array as a perfect hash.
//	 *  Return shared object for 0..INTERVAL_POOL_MAX_VALUE or a new
//	 *  Interval object with a..a in it.  On Java.g4, 218623 IntervalSets
//	 *  have a..a (set with 1 element).
//	 */
//	public static Interval of(long a, long b) {
//		// cache just a..a
//		if ( a!=b || a<0 || a>INTERVAL_POOL_MAX_VALUE ) {
//			return new Interval(a,b);
//		}
//		if ( cache[a]==null ) {
//			cache[a] = new Interval(a,a);
//		}
//		return cache[a];
//	}

	/** return number of elements between a and b inclusively. x..x is length 1.
	 *  if b &lt; a, then length is 0.  9..10 has length 2.
	 */
	public long length() {
		return b-a;
	}

	@Override
	public boolean equals(Object o) {
//		if ( o==null || !(o instanceof Interval) ) {
//			return false;
//		}
		Interval other = (Interval)o;
		return this.a==other.a && this.b==other.b;
	}

	@Override
	public int hashCode() {
		long hash = 23;
		hash = hash * 31 + a;
		hash = hash * 31 + b;
		return (int) hash;
	}

	/** Does this start completely before other? Disjoint */
	public boolean startsBeforeDisjoint(Interval other) {
		return this.a<other.a && this.b<other.a;
	}

	/** Does this start at or before other? Nondisjoint */
	public boolean startsBeforeNonDisjoint(Interval other) {
		return this.a<=other.a && this.b>=other.a;
	}

	/** Does this.a start after other.b? May or may not be disjoint */
	public boolean startsAfter(Interval other) { return this.a>other.a; }

	/** Does this start completely after other? Disjoint */
	public boolean startsAfterDisjoint(Interval other) {
		return this.a>other.b;
	}

	/** Does this start after other? NonDisjoint */
	public boolean startsAfterNonDisjoint(Interval other) {
		return this.a>other.a && this.a<=other.b; // this.b>=other.b implied
	}

	/** Are both ranges disjoint? I.e., no overlap? */
	public boolean disjoint(Interval other) {
		return startsBeforeDisjoint(other) || startsAfterDisjoint(other);
	}

	/** Are two longervals adjacent such as 0..41 and 42..42? */
	public boolean adjacent(Interval other) {
		return this.a == other.b+1 || this.b == other.a-1;
	}

	public boolean properlyContains(Interval other) {
		return other.a >= this.a && other.b <= this.b;
	}

	/** Return the longerval computed from combining this and other */
	public Interval union(Interval other) {
		return new Interval(min(a, other.a), max(b, other.b));
	}

	/** Return the longerval in common between this and o */
	@Nullable
	public Interval intersection(Interval other) {
		long a = max(this.a, other.a);
		long b = min(this.b, other.b);
		return a > b ? null : new Interval(a, b);
	}

	/** Return the longerval with elements from this not in other;
	 *  other must not be totally enclosed (properly contained)
	 *  within this, which would result in two disjoint longervals
	 *  instead of the single one returned by this method.
	 */
	public Interval differenceNotProperlyContained(Interval other) {
		Interval diff = null;
		// other.a to left of this.a (or same)
		if ( other.startsBeforeNonDisjoint(this) ) {
			diff = new Interval(max(this.a, other.b + 1),
							   this.b);
		}

		// other.a to right of this.a
		else if ( other.startsAfterNonDisjoint(this) ) {
			diff = new Interval(this.a, other.a - 1);
		}
		return diff;
	}

	@Override
	public String toString() {
		return a+".."+b;
	}

	public static long intersectLength(long x1, long y1, long x2, long y2) {
		long a = max(x1, x2);
		long b = min(y1, y2);
		return a <= b ? b - a : -1;
	}
	public static int intersectLength(int x1, int y1, int x2, int y2) {
		int a = max(x1, x2);
		int b = min(y1, y2);
		return a <= b ? b - a : -1;
	}

	@Nullable public static Interval intersect(long x1, long x2, long y1, long y2) {
		return new Interval(x1, x2).intersection(new Interval(y1, y2));
	}
	public static Interval union(long x1, long x2, long y1, long y2) {
		return new Interval(x1, x2).union(new Interval(y1, y2));
	}

	public static long unionLength(long x1, long x2, long y1, long y2) {
		//return new Interval(x1, x2).union(new Interval(y1, y2)).length();
		return max(x2, y2) - min(x1, y1);
	}

	public long mid() {
		return (a+b)/2;
	}

//static Interval[] cache = new Interval[INTERVAL_POOL_MAX_VALUE+1];
//	public static final long INTERVAL_POOL_MAX_VALUE = 1000;
//	public static long creates = 0;
//	public static long misses = 0;
//	public static long hits = 0;
//	public static long outOfRange = 0;


}