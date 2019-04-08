package nars.time;

import jcog.WTF;
import nars.task.util.TimeRange;

/**
 * reference to a temporal context:
 *      --start, end
 *          occurrence
 *      --dur
*           time constant (may be less equal or greater than the occurrence range)
 *
 *
 * represents a perceptual window in time, including a perceptual duration (dur) and
 * reference to invoking context X.
 * TODO Use this in all applicable long,long,int,dur parameter sequences
 * */
public class When<X> extends TimeRange {

    public final int dur;
    public final X x;

    public When(long start, long end, int dur, X x) {
        super(start, end);

        if (dur <= 0)
            throw new WTF();

        this.dur = dur;
        this.x = x;
    }


}
