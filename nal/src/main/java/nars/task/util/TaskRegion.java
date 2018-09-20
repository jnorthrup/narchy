package nars.task.util;

import jcog.math.LongInterval;
import jcog.tree.rtree.HyperRegion;
import nars.Task;
import nars.task.Tasked;
import nars.truth.TruthFunctions;
import org.apache.commons.lang3.ArrayUtils;

import java.util.function.Consumer;
import java.util.function.Predicate;

/** 3d cuboid region:
 *      time            start..end              64-bit signed long
 *      frequency:      min..max in [0..1]      32-bit float
 *      confidence:     min..max in [0..1)      32-bit float
 */
public interface TaskRegion extends HyperRegion, Tasked, LongInterval {

    /**
     * cost of splitting a node by time
     */
    float TIME_COST = 1f;

    /**
     * cost of splitting a node by freq
     */
    float FREQ_COST = 1f;

    /**
     * cost of splitting a node by conf
     */
    float CONF_COST = 4f;

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



    }

    default float freqCost() {
        return ((float) range(1)) * FREQ_COST;
    }

    default float confCost() {
        return ((float) range(2)) * CONF_COST;
    }

    @Override
    default double range(final int dim) {
        switch (dim) {
            case 0:
                return 1 + end()-start();
            default:
                return /*Math.abs*/(coordF(true, dim) - coordF(false, dim));
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
                    long ts = start();
                    long te = end();
                    
                    







                    
                    return new TasksRegion(Math.min(ts, es), Math.max(te, ee),
                            f0, f1, c0, c1
                    );
                } else {
                    return new TasksRegion(
                            Math.min(start(), es), Math.max(end(), ee),
                            Math.min(coordF(false, 1), ef),
                            Math.max(coordF(true, 1), ef),
                            Math.min(coordF(false, 2), ec),
                            Math.max(coordF(true, 2), ec)
                    );
                }
            } else {
                TaskRegion er = (TaskRegion) r;
                return new TasksRegion(
                        Math.min(start(), er.start()), Math.max(end(), er.end()),
                        Math.min(coordF(false, 1), er.coordF(false, 1)),
                        Math.max(coordF(true, 1), er.coordF(true, 1)),
                        Math.min(coordF(false, 2), er.coordF(false, 2)),
                        Math.max(coordF(true, 2), er.coordF(true, 2))
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
    double coord(boolean maxOrMin, int dimension);

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


    /** intersects only the time dimension */
    default boolean intersectsTime(LongInterval x) {
        return this == x || intersects(x.start(), x.end());
    }

}
