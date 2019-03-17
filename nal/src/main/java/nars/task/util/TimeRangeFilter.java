package nars.task.util;

import jcog.math.Longerval;
import nars.time.Tense;
import org.eclipse.collections.api.block.predicate.primitive.LongLongPredicate;
import org.jetbrains.annotations.Nullable;

public final class TimeRangeFilter extends TimeRange implements LongLongPredicate {


    final static private TimeRangeFilter Eternal = new TimeRangeFilter(Tense.ETERNAL, Tense.ETERNAL, Mode.Near);

    public enum Mode  {
        Near {
            @Override
            public boolean accept(long s, long e, long start, long end) {
                return true;
            }
        },
        Intersects {
            @Override
            public boolean accept(long s, long e, long start, long end) {
                return Longerval.intersects(s, e, start, end);
            }
        },
        Contains {
            @Override
            public boolean accept(long s, long e, long start, long end) {
                return (s >= start && e <= end);
            }
        };

        abstract public boolean accept(long s, long e, long start, long end);
    }

    /** if null, allows as if Mode.Near */
    @Nullable
    private final Mode mode;

    public static TimeRangeFilter the(long start, long end, Mode mode) {
        assert(start!=TIMELESS);
        if (mode == Mode.Contains && (start == end))
            throw new RuntimeException("nothing contained in zero length time interval");

        if (start == Tense.ETERNAL) {
            return TimeRangeFilter.Eternal;
        } else {
            return new TimeRangeFilter(start, end, mode);
        }
    }

    private TimeRangeFilter(long start, long end, Mode mode) {
        super(start, end);
        this.mode = mode==Mode.Near ? null : mode;
    }

    @Override
    public final boolean accept(long s, long e) {
        return mode == null || mode.accept(s, e, start, end);
    }

    public final boolean accept(TaskRegion t) {
        return mode == null || mode.accept(t.start(), t.end(), start, end);
    }
}
