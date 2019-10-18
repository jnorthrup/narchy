package nars.time;

/**
 * reference to a temporal context:
 *      --start, end
 *          occurrence
 *      --dur
*           time constant (may be less equal or greater than the occurrence range)
 *
 *
 * represents a mutable perceptual window in time, including a perceptual duration (dur) and
 * reference to invoking context X.
 * TODO Use this in all applicable long,long,int,dur parameter sequences
 * */
public class When<X> /*extends TimeRange */{

    public X x;
    public long start;
    public long end;
    public float dur;

    public When() {

    }

    public When(long start, long end, float dur, X x) {
        the(x);
        range(start, end);
        dur(dur);
    }

    public final When the(X x) {
        this.x = x;
        return this;
    }

    /** dur >= 0 */
    public final When dur(float dur) {
        this.dur = dur;
        return this;
    }

    public final When range(long s, long e) {
        start = s; end = e;
        return this;
    }

}
