package nars.time;

import nars.task.util.TimeRange;

import java.util.function.LongSupplier;

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

    public final float dur;
    public final X x;


    public When(long start, long end, float dur, X x) {
        super(start, end);
        assert(dur>=0);



        this.dur = dur;
        this.x = x;
    }

    public When<X> update(LongSupplier clock) {
        long next = clock.getAsLong();
        if (start!=next)
            return new When<>(next, next+(end-start), dur, x);
        else
            return this;
    }

}
