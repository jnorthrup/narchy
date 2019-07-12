package jcog.math;

import jcog.Util;
import org.eclipse.collections.impl.block.factory.Comparators;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * An immutable inclusive longerval a..b implementation of LongInterval
 */
public class Longerval implements LongInterval, Comparable<Longerval> {

    final static Comparator<Longerval> comparator = Comparators
            .byLongFunction((Longerval l) -> l.start)
            .thenComparingLong((l) -> l.end);

    private static final Longerval Eternal = new Longerval(ETERNAL, ETERNAL);

    public final long start, end;

    public Longerval(long start) {
        this(start, start);
    }

    public Longerval(long start, long end) {
        assert (start != TIMELESS && end >= start);
        this.start = start;
        this.end = end;
    }

    @Nullable
    public static Longerval intersection(long myA, long myB, long otherA, long otherB) {
        return new Longerval(myA, myB).intersection(otherA, otherB);
    }


    /** cs,ce = container;   xs,xe = possibly contained */
    public static boolean contains(long cs, long ce, long xs, long xe) {
        return xs >= cs && xe <= ce;
    }


//    @Nullable
//    public static long[] intersectionArray(long myA, long myB, long otherA, long otherB) {
//        return intersectionArray(myA, myB, otherA, otherB, null);
//    }

    @Override
    public final long start() {
        return start;
    }

    @Override
    public final long end() {
        return end;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        Longerval other = (Longerval) o;
        return this.start == other.start && this.end == other.end;
    }

    @Override
    public int hashCode() {
        return Util.hashCombine(start, end);
    }

    public Longerval union(Longerval other) {
        if (this == other) return this;
        Longerval u = union(other.start, other.end);
        return u.equals(other) ? other : u; //equality to this is already tested
    }

    public final Longerval union(long bs, long be) {
        long as = this.start;
        if (as == ETERNAL || bs == ETERNAL)
            return Longerval.Eternal;
        if (as == TIMELESS || bs == TIMELESS)
            throw new UnsupportedOperationException();
        long ae = this.end;
        return new Longerval(min(as, bs), max(ae, be));
    }

    @Nullable
    public final Longerval intersection(long bs, long be) {
        long as = this.start;
        if (as == ETERNAL || bs == ETERNAL)
            return Longerval.Eternal;
        if (as == TIMELESS || bs == TIMELESS)
            throw new UnsupportedOperationException();

        long ae = this.end;
        long s = max(as, bs), e = min(ae, be);
        return s > e ? null : ((s == as && e == ae) ? this : new Longerval(s, e));
    }


//	@Nullable public static Longerval intersect(long x1, long x2, long y1, long y2) {

    private long[] intervalArray() {
        return intervalArray(null);
    }

    private long[] intervalArray(@Nullable long[] target) {
        if (target == null)
            target = new long[2];
        target[0] = start;
        target[1] = end;
        return target;
    }

    @Nullable
    public static long[] intersectionArray(long myA, long myB, long otherA, long otherB, @Nullable long[] target) {
        @Nullable Longerval x = Longerval.intersection(myA, myB, otherA, otherB);
        return x == null ? null : x.intervalArray(target);
    }

    public static long[] unionArray(long myA, long myB, long otherA, long otherB) {
        return LongInterval.union(myA, myB, otherA, otherB).intervalArray();
    }

//    /**
//     * Return the longerval with elements from this not in other;
//     * other must not be totally enclosed (properly contained)
//     * within this, which would result in two disjoint longervals
//     * instead of the single one returned by this method.
//     */
//    public Longerval differenceNotProperlyContained(Longerval other) {
//        Longerval diff = null;
//
//        if (other.startsBeforeNonDisjoint(this)) {
//            diff = new Longerval(max(this.start, other.end + 1),
//                    this.end);
//        } else if (other.startsAfterNonDisjoint(this)) {
//            diff = new Longerval(this.start, other.start - 1);
//        }
//        return diff;
//    }

    @Override
    public String toString() {
        return start + ".." + end;
    }

    public long[] toArray() {
        return new long[]{start, end};
    }

    @Override
    public final int compareTo(Longerval x) {
        return this.equals(x) ?
                0
                :
                comparator.compare(this, x);
    }

}