package nars.task.util;

import jcog.Util;
import jcog.math.Interval;
import jcog.tree.rtree.HyperRegion;
import nars.Task;
import nars.task.Tasked;

import java.util.List;

import static nars.time.Tense.ETERNAL;

public interface TaskRegion extends HyperRegion, Tasked {


    /**
     * cost of stretching a node by time
     */
    float TIME_COST = 1f;
    /**
     * cost of stretching a node by freq
     */
    float FREQ_COST = 4f;
    /**
     * cost of stretching a node by conf
     */
    float CONF_COST = 0.1f;

    /**
     * nearest point between starts and ends (inclusive) to the point 'x'
     */
    @Deprecated
    static long theNearestTimeWithin(long x, long starts, long ends) {
        assert (ends >= starts);

        if (x == ETERNAL) {
            if (starts == ETERNAL)
                return ETERNAL; //the avg of eternal with eternal produces 0 which is not right
            else
                return (starts + ends) / 2; //midpoint
        } else if (x <= starts) {
            return starts; //point or at or beyond the start
        } else if (x >= ends) {
            return ends; //at or beyond the end
        } else {
            return x; //internal: x is between starts,ends
        }
    }

//    static long furthestBetween(long s, long e, long when) {
//        assert (when != ETERNAL);
//
//        if (s == ETERNAL) {
//            return when;
//        } else if (when < s || e == s) {
//            return e; //point or at or beyond the start
//        } else if (when > e) {
//            return s; //at or beyond the end
//        } else {
//            //internal, choose most distant endpoint
//            if (Math.abs(when - s) > Math.abs(when - e))
//                return s;
//            else
//                return e;
//        }
//    }

    @Override
    boolean equals(Object obj);

    @Override
    int hashCode();

    long start();

    long end();

    default long mid() {
        long s = start();
        return s == ETERNAL ? ETERNAL : (s + end()) / 2L;
    }

    default long theNearestTimeWithin(long a, long b) {
        if (a == b || a == ETERNAL)
            return a;

        long m = mid();
        if (m == ETERNAL)
            return (a + b) / 2L;


        if (Math.abs(m - a) <= Math.abs(m - b))
            return a;
        else
            return b;
    }

    /**
     * amount of time this spans for
     */
    default long range() {
        long s = start();
        return s != ETERNAL ? end() - s : 0;
    }

    /**
     * if ends after the given time
     */
    default boolean isAfter(long when) {
        long e = end();
        return e == ETERNAL || e > when;
    }

    /**
     * if starts before the given time
     */
    default boolean isBefore(long when) {
        long s = start();
        return s == ETERNAL || s < when;
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
        if (start != ETERNAL) {
            if (start == when)
                return true;
            if (when >= start) {
                if (when <= end())
                    return true;
            }
            return false;
        } else {
            return true;
        }
    }

    default long myNearestTimeTo(final long x) {
        long s = start();
        if (s == ETERNAL)
            return ETERNAL;
        long e = end();
        if (s <= x && e >= x)
            return x;
        else {
            long m = (s+e)/2L;
            if (Math.abs(m - x) <= Math.abs(m - x))
                return s; //closer to start
            else
                return e; //closer to end
        }
    }

