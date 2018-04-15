package jcog.math;

/** pair of 64-bit signed long integers representing an interval.
 *  a special 'ETERNAL' value represents (-infinity,+infinity)
 *
 *  TODO allow (-infinity, ..x) and (x, +infinity)
 * */
public interface LongInterval {
    
    long ETERNAL = Long.MIN_VALUE;
    long TIMELESS = Long.MAX_VALUE;
    
    long start();

    long end();

    default long mid() {
        long s = start();
        return s == ETERNAL ? ETERNAL : (s + end()) / 2L;
    }

    /** return number of elements between a and b inclusively. x..x is length 1.
     *  if b &lt; a, then length is 0.  9..10 has length 2.
     */
    default long range() {
        long s = start();
        if (s == ETERNAL)
            throw new ArithmeticException("ETERNAL range calculated");
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

        //test if inside
        long mid = (s + e) / 2;
        if (s >= a && s <= b) {
            return mid; //the mid-point of this task directly
        }

        //compare to which of the two ends is closest the midpoint
        if (Math.abs(mid - a) <= Math.abs(mid - b))
            return a;
        else
            return b;
    }

    /**
     * if starts after the given time
     */
    default boolean isAfter(long when) {
        return isAfter(when, 0);
    }

    default boolean isAfter(long when, int margin) {
        long e = start();
        return e == ETERNAL || e - margin > when;
    }

    /**
     * if ends before the specified time
     */
    default boolean isBefore(long when) {
        return isBefore(when, 0);
    }

    default boolean isBefore(long when, int margin) {
        long e = end();
        return e == ETERNAL || e + margin < when;
    }

    default boolean isDuringAny(long... when) {
        if (when.length == 2 && when[0] == when[1]) return isDuring(when[0]); //quick
        for (long x : when) {
            if (isDuring(x)) return true;
        }
        return false;
    }

    default boolean isDuringAll(long... when) {
        if (when.length == 2 && when[0] == when[1]) return isDuring(when[0]); //quick
        for (long x : when) {
            if (!isDuring(x)) return false;
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
     * finds the nearest point inside this interval to the provided point, which may
     * intersect or not with this interval.
     */
    default long nearestPointInternal(long x) {
        long s = start();
        if (s == ETERNAL)
            return ETERNAL;
        long e = end();
        if (s <= x && e >= x)
            return x;
        else {
            long m = (s + e) / 2L;
            if (Math.abs(m - x) <= Math.abs(m - x))
                return s; //closer to start
            else
                return e; //closer to end
        }
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
            return (a + b) / 2L; //midpoint of the contained range surrounded by this
        } else if (a < s && b > e) {
            return (s + e) / 2L; //midpoint of this within the range surrounding this
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

    default long minDistanceTo(long when) {

        long s = start();
        if (s == ETERNAL)
            return 0;

        assert (when != ETERNAL);
        long e = end();
        if (s <= when && e >= when)
            return 0;
        long d = Math.abs(s - when);
        if (s == e)
            return d;
        else
            return Math.min(d, Math.abs(e - when));
    }

    /** if the task intersects (ex: occurrs during) the specified interval,
     *  returned distance is zero, regardless of how far it may extend before or after it */
    default long minDistanceTo(long a, long b) {

        assert (b >= a): a + " > " + b;

        if (a == ETERNAL) {
            //throw new TODO();
            return 0;
        }

        long s = start();
        if (s == ETERNAL)
            return 0;

        long e = end();
        if (intersects(a, b)) {
            return 0; //contains that interval
        } else {
            long sa = Math.abs(s - a);
            if (a == b) {
                if (s == e) {
                    return sa;
                } else {
                    return Math.min(sa, Math.abs(e - b));
                }
            } else {
                long sab = Math.min(sa, Math.abs(s - b));
                if (s == e) {
                    return sab;
                } else {
                    return Math.min(sab, Math.min(Math.abs(e - a), Math.abs(e - b)));
                }
            }
        }
    }

    default long meanDistanceTo(long x) {
        long start = start();
        if (start == ETERNAL) return 0;
        long end = end();
        long distToStart = Math.abs(start - x);
        if (end == start) {
            return distToStart;
        } else {
            long distToEnd = Math.abs(end - x);
            return (distToStart + distToEnd)/2L;
        }
    }

    default boolean intersects(long rangeStart, long rangeEnd) {
        if (rangeStart == ETERNAL)
            return true;
        long start = start();
        return (start == ETERNAL) || (rangeEnd >= start && rangeStart <= end());
    }

    default boolean contains(long rangeStart, long rangeEnd) {
        long start = start();
        return (start == ETERNAL) || (rangeStart >= start && rangeEnd <= end());
    }
}
