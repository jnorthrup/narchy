package nars.time;

import nars.NAR;
import nars.task.util.TimeRange;

/** represents a perceptual window in time, including a perceptual duration (dur) and
 * reference to invoking context NAR.
 * TODO Use this in all applicable long,long,int,dur parameter sequences
 * */
public class When extends TimeRange {

    public final int dur;
    public final NAR nar;

    public When(long start, long end, int dur, NAR nar) {
        super(start, end);
        this.dur = dur;
        this.nar = nar;
    }

    public When(long start, long end, NAR nar) {
        this(start, end, nar.dur(), nar);
    }

    public When(long when, NAR nar) {
        this(when, when, nar);
    }


    public static When eternal(NAR n) {
        return new When(Tense.ETERNAL, Tense.ETERNAL, n);
    }

    /** generates a default 'now' moment: current NAR clock time with dur/2 radius.
     *  the equal-length past and future periods comprising the extent of the present moment. */
    public static When now(NAR nar, int dur) {
        long now = nar.time();
        return new When(now - dur/2, now + dur/2, dur, nar);
    }

    public static When now(NAR nar) {
        return now(nar, nar.dur());
    }
    public static When sinceAgo(long ago, NAR nar) {
        long now = nar.time();
        return new When(now - Math.max(0, ago), now, nar);
    }
    public static When since(long when, NAR nar) {
        long now = nar.time();
        return new When(Math.min(when, now), now, nar);
    }
    public static When until(long when, NAR nar) {
        long now = nar.time();
        return new When(now, Math.max(when, now), nar);
    }

    /** creates new evidence */
    public final long[] newStamp() {
        return new long[]{nar.time.nextStamp()};
    }

//    public long getStart() {
//        return start;
//    }
//
//    public long getEnd() {
//        return end;
//    }
//
//    public int getDur() {
//        return dur;
//    }
//
//    public NAR getN() {
//        return n;
//    }
}
