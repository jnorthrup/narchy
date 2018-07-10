package nars.task.util;

import jcog.math.LongInterval;
import jcog.math.Longerval;
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
        return Longerval.intersects(start, end, t.start(), t.end());
    }


    @Override
    public boolean contains(HyperRegion x) {
        return ((LongInterval)x).containedBy(start, end);
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
