package nars.task.util;

import jcog.math.LongInterval;
import nars.time.Tense;
import org.eclipse.collections.api.block.predicate.primitive.LongLongPredicate;
import org.jetbrains.annotations.Nullable;

public final class TimeRangeFilter extends TimeRange implements LongLongPredicate {


    private static final TimeRangeFilter Eternal = new TimeRangeFilter(Tense.ETERNAL, Tense.ETERNAL, Mode.Near);

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
                return LongInterval.intersects(s, e, start, end);
            }
        },
        Contains {
            @Override
            public boolean accept(long s, long e, long start, long end) {
                return (s >= start && e <= end);
            }
        };

        public abstract boolean accept(long s, long e, long start, long end);
    }

    /** if null, allows as if Mode.Near */
    private final @Nullable Mode mode;

    public static TimeRangeFilter the(long start, long end, Mode mode) {
        assert(start!=TIMELESS);
        if (mode == Mode.Contains && (start == end))
            throw new RuntimeException("nothing contained in zero length time interval");

		return start == Tense.ETERNAL ? TimeRangeFilter.Eternal : new TimeRangeFilter(start, end, mode);
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
