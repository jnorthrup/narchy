package nars.task.util;

import jcog.math.Longerval;
import nars.time.Tense;
import org.eclipse.collections.api.block.predicate.primitive.LongLongPredicate;

abstract public class TimeRangeFilter extends TimeRange implements LongLongPredicate {


    final static private TimeRangeFilter Eternal = new TimeRangeFilter(Tense.ETERNAL, Tense.ETERNAL) {
        @Override public boolean accept(long value1, long value2) {
            return true;
        }
    } ;

    public static TimeRangeFilter the(long start, long end, boolean intersectOrContain) {
        if (!intersectOrContain && (start == end))
            throw new RuntimeException("nothing contained in zero length time interval");

        if (start == Tense.ETERNAL) {
            return TimeRangeFilter.Eternal;
        } else {
            if (intersectOrContain) {
                return new TimeRangeFilter(start, end) {
                    @Override public boolean accept(long s, long e) {
                        return Longerval.intersectLength(s, e, start, end) != -1;
                    }
                };
            } else {
                return new TimeRangeFilter(start, end) {
                    @Override public boolean accept(long s, long e) {
                        return (s >= start && e <= end);
                    }
                };
            }
        }
    }




    public TimeRangeFilter(long start, long end) {
        super(start, end);
    }


}
