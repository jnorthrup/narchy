package nars.task.util;

import jcog.Util;
import jcog.tree.rtree.HyperRegion;
import nars.Task;
import nars.task.Tasked;

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

    /** nearest point between starts and ends (inclusive) to the point 'x' */
    static long nearestTimeTo(long x, long starts, long ends) {
        assert(ends >= starts);

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
        return (start() + end()) / 2;
    }

    default long nearestTimeTo(long when) {
        return TaskRegion.nearestTimeTo(when, start(), end());
    }

//    default long furthestTimeTo(long when) {
//        return furthestBetween(start(), end(), when);
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

        //return 1 + (float) range(0) /* * 1 */;

        double dt = range(0);
        return 1 + (float) Math.log(dt+1) /* * 1 */;
    }

    default float freqCost() {
        return 1 + (float) range(1) * FREQ_SAMENESS_IMPORTANCE;
    }

    default float confCost() {
        return 1 + (float) range(2) * CONF_SAMENESS_IMPORTANCE;
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

}
