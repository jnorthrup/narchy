package nars.task.util;

import jcog.math.LongInterval;
import jcog.tree.rtree.HyperRegion;
import jcog.util.ArrayUtils;
import nars.Param;
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
    float TIME_COST = 2f;

    /**
     * proportional value of splitting a node by freq
     */
    float FREQ_COST = 0.5f;

    /**
     * proportional value of splitting a node by conf
     */
    float CONF_COST = 0.125f;

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
        switch (dim) {
            case 0:
                return (1+range(0)) * TIME_COST;
            case 1:
                return range(1) * FREQ_COST;
            case 2:
                return range(2) * CONF_COST;
        }
        throw new UnsupportedOperationException();
    }


    @Override
    default double range(final int dim) {
        switch (dim) {
            case 0:
                return end() - start();
            default:
                return Math.max(Param.TRUTH_EPSILON, /*Math.abs*/(coordF(dim, true) - coordF(dim, false)));
        }

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
            return mbr(this, (TaskRegion) r);
        }
    }

    static TasksRegion mbr(TaskRegion r, Task x) {
        return TasksRegion.mbr(r, x.start(), x.end(), x.freq(), x.conf());
    }

    static TaskRegion mbr(TaskRegion x, TaskRegion y) {
        if (y instanceof Task) {
            assert(!(x instanceof Task)): "mbr(task,task) should force creation of TasksRegion";
            return TaskRegion.mbr(x, (Task) y);
        }

        TasksRegion z = new TasksRegion(
                Math.min(x.start(), y.start()), Math.max(x.end(), y.end()),
                Math.min(x.coordF(1, false), y.coordF(1, false)),
                Math.max(x.coordF(1, true), y.coordF(1, true)),
                Math.min(x.coordF(2, false), y.coordF(2, false)),
                Math.max(x.coordF(2, true), y.coordF(2, true))
        );
        if (x instanceof TasksRegion && z.equals(x))
            return x; //contains or equals y
        else if (y instanceof TasksRegion && z.equals(y))
            return y; //contained by y
        else
            return z; //enlarged (intersecting or disjoint)
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
                            coordF(1, false) <= t.coordF(1, false) &&
                            coordF(1, true) >= t.coordF(1, true) &&
                            coordF(2, false) <= t.coordF(2, false) &&
                            coordF(2, true) >= t.coordF(2, true);
        }
    }

    default double coord(int dimension, boolean maxOrMin) {
        return coordF(dimension, maxOrMin);
    }

    @Override
    float coordF(int dimension, boolean maxOrMin);

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
