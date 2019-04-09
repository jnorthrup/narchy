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
    static final float HALF_EPSILON = Param.TRUTH_EPSILON / 2;


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

    static TasksRegion mbr(TaskRegion r, Task x) {
        return TasksRegion.mbr(r, x.start(), x.end(), x.freq(), x.conf());
    }

    static TaskRegion mbr(TaskRegion x, TaskRegion y) {
        if (x == y) return x;

        if (y instanceof Task) {
            assert (!(x instanceof Task)) : "mbr(task,task) should force creation of TasksRegion";
            return TaskRegion.mbr(x, (Task) y);
        }

        TasksRegion z = new TasksRegion(
                Math.min(x.start(), y.start()), Math.max(x.end(), y.end()),
                Math.min(x.freqMin(), y.freqMin()),
                Math.max(x.freqMax(), y.freqMax()),
                Math.min(x.confMin(), y.confMin()),
                Math.max(x.confMax(), y.confMax())
        );
        //may only be valid for non-Tasks
        if (x instanceof TasksRegion && z.equals(x))
            return x; //contains or equals y
        else if (y instanceof TasksRegion && z.equals(y))
            return y; //contained by y
        else
            return z; //enlarged (intersecting or disjoint)
    }

    @Override
    boolean equals(Object obj);


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
    int hashCode();

    default float expectation() {
        return TruthFunctions.expectation(freqMean(), confMin());
    }

    default short[] why() {
        return ArrayUtils.EMPTY_SHORT_ARRAY;
    }

    @Override
    default double cost(final int dim) {
        switch (dim) {
            case 0:
                return range(0) * TIME_COST;
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
                return 1 + (end() - start());
            case 1:
                return HALF_EPSILON + (freqMax() - freqMin());
            case 2:
                return HALF_EPSILON + (confMax() - confMin());
            default:
                throw new UnsupportedOperationException();
        }

    }

    @Override
    default int dim() {
        return 3;
    }

    @Override
    default TaskRegion mbr(HyperRegion r) {
        return mbr(this, (TaskRegion) r);
    }

    @Override
    default /* final */ boolean intersects(HyperRegion x) {
        if (x == this) return true;
        long start = start();
        if (start == ETERNAL) return true;

        TaskRegion t = (TaskRegion) x;
        if (t.intersects(start, end())) {
            return freqMin() <= t.freqMax()+HALF_EPSILON &&
                   freqMax() >= t.freqMin()-HALF_EPSILON &&
                   confMin() <= t.confMax()+HALF_EPSILON &&
                   confMax() >= t.confMin()-HALF_EPSILON;
        }
        return false;
    }

    @Override
    default /* final */ boolean contains(HyperRegion x) {
        if (x == this) return true;
        TaskRegion t = (TaskRegion) x;
        if (LongInterval.super.contains(((LongInterval)t))) {
            return freqMin() <= t.freqMin()+HALF_EPSILON &&
                    freqMax() >= t.freqMax()-HALF_EPSILON &&
                    confMin() <= t.confMin()+HALF_EPSILON &&
                    confMax() >= t.confMax()-HALF_EPSILON;
        }
        return false;
    }

    default double coord(int dimension, boolean maxOrMin) {
        //return coordF(dimension, maxOrMin);
        throw new UnsupportedOperationException();
    }

    default boolean intersectsConf(float cMin, float cMax) {
        return (cMin <= confMax() && cMax >= confMin());
    }

    default boolean containsConf(float cMin, float cMax) {
        return (confMin() <= cMin && confMax() >= cMax);
    }

    default float freqMean() {
        return (freqMin() + freqMax()) / 2;
    }
    default float confMean() {
        return (confMin() + confMax()) / 2;
    }

    float freqMin();
    float freqMax();
    float confMin();
    float confMax();

}
