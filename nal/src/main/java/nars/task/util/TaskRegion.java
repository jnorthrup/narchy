package nars.task.util;

import jcog.math.LongInterval;
import jcog.tree.rtree.HyperRegion;
import jcog.util.ArrayUtils;
import nars.Task;
import nars.task.Tasked;
import nars.truth.Truth;
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
                return 1 + (freqMax() - freqMin());
            case 2:
                return 1 + (confMax() - confMin());
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
            return freqMinI() <= t.freqMaxI() &&
                   freqMaxI() >= t.freqMinI() &&
                   confMinI() <= t.confMaxI() &&
                   confMaxI() >= t.confMinI();
        }
        return false;
    }

    @Override
    default /* final */ boolean contains(HyperRegion x) {
        if (x == this) return true;
        TaskRegion t = (TaskRegion) x;
        if (LongInterval.super.contains(((LongInterval)t))) {
            return freqMinI() <= t.freqMinI() &&
                    freqMaxI() >= t.freqMaxI() &&
                    confMinI() <= t.confMinI() &&
                    confMaxI() >= t.confMaxI();
        }
        return false;
    }

    default double coord(int dimension, boolean maxOrMin) {
        //return coordF(dimension, maxOrMin);
        throw new UnsupportedOperationException();
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

    default int i(boolean freqOrConf, boolean maxOrMin) {
        float v = freqOrConf ? (maxOrMin ? freqMax() : freqMin()) : (maxOrMin ? confMax() : confMin());
        return Truth.truthToInt(v);
    }

    default int freqMinI() {
        return i(true, false);
    }
    default int freqMaxI() {
        return i(true, true);
    }
    default int confMinI() {
        return i(false, false);
    }
    default int confMaxI() {
        return i(false, true);
    }

}