    /**
     * to the interval [x,y]
     * <p>
     * TODO verify if these semantics make sense any more
     */
    default long myNearestTimeTo(final long a, final long b) {

        assert (b >= a && (a != ETERNAL || a == b));

        if (a == ETERNAL)
            return mid();

        long s = this.start();
        if (s == ETERNAL)
            return (a + b) / 2L; //use midpoint of the two if this task is eternal

        long e = this.end();
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

//    /**
//     * TODO see if this can be made faster
//     */
//    default long distanceTo(long start, long end) {
//        assert (start != ETERNAL);
//
//        if (start == end) {
//            return distanceTo(start);
//        } else {
//            long s = this.start();
//            if (s == ETERNAL) return 0;
//
//
//            long e = this.end();
//
//            if (Interval.intersectLength(s, e, start, end) >= 0)
//                return 0; //intersects
//            else {
//                return Interval.unionLength(s, e, start, end) - (end - start) - (e - s);
//            }
//        }
//    }

    static long[] range(List<? extends TaskRegion> ie) {
        long start = Long.MAX_VALUE, end = Long.MIN_VALUE;
        int n = ie.size();
        for (int i = 0; i < n; i++) {
            TaskRegion x = ie.get(i);
            long s = x.start();
            if (s != ETERNAL) {
                if (s < start) start = s;
                long e = x.end();
                if (e > end) end = e;
            }
        }
        if (start == Long.MAX_VALUE) //nothing or all eternal
            return Task.ETERNAL_ETERNAL;
        else
            return new long[]{start, end};
    }

//    /** untested */
//    default long medianTimeTo(long a,long b) {
//        long n = nearestTimeTo(a);
//        if (n == ETERNAL)
//            return n;
//        if (a!=b)
//            n = Math.min(n, nearestTimeTo(b));
//
//        long f = furthestTimeTo(a);
//        assert(f!=ETERNAL);
//        if (a!=b)
//            f = Math.max(f, furthestTimeTo(b));
//
//        return (n+f)/2;
//    }
//    /** untested */
//   default long furthestTimeOf(long a, long b) {
//
//        if (a == b) return a;
//
//        assert(a < b);
//
//        long m = mid();
//        if (a == ETERNAL)
//            return m;
//        else {
//            if (Math.abs(a-m) >= Math.abs(b-m))
//                return a;
//            else
//                return b;
//        }
//    }
//
//    /** untested */
//    default long furthestTimeTo(long when) {
//
//
//        long s = start();
//        if (s == ETERNAL) {
//            return when;
//        } else {
//            long e = end();
//            if (s == e)
//                return s;
//
//            if (when == ETERNAL)
//                return mid();
//
//            long ds = Math.abs(s - when);
//
//            long de = Math.abs(e - when);
//            if (ds >= de) return s;
//            else return e;
//        }
//    }

    @Override
    default double cost() {
        return timeCost() * freqCost() * confCost();
    }

    @Override
    default double perimeter() {
        return timeCost() + freqCost() + confCost();
    }


    default float timeCost() {

        return 1 + (float) range(0) * TIME_COST;

        //return 1 + (float) Math.log(range(0)+1) /* * 1 */;
    }

    default float freqCost() {
        return 1 + (float) range(1) * FREQ_COST;
    }

    default float confCost() {
        return 1 + (float) range(2) * CONF_COST;
    }

    @Override
    default double range(final int dim) {
        return /*Math.abs*/(coordF(true, dim) - coordF(false, dim));
    }

    @Override
    default int dim() {
        return 3;
    }

    @Override
    default TaskRegion mbr(HyperRegion r) {
        if (this == r)
            return this;
        else {
            if (r instanceof Task) {
                //accelerated mbr
                Task er = (Task) r;
                float ef = er.freq();
                float ec = er.conf();
                long es = er.start();
                long ee = er.end();
                if (this instanceof Task) {
                    Task tr = (Task) this;
                    float tf = tr.freq();
                    float f0, f1;


                    if (tf <= ef) {
                        f0 = tf;
                        f1 = ef;
                    } else {
                        f0 = ef;
                        f1 = tf;
                    }
                    float c0;
                    float c1;
                    float tc = tr.conf();
                    if (tc <= ec) {
                        c0 = tc;
                        c1 = ec;
                    } else {
                        c0 = ec;
                        c1 = tc;
                    }
                    long ts = start();
                    long te = end();
                    //                    if (ts == es && te == ee) {
                    //may not be safe:
//                        if (tf == ef && tc == ec)
//                            return this; //identical taskregion, so use this
//                        else {
//                            ns = ts;
//                            ne = te;
//                        }
//                    } else {
                    //                    }
                    return new TasksRegion(Math.min(ts, es), Math.max(te, ee),
                            f0, f1, c0, c1
                    );
                } else {
                    return new TasksRegion(
                            Math.min(start(), es), Math.max(end(), ee),
                            Util.min(coordF(false, 1), ef),
                            Util.max(coordF(true, 1), ef),
                            Util.min(coordF(false, 2), ec),
                            Util.max(coordF(true, 2), ec)
                    );
                }
            } else {
                TaskRegion er = (TaskRegion) r;
                return new TasksRegion(
                        Math.min(start(), er.start()), Math.max(end(), er.end()),
                        Util.min(coordF(false, 1), er.coordF(false, 1)),
                        Util.max(coordF(true, 1), er.coordF(true, 1)),
                        Util.min(coordF(false, 2), er.coordF(false, 2)),
                        Util.max(coordF(true, 2), er.coordF(true, 2))
                );
            }
        }
    }

    @Override
    default boolean intersects(HyperRegion x) {
        if (x == this) return true;
        long start = start();
        if (x instanceof TimeRange) {
            TimeRange t = (TimeRange) x;
            return start <= t.end && end() >= t.start;
        } else {
            TaskRegion t = (TaskRegion) x;
            return start <= t.end() &&
                    end() >= t.start() &&
                    coordF(false, 1) <= t.coordF(true, 1) &&
                    coordF(true, 1) >= t.coordF(false, 1) &&
                    coordF(false, 2) <= t.coordF(true, 2) &&
                    coordF(true, 2) >= t.coordF(false, 2);
        }
    }

    @Override
    default boolean contains(HyperRegion x) {
        if (x == this) return true;

        long start = start();
        if (x instanceof TimeRange) {
            TimeRange t = (TimeRange) x;
            return start <= t.start && end() >= t.end;
        } else {
            TaskRegion t = (TaskRegion) x;
            return
                    start <= t.start() && end() >= t.end() &&
                            coordF(false, 1) <= t.coordF(false, 1) &&
                            coordF(true, 1) >= t.coordF(true, 1) &&
                            coordF(false, 2) <= t.coordF(false, 2) &&
                            coordF(true, 2) >= t.coordF(true, 2);
        }
    }


    @Override
    default double coord(boolean maxOrMin, int dimension) {
        return coordF(maxOrMin, dimension);
    }

    @Override
    float coordF(boolean maxOrMin, int dimension);

    default boolean isDuring(long start, long end) {
        return Interval.intersect(start, end, start(), end())!=null;
    }
}
