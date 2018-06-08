package nars.task.util;

import jcog.math.LongInterval;
import jcog.tree.rtree.HyperRegion;

/**
 * only valid for comparison during rtree iteration
 */
public class TimeRange implements HyperRegion {

    long start = Long.MIN_VALUE, end = Long.MAX_VALUE;

    public TimeRange() {

    }

    public TimeRange(long s, long e) {
        set(s, e);
    }

    public TimeRange set(long s, long e) {
        this.start = s;
        this.end = e;
        return this;
    }
    @Override
    public boolean intersects(HyperRegion x) {
        LongInterval t = (LongInterval)x;
        return t.intersects(start, end);
    }


    @Override
    public boolean contains(HyperRegion x) {
        LongInterval t = (LongInterval)x;
        return t.contains(start, end);
    }

    @Override
    public final HyperRegion mbr(HyperRegion r) {
        throw new UnsupportedOperationException();
    }

    @Override
    public final int dim() {
        return 3;
    }



    @Override
    public final double coord(boolean maxOrMin, int dimension) {
        throw new UnsupportedOperationException();
    }

}
