package jcog.math;

import jcog.WTF;

import java.util.Arrays;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * pair of 64-bit signed long integers representing an interval.
 * a special 'ETERNAL' value represents (-infinity,+infinity)
 * <p>
 * TODO allow (-infinity, ..x) and (x, +infinity)
 */
public interface LongInterval {

	long ETERNAL = Long.MIN_VALUE;
	long TIMELESS = Long.MAX_VALUE;


	/**
	 * returns -1 if no intersection; 0 = adjacent, > 0 = non-zero interval in common
	 */
	static long intersectLength(long x1, long y1, long x2, long y2) {
		if (x1 == ETERNAL || x1 == TIMELESS || x2 == ETERNAL || x2 == TIMELESS)
			throw new WTF();
		long a = max(x1, x2);
		long b = min(y1, y2);
		return a <= b ? b - a : -1;
	}

	/**
	 * true if [as..ae] intersects [bs..be]
	 */
	static boolean intersects(long as, long ae, long bs, long be) {
		assert (as != TIMELESS && bs != TIMELESS);
		return intersectsSafe(as, ae, bs, be);
	}

	static boolean intersectsSafe(long as, long ae, long bs, long be) {
		return (as == ETERNAL) || (bs == ETERNAL) || intersectsRaw(as, ae, bs, be);
	}

	static boolean intersectsRaw(long as, long ae, long bs, long be) {
		return max(as, bs) <= min(ae, be);
	}


	//		return internew Longerval(x1, x2).intersection(y1, y2);
//	}
	static Longerval union(long x1, long x2, long y1, long y2) {
		return new Longerval(x1, x2).union(y1, y2);
	}

	static long unionLength(long x1, long x2, long y1, long y2) {
		return max(x2, y2) - min(x1, y1);
	}

	/**
	 * returns -1 if no intersection; 0 = adjacent, > 0 = non-zero interval in common
	 */
	static int intersectLength(int x1, int x2, int y1, int y2) {
		int a = max(x1, x2);
		int b = min(y1, y2);
		return a <= b ? b - a : -1;
	}

	long start();

	long end();

	default long mid() {
		long s = start();
		return s == ETERNAL ? ETERNAL : (s + end()) / 2L;
	}

	/**
	 * return number of elements between a and b inclusively. x..x is length 1.
	 * if b &lt; a, then length is 0.  9..10 has length 2.
	 */
	default long range() {
		long s = start();
		if (s == ETERNAL)
			throw new ArithmeticException("ETERNAL range calculated");
		return 1 + (end() - s);
	}

	default long rangeIfNotEternalElse(long what) {
		long s = start();
		if (s == ETERNAL)
			return what;
		return 1 + (end() - s);
	}


	/**
	 * finds the nearest point within the provided interval relative to some point in this interval
	 */
	default long nearestPointExternal(long a, long b) {
		if (a == b || a == ETERNAL)
			return a;

		long s = start();
		if (s == ETERNAL)
			return (a + b) / 2L;
		long e = end();


		long mid = (s + e) / 2;
		if (s >= a && s <= b) {
			return mid;
		}


		if (Math.abs(mid - a) <= Math.abs(mid - b))
			return a;
		else
			return b;
	}


	default boolean isDuringAny(long... when) {
		if (when.length == 2 && when[0] == when[1]) return isDuring(when[0]);
		for (long l : when) {
			if (isDuring(l)) {
				return true;
			}
		}
		return false;
	}

	default boolean isDuringAll(long... when) {
		if (when.length == 2 && when[0] == when[1]) return isDuring(when[0]);
		for (long l : when) {
			if (!isDuring(l)) {
				return false;
			}
		}
		return true;
	}

	default boolean isDuring(long when) {
		if (when == ETERNAL)
			return true;
		long start = start();
		return (start == ETERNAL) || (start == when) || ((when >= start) && (when <= end()));
	}



	/**
	 * finds the nearest point inside this interval to the provided range, which may be
	 * inside, intersecting, or disjoint from this interval.
	 */
	default long nearestPointInternal(long a, long b) {

		assert (b >= a && (a != ETERNAL || a == b));

		if (a == ETERNAL)
			return mid();

		long s = this.start();
		if (s == ETERNAL)
			return ETERNAL;

		long e = this.end();
		if (s == e)
			return s;

		if ((a >= s) && (b <= e)) {
			return (a + b) / 2L;
		} else if (a < s && b > e) {
			return (s + e) / 2L;
		} else {
			long se = (s + e) / 2L;
			long ab = (a + b) / 2L;
			if (se <= ab) {
				return e;
			} else {
				return s;
			}
		}
	}


