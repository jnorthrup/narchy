package nars.task.util;

import jcog.Util;
import jcog.math.LongInterval;
import jcog.tree.rtree.HyperRegion;
import jcog.util.ArrayUtil;
import nars.NAL;
import nars.Task;
import nars.task.Tasked;
import nars.truth.Truth;

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
    float FREQ_COST = 1f;

    /**
     * proportional value of splitting a node by conf
     */
    float CONF_COST = 1f;

    static Consumer<TaskRegion> asTask(Consumer<? super Task> each) {
        return r -> {
            Task x = _task(r);
            if (x != null) each.accept(x);
        };
    }

    static Predicate<TaskRegion> asTask(Predicate<? super Task> each) {
        return r -> {
            Task x = _task(r);
            return x == null || each.test(x);
        };
    }

    static Task _task(TaskRegion r) {
        return r instanceof Task ? (Task) r : r.task();
    }

    static TaskRegion mbr(TaskRegion x, Task y) {
        return x == y ? x : TasksRegion.mbr(x, y.start(), y.end(), y.freq(), y.conf());
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

//    default float expectation() {
//        return (float) TruthFunctions.expectation(freqMean(), confMin());
//    }

    @Override
    default double cost(final int dim) {
        switch (dim) {
            case 0:
                return (range(0)) * TIME_COST;
                //return Math.log(range(0)) * TIME_COST;
                //return Math.sqrt(range(0)) * TIME_COST;
            case 1:
                return (NAL.truth.TRUTH_EPSILON + range(1)) * FREQ_COST;
            case 2:
                return (NAL.truth.TASK_REGION_CONF_EPSILON + range(2)) * CONF_COST;
        }
        throw new UnsupportedOperationException();
    }

    @Override
    default double range(final int dim) {
        switch (dim) {
            case 0:
                return 1 + (end() - start());
            case 1:
                return (1 + freqMaxI() - freqMinI()) * NAL.truth.TRUTH_EPSILON;
            case 2:
                return (1 + confMaxI() - confMinI()) * NAL.truth.TASK_REGION_CONF_EPSILON;
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
        if (this == r) return this;

        if (r instanceof Task) {
            assert (!(this instanceof Task)) : "mbr(task,task) should force creation of TasksRegion";
            return TaskRegion.mbr(this, (Task) r);
        } else {
            TaskRegion R = (TaskRegion) r;
            if (contains(r))
                return this;
            else if (r.contains(this))
                return R;

            return new TasksRegion(
                Math.min(start(), R.start()), Math.max(end(), R.end()),
                Math.min(freqMinI(), R.freqMinI()), Math.max(freqMaxI(), R.freqMaxI()),
                Math.min(confMinI(), R.confMinI()), Math.max(confMaxI(), R.confMaxI())
            );
//            //may only be valid for non-Tasks
//            if (this instanceof TasksRegion && z.equals(this))
//                return this; //contains or equals y
//            else if (r instanceof TasksRegion && z.equals(r))
//                return (TaskRegion) r; //contained by y
//            else
//                return z; //enlarged (intersecting or disjoint)
        }
    }

    @Override
    default /* final */ boolean intersects(HyperRegion _y) {
        if (_y == this) return true;

        TaskRegion y = (TaskRegion) _y;
        if (LongInterval.super.intersects(y)) {
        //if (y.intersects(start(), end())) {

            int xca = confMinI(), ycb = y.confMaxI();
            if (xca <= ycb) {
                int xfa = freqMinI(), yfb = y.freqMaxI();
                if (xfa <= yfb) {


                    boolean xt = this instanceof Task, yt = _y instanceof Task;
                    if (xt && yt)
                        return true; //HACK shortcut since tasks currently only have one flat freq but could change with piecewise linear truth

                    int xcb = xt ? xca : confMaxI();
                    int yca = yt ? ycb : y.confMinI();
                    if (xcb >= yca) {
                        int xfb = xt ? xfa : freqMaxI();
                        int yfa = yt ? yfb : y.freqMinI();
                        return (xfb >= yfa);
                    }
                }
            }
        }
        return false;
    }

    @Override
    default /* final */ boolean contains(HyperRegion x) {
        if (x == this) return true;
        TaskRegion t = (TaskRegion) x;
        if (LongInterval.super.contains(((LongInterval)t))) {
            return
                confMinI() <= t.confMinI() &&
                confMaxI() >= t.confMaxI() &&
                freqMinI() <= t.freqMinI() &&
                freqMaxI() >= t.freqMaxI()
                ;
        }
        return false;
    }

    default double coord(int dimension, boolean maxOrMin) {
        switch (dimension) {
            case 0: return maxOrMin ? end() : start();
            case 1: return (maxOrMin ? freqMaxI() : freqMinI())*(NAL.truth.TRUTH_EPSILON);
            case 2: return (maxOrMin ? confMaxI() : confMinI())*(NAL.truth.TASK_REGION_CONF_EPSILON);
            default: return Double.NaN;
        }
    }

    default double center(int dimension) {
        switch (dimension) {
            case 0: return mid();
            case 1: return (freqMinI() + freqMaxI())*(0.5 * NAL.truth.TRUTH_EPSILON);
            case 2: return (confMinI() + confMaxI())*(0.5 * NAL.truth.TASK_REGION_CONF_EPSILON);
            default: return Double.NaN;
        }
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
        return Util.toInt(
            freqOrConf ?
                (maxOrMin ? freqMax() : freqMin()) :
                (maxOrMin ? confMax() : confMin()),

            freqOrConf ? Truth.hashDiscretenessCoarse : Truth.hashDiscretenessFine);
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
