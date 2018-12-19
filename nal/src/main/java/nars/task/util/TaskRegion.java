package nars.task.util;

import jcog.math.LongInterval;
import jcog.tree.rtree.HyperRegion;
import jcog.util.ArrayUtils;
import nars.Task;
import nars.task.Tasked;
import nars.truth.func.TruthFunctions;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * 3d cuboid region:
 * time            start..end              64-bit signed long
 * frequency:      min..max in [0..1]      32-bit float
 * confidence:     min..max in [0..1)      32-bit float
 */
public interface TaskRegion extends HyperRegion, Tasked, LongInterval {

    /**
     * proportional value of splitting a node by time
     */
    float TIME_COST = 1f;

    /**
     * proportional value of splitting a node by freq
     */
    float FREQ_COST = 0.5f;

    /**
     * proportional value of splitting a node by conf
     */
    float CONF_COST = 0.05f;

    static Consumer<TaskRegion> asTask(Consumer<? super Task> each) {
        return r -> {
            Task x = r.task();
            if (x != null && !x.isDeleted()) each.accept(x);
        };
    }

    static Predicate<TaskRegion> asTask(Predicate<? super Task> each) {
        return r -> {
            Task x = r.task();
            if (x != null && !x.isDeleted()) return each.test(x);

            return true;
        };
    }


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


//    @Override
//    default double cost() {
//        double x = timeCost() * freqCost() * confCost();
//        assert (x == x);
//        return x;
//    }

//    @Override
//    default double perimeter() {
//        return timeCost() + freqCost() + confCost();
//    }


    @Override
    default double cost(final int dim) {
        switch(dim) {
            case 0:
                return ((float) range(0)) * TIME_COST;
            case 1:
                return ((float) range(1)) * FREQ_COST;
            case 2:
                return ((float) range(2)) * CONF_COST;
        }
        throw new UnsupportedOperationException();
    }


    @Override
    default double range(final int dim) {
        switch (dim) {
            case 0:
                return end() - start();
            default:
                return /*Math.abs*/(coordF(dim, true) - coordF(dim, false));
        }

    }

    @Override
    default int dim() {
        return 3;
    }

    @Override
    default TaskRegion mbr(HyperRegion r) {
        if (this == r || contains(r))
            return this;
        else {

            if (r instanceof Task) {

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
                    return new TasksRegion(Math.min(start(), es), Math.max(end(), ee),
                            f0, f1, c0, c1
                    );
                } else {
                    return new TasksRegion(
                            Math.min(start(), es), Math.max(end(), ee),
                            Math.min(coordF(1, false), ef),
                            Math.max(coordF(1, true), ef),
                            Math.min(coordF(2, false), ec),
                            Math.max(coordF(2, true), ec)
                    );
                }
            } else {
                TaskRegion er = (TaskRegion) r;
                return new TasksRegion(
                        Math.min(start(), er.start()), Math.max(end(), er.end()),
                        Math.min(coordF(1, false), er.coordF(1, false)),
                        Math.max(coordF(1, true), er.coordF(1, true)),
                        Math.min(coordF(2, false), er.coordF(2, false)),
                        Math.max(coordF(2, true), er.coordF(2, true))
                );
            }
        }
    }

    @Override
    default boolean intersects(HyperRegion x) {
        if (x == this) return true;
        long start = start();
        long end = end();
        if (x instanceof TimeRange) {
            if (x instanceof TimeConfRange) {
                TimeConfRange t = (TimeConfRange) x;
                return start <= t.end && end >= t.start && confMin() <= t.cMax && confMax() >= t.cMin;
            } else {
                TimeRange t = (TimeRange) x;
                return start <= t.end && end >= t.start;
            }
        } else {
            TaskRegion t = (TaskRegion) x;
            return start <= t.end() &&
                    end >= t.start() &&
                    coordF(1, false) <= t.coordF(1, true) &&
                    coordF(1, true) >= t.coordF(1, false) &&
                    coordF(2, false) <= t.coordF(2, true) &&
                    coordF(2, true) >= t.coordF(2, false);
        }
    }

    @Override
    default boolean contains(HyperRegion x) {
        if (x == this) return true;

        long start = start();
        long end = end();
        if (x instanceof TimeRange) {
            if (x instanceof TimeConfRange) {
                TimeConfRange t = (TimeConfRange) x;
                return start <= t.start && end >= t.end && confMin() <= t.cMin && confMax() >= t.cMax;
            } else {
                TimeRange t = (TimeRange) x;
                return start <= t.start && end >= t.end;
            }
        } else {
            TaskRegion t = (TaskRegion) x;
            return
                    start <= t.start() && end >= t.end() &&
                            coordF(1, false) <= t.coordF(1, false) &&
                            coordF(1, true) >= t.coordF(1, true) &&
                            coordF(2, false) <= t.coordF(2, false) &&
                            coordF(2, true) >= t.coordF(2, true);
        }
    }


    @Override
    double coord(int dimension, boolean maxOrMin);

    default boolean intersectsConf(float cMin, float cMax) {
        return (cMin <= confMax() && cMax >= confMin());
    }

    default boolean containsConf(float cMin, float cMax) {
        return (confMin() <= cMin && confMax() >= cMax);
    }

    default float freqMin() {
        return coordF(1, false);
    }

    default float freqMean() {
        return (freqMin() + freqMax()) * 0.5f;
    }

    default float freqMax() {
        return coordF(1, true);
    }

    default float confMin() {
        return coordF(2, false);
    }

    default float confMax() {
        return coordF(2, true);
    }


//    /**
//     * intersects only the time dimension
//     */
//    default boolean intersectsTime(LongInterval x) {
//        return this == x || intersects(x.start(), x.end());
//    }

}
