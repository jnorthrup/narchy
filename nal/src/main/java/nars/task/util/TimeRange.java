package nars.task.util;

import jcog.math.LongInterval;
import jcog.math.Longerval;
import jcog.tree.rtree.HyperRegion;
import nars.time.Tense;

/**
 * only valid for comparison during rtree iteration
 */
public class TimeRange extends Longerval implements HyperRegion {

    public static final TimeRange ETERNAL = new TimeRange(Tense.ETERNAL, Tense.ETERNAL);

    public TimeRange(long[] t) {
        super(t[0], t[1]);
        assert(t.length==2);
    }

    public TimeRange(long w) {
        this(w, w);
    }

    public TimeRange(long s, long e) {
        super(s, e);
    }


    @Override
    public boolean intersects(HyperRegion x) {
        var s = this.start;
        if (s == Tense.ETERNAL)
            return true;

        var t = (LongInterval)x;
        return LongInterval.intersectsSafe(s, end, t.start(), t.end());
    }


    @Override
    public boolean contains(HyperRegion x) {
        //return ((LongInterval)x).containedBy(start, end);
        var t = ((LongInterval) x);
        return containsSafe(t.start(), t.end());
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
    public final double coord(int dimension, boolean maxOrMin) {
        throw new UnsupportedOperationException();
    }


}