	static long minTimeTo(long when, long s, long e) {
		return (s <= when && e >= when) ?
			0 /* internal */ :
			Math.min(Math.abs(s - when), Math.abs(e - when)); /* external */
	}


	default long minTimeTo(LongInterval i) {
		return i == this ? 0 : minTimeTo(i.start(), i.end());
	}

	default long minTimeTo(long a) {
		return minTimeTo(a, a);
	}

	/**
	 * if the task intersects (ex: occurrs during) the specified interval,
	 * returned time distance is zero, regardless of how far it may extend before or after it
	 */
	default long minTimeTo(long a, long b) {

		if (a == ETERNAL)
			return 0;
		if (a == TIMELESS)
			throw new WTF(); //return TIMELESS;

		long s = start(); //assert(s!=TIMELESS);

		if (s == ETERNAL || s == a)
			return 0;

		long e = end();

		if (/*e != s && */intersectsRaw(a, b, s, e))
			return 0;

		long sa = Math.abs(s - a);
		if (a == b) {
			return s == e ? sa : Math.min(sa, Math.abs(e - b));
		} else {
			long sab = Math.min(sa, Math.abs(s - b));
			return s == e ? sab : Math.min(sab, Math.min(Math.abs(e - a), Math.abs(e - b)));
		}
		//} else {
		//return 0;
		//}
	}

	default long maxTimeTo(long a, long b) {
		if (a == ETERNAL) return 0;
		long s = start();
		if (s == ETERNAL) return 0;
		long e = end();
		long ul = LongInterval.unionLength(s, e, a, b);

//		long sasbeaeb = Math.max(
//			max(Math.abs(s - a), Math.abs(s - b)),
//			max(Math.abs(e - a), Math.abs(e - b))
//		);

		return ul;
	}


	default long meanTimeTo(long s, long e) {
		long ds = meanTimeTo(s);
		return s==e ? ds : (ds + meanTimeTo(e))/2;
	}

	default long meanTimeTo(long x) {
		if (x == ETERNAL)
			return 0;
		long start = start();
		if (start == ETERNAL || start == x) return 0;
		long end = end();
		if (x <= end && x >= start)
			return 0; //contained

		long distToStart = Math.abs(start - x);
		if (end == start) {
			return distToStart;
		} else {
			long distToEnd = Math.abs(end - x);
			return (distToStart + distToEnd) / 2L;
		}
	}


	default long maxTimeTo(long x) {

		long start = start();
		if (start == ETERNAL) return 0;
		long end = end();
		long distToStart = Math.abs(start - x);
		if (end == start) {
			return distToStart;
		} else {
			long distToEnd = Math.abs(end - x);
			return Math.max(distToStart, distToEnd);
		}
	}

	default boolean intersects(LongInterval i) {
		return this == i || intersects(i.start(), i.end());
	}

	default boolean intersects(long s, long e) {
		assert (s != TIMELESS);
		if (s == ETERNAL)
			return true;
		long start = start();
		return (start == ETERNAL) || (e >= start && s <= end());
	}

	default boolean intersectsRaw(long s, long e) {
		return (e >= start() && s <= end());
	}

	default boolean contains(long s, long e) {
		assert (s != TIMELESS);
		return containsSafe(s, e);
	}
	default boolean containedBy(long s, long e) {
		assert (s != TIMELESS);
		return containedBySafe(s, e);
	}

	default boolean containsSafe(long s, long e) {
		long start = start();
		if (start == ETERNAL)
			return true;
		else
			return s!=ETERNAL && (s >= start && e <= end());
	}
	default boolean containedBySafe(long s, long e) {
		if (s == ETERNAL)
			return true;
		else {
			long start = start();
			return start != ETERNAL && start >= s && end() <= e;
		}
	}

	/**
	 * eternal contains itself
	 */
	default boolean contains(LongInterval b) {
		if (this == b) return true;
		long as = start();
		if (as == ETERNAL)
			return true;
		else {
			long bs = b.start();
			return /*bs != ETERNAL &&*/ bs >= as && b.end() <= end();
		}
	}

	default boolean containsRaw(LongInterval b) {
		return this == b || (b.start() >= start() && b.end() <= end());
	}


}
