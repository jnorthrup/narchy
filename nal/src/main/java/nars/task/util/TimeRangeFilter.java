package nars.task.util;

import jcog.math.Longerval;
import org.eclipse.collections.api.block.predicate.primitive.LongLongPredicate;

public class TimeRangeFilter extends TimeRange implements LongLongPredicate {

    public final boolean intersectOrContain;

    public TimeRangeFilter(long min, long max, boolean intersectOrContain) {
        super(min, max);

        this.intersectOrContain = intersectOrContain;

        if (!intersectOrContain && (min == max))
            throw new RuntimeException("nothing contained in zero length time interval");

    }

    @Override
    public boolean accept(long s, long e) {
        return intersectOrContain ?
                Longerval.intersectLength(s, e, start, end) != -1
                :
                (s >= start && e <= end);
    }
}
