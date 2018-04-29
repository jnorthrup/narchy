package jcog.math;

import org.jetbrains.annotations.Nullable;

import static java.lang.Math.max;
import static java.lang.Math.min;

/** An immutable inclusive longerval a..b implementation of LongInterval */
public class Longerval implements LongInterval {

	//public static final Interval INVALID = new Interval(-1,-2);

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
		long hash = 23;
		hash = hash * 31 + a;
		hash = hash * 31 + b;
		return Long.hashCode(hash);
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
		return this.a>other.a && this.a<=other.b; // this.b>=other.b implied
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
		return new Longerval(min(a, other.a), max(b, other.b));
	}
	public Longerval union(long oa, long ob) {
		return new Longerval(min(a, oa), max(b, ob));
	}

	/** Return the longerval in common between this and o */
	@Nullable
	public Longerval intersection(Longerval other) {
		if (equals(other)) return this;
		long a = max(this.a, other.a);
		long b = min(this.b, other.b);
		return a > b ? null : new Longerval(a, b);
	}

	/** Return the longerval with elements from this not in other;
	 *  other must not be totally enclosed (properly contained)
	 *  within this, which would result in two disjoint longervals
	 *  instead of the single one returned by this method.
	 */
	public Longerval differenceNotProperlyContained(Longerval other) {
		Longerval diff = null;
		// other.a to left of this.a (or same)
		if ( other.startsBeforeNonDisjoint(this) ) {
			diff = new Longerval(max(this.a, other.b + 1),
							   this.b);
		}

		// other.a to right of this.a
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
		long a = max(x1, x2);
		long b = min(y1, y2);
		return a <= b ? b - a : -1;
	}

	/** returns -1 if no intersection; 0 = adjacent, > 0 = non-zero interval in common */
	public static int intersectLength(int x1, int y1, int x2, int y2) {
		int a = max(x1, x2);
		int b = min(y1, y2);
		return a <= b ? b - a : -1;
	}

	@Nullable public static Longerval intersect(long x1, long x2, long y1, long y2) {
		return new Longerval(x1, x2).intersection(new Longerval(y1, y2));
	}
	public static Longerval union(long x1, long x2, long y1, long y2) {
		return new Longerval(x1, x2).union(new Longerval(y1, y2));
	}

	public static long unionLength(long x1, long x2, long y1, long y2) {
		//return new Interval(x1, x2).union(new Interval(y1, y2)).length();
		return max(x2, y2) - min(x1, y1);
	}





}