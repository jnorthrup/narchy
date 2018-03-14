package nars.task.util;

import jcog.Util;
import jcog.math.LongInterval;
import jcog.tree.rtree.HyperRegion;
import nars.Task;
import nars.task.Tasked;
import nars.time.Tense;
import nars.truth.TruthFunctions;
import org.apache.commons.lang3.ArrayUtils;

import java.util.List;

import static nars.truth.TruthFunctions.c2wSafe;

/** 3d cuboid region:
 *      time            start..end              64-bit signed long
 *      frequency:      min..max in [0..1]      32-bit float
 *      confidence:     min..max in [0..1)      32-bit float
 */
public interface TaskRegion extends HyperRegion, Tasked, LongInterval {

    /**
     * cost of stretching a node by time
     */
    float TIME_COST = 1f;

    /**
     * cost of stretching a node by freq
     */
    float FREQ_COST = 1f;

    /**
     * cost of stretching a node by conf
     */
    float CONF_COST = 1f;


    @Override
    boolean equals(Object obj);

    @Override
    int hashCode();


    default float expectation() {
        return TruthFunctions.expectation(freqMean(), confMin());
    }

    default short[] cause() {
        return ArrayUtils.EMPTY_SHORT_ARRAY;
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

    static long[] range(List<? extends LongInterval> ie) {
        long start = Long.MAX_VALUE, end = Long.MIN_VALUE;
        int n = ie.size();
        for (int i = 0; i < n; i++) {
            LongInterval x = ie.get(i);
            long s = x.start();
            if (s != ETERNAL) {
                if (s < start) start = s;
                long e = x.end();
                if (e > end) end = e;
            }
        }
        if (start == Long.MAX_VALUE) //nothing or all eternal
            return Tense.ETERNAL_ETERNAL;
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
        double x = timeCost() * freqCost() * confCost();
        assert(x==x);
        return x;
    }

    @Override
    default double perimeter() {
        return timeCost() + freqCost() + confCost();
    }


    default float timeCost() {

        return ((float) range(0)) * TIME_COST;

//        float r = (float) range(0);
//        return r == 0 ? 0 : (float) (Math.log(1 + r) * TIME_COST);
    }

    default float freqCost() {
        return ((float) range(1)) * FREQ_COST;
    }

    default float confCost() {
        return ((float) range(2)) * CONF_COST;
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
        if (contains(r))
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
            if (x instanceof TimeConfRange) {
                TimeConfRange t = (TimeConfRange) x;
                return start <= t.end && end() >= t.start && confMin() <= t.cMax && confMax() >= t.cMin;
            } else {
                TimeRange t = (TimeRange) x;
                return start <= t.end && end() >= t.start;
            }
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
            if (x instanceof TimeConfRange) {
                TimeConfRange t = (TimeConfRange) x;
                return start <= t.start && end() >= t.end && confMin() <= t.cMin && confMax() >= t.cMax;
            } else {
                TimeRange t = (TimeRange) x;
                return start <= t.start && end() >= t.end;
            }
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

    default boolean intersectsConf(float cMin, float cMax) {
        return (cMin <= confMax() && cMax >= confMin());
    }

    default boolean containsConf(float cMin, float cMax) {
        return (confMin() <= cMin && confMax() >= cMax);
    }

    default float freqMin() {
        return coordF(false, 1);
    }

    default float freqMean() {
        return (freqMin() + freqMax()) * 0.5f;
    }

    default float freqMax() {
        return coordF(true, 1);
    }

    default float confMin() {
        return coordF(false, 2);
    }

    default float confMax() {
        return coordF(true, 2);
    }

    default float eviInteg() {
        return range() * (c2wSafe(confMin()) + c2wSafe(confMax())) / 2f;
    }


}
