package nars.task.util;

import jcog.WTF;
import jcog.math.LongInterval;
import jcog.math.Longerval;
import jcog.tree.rtree.HyperRegion;
import nars.time.Tense;

/**
 * only valid for comparison during rtree iteration
 */
public class TimeRange implements HyperRegion, LongInterval {

    public static final TimeRange ETERNAL = new TimeRange(Tense.ETERNAL, Tense.ETERNAL);

    final public long start, end;

    public TimeRange(long[] t) {
        this(t[0], t[1]);
        assert(t.length==2);
    }

    public TimeRange(long w) {
        this(w, w);
    }

    public TimeRange(long s, long e) {
        if (e == Tense.TIMELESS)
            throw new WTF();
        if (s==TIMELESS)
            throw new WTF();
        if (s > e)
            throw new WTF();

        this.start = s;
        this.end = e;
    }

    @Override
    public boolean intersects(HyperRegion x) {
        long s = this.start;
        if (s == Tense.ETERNAL)
            return true;

        LongInterval t = (LongInterval)x;
        return Longerval.intersects(s, end, t.start(), t.end());
    }


    @Override
    public boolean contains(HyperRegion x) {
        long s = this.start;
        if (s == Tense.ETERNAL)
            return true;
        return ((LongInterval)x).containedBy(s, end);
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

    @Override
    public long start() {
        return start;
    }

    @Override
    public long end() {
        return end;
    }

    public final long minTimeTo(LongInterval b) {
        return minTimeTo(b.start(), b.end());
    }
    public final long meanTimeTo(LongInterval b) {
        return meanTimeTo(b.start(), b.end());
    }

}
