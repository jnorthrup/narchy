package nars.task.util;

import jcog.Util;
import jcog.tree.rtree.HyperRegion;
import nars.Task;
import nars.task.Tasked;

import static nars.task.util.TaskRegion.nearestBetween;
import static nars.time.Tense.ETERNAL;

public interface TaskRegion extends HyperRegion, Tasked {

    /**
     * relative to time sameness (1)
     */
    float FREQ_SAMENESS_IMPORTANCE = 0.25f;
    /**
     * relative to time sameness (1)
     */
    float CONF_SAMENESS_IMPORTANCE = 0.05f;

    static long nearestBetween(long s, long e, long when) {
        assert (when != ETERNAL);

        if (s == ETERNAL) {
            return when;
        } else if (when < s || e == s) {
            return s; //point or at or beyond the start
        } else if (when > e) {
            return e; //at or beyond the end
        } else {
            return when; //internal
        }
    }

    static long furthestBetween(long s, long e, long when) {
        assert (when != ETERNAL);

        if (s == ETERNAL) {
            return when;
        } else if (when < s || e == s) {
            return e; //point or at or beyond the start
        } else if (when > e) {
            return s; //at or beyond the end
        } else {
            //internal, choose most distant endpoint
            if (Math.abs(when - s) > Math.abs(when - e))
                return s;
            else
                return e;
        }
    }

    @Override
    boolean equals(Object obj);

    @Override
    int hashCode();

    long start();

    long end();

    default long mid() {
        return (start() + end()) / 2;
    }

    default long nearestTimeTo(long when) {
        return nearestBetween(start(), end(), when);
    }

    default long furthestTimeTo(long when) {
        return furthestBetween(start(), end(), when);
    }

    @Override
    default double cost() {
        return timeCost() * freqCost() * confCost();
    }

    @Override
    default double perimeter() {
        return timeCost() + freqCost() + confCost();
    }


    default float timeCost() {
        return (float) (1 + range(0));
    }

    default float freqCost() {
        float d = (float) range(1);
        return 1 + d /* * timeCost()*/ * FREQ_SAMENESS_IMPORTANCE;
    }

    default float confCost() {
        float d = (float) range(2);
        return 1 + d /* * timeCost()*/ * CONF_SAMENESS_IMPORTANCE;
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
                    float tc = tr.conf();
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
                    long ns = Math.min(ts, es);
                    long ne = Math.max(te, ee);
//                    }
                    return new TasksRegion( ns, ne,
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
        //        for (int i = 0; i < d; i++)
        //            if (coordF(false, i) > x.coordF(true, i) ||
        //                    coordF(true, i) < x.coordF(false, i))
        //                return false;
        //        return true;
        if (x instanceof TimeRange) {
            TimeRange t = (TimeRange) x;
            return !((start() > t.end) || (end() < t.start));
        } else {
            TaskRegion t = (TaskRegion) x;
            if ((start() > t.end()) || (end() < t.start()))
                return false;
            if ((coordF(false, 1) > t.coordF(true, 1)) || (coordF(true, 1) < t.coordF(false, 1)))
                return false;
            return (!(coordF(false, 2) > t.coordF(true, 2))) && (!(coordF(true, 2) < t.coordF(false, 2)));
        }
    }

    @Override
    default boolean contains(HyperRegion x) {
        if (x == this) return true;

        //    default boolean contains(HyperRegion<X> x) {
        //        int d = dim();
        //        for (int i = 0; i < d; i++)
        //            if (coordF(false, i) > x.coordF(false, i) ||
        //                    coordF(true, i) < x.coordF(true, i))
        //                return false;
        //        return true;
        //    }
        if (x instanceof TimeRange) {
            TimeRange t = (TimeRange) x;
            return !((start() > t.start) || (end() < t.end));
        } else {
            TaskRegion t = (TaskRegion) x;
            if ((start() > t.start()) || (end() < t.end()))
                return false;
            if ((coordF(false, 1) > t.coordF(false, 1)) || (coordF(true, 1) < t.coordF(true, 1)))
                return false;
            return (!(coordF(false, 2) > t.coordF(false, 2))) && (!(coordF(true, 2) < t.coordF(true, 2)));
        }
    }


    @Override
    default double coord(boolean maxOrMin, int dimension) {
        return coordF(maxOrMin, dimension);
    }

    @Override
    float coordF(boolean maxOrMin, int dimension);

}
