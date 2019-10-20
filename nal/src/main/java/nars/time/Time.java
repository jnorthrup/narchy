package nars.time;

import javax.measure.Quantity;
import java.io.Serializable;

import static java.lang.Math.round;
import static nars.time.Tense.ETERNAL;

/**
 * 1-D Time Model (and Clock)
 */
public abstract class Time implements Serializable {


    public abstract long now();

    /**
     * time elapsed since last cycle
     */
    public abstract long sinceLast();

    /**
     * returns a new stamp evidence id
     */
    public abstract long nextStamp();

    /**
     * the default duration applied to input tasks that do not specify one
     * >0
     */
    public abstract float dur();


    /** update to the next time */
    public abstract void next();

    /** clock reset */
    public abstract void reset();

    /**
     * set the duration, return this
     *
     * @param d, d>0
     */
    public abstract Time dur(float d);



//    /**
//     * returns a string containing the time elapsed/to the given time
//     */
//    public String durationToString(long target) {
//        long now = now();
//        return durationString(now - target);
//    }

    /**
     * produces a string representative of the amount of time (cycles, not durs)
     */
    public abstract String timeString(long time);

    public long toCycles(Quantity q) {
        throw new UnsupportedOperationException("Only in RealTime implementations");
    }


    @Deprecated public long relativeOccurrence(Tense tense) {
        /*m.duration()*/
        switch (tense) {
            case Present:
                return now();
            case Past:
                return (long) round((float) now() - dur());
            case Future:
                return (long) round((float) now() + dur());
            default:
                return ETERNAL;
        }
    }
}
