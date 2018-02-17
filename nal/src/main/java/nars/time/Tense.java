package nars.time;

import jcog.Util;
import jcog.math.LongInterval;
import nars.NAR;


/**
 * tense modes, and various Temporal utility functions and constants
 */
public enum Tense {

    Eternal(":-:"),
    Past(":\\:"),
    Present(":|:"),
    Future(":/:");

    /**
     * default for atemporal events
     * means "always" in Judgment/Question, but "current" in Goal/Quest
     */
    public static final long ETERNAL = LongInterval.ETERNAL;
    public static final long TIMELESS = LongInterval.TIMELESS;

    /**
     * integer version of long ETERNAL
     */
    public static final int DTERNAL = Integer.MIN_VALUE;

    /**
     * a dt placeholder value for preventing premature flattening during derivation term construction
     */
    public static final int XTERNAL = Integer.MAX_VALUE;
    public static final long[] ETERNAL_FOCUS = new long[]{ETERNAL, ETERNAL};


    public final String symbol;

    Tense(String string) {
        symbol = string;
    }

    static int order(float timeDiff, int durationCycles) {
        float halfDuration = durationCycles / 2.0f;
        if (timeDiff >= halfDuration) {
            return +1;
        } else if (timeDiff <= -halfDuration) {
            return -1;
        } else {
            return 0;
        }
    }

    /**
     * if (relative) event B after (stationary) event A then order=forward;
     * event B before       then order=backward
     * occur at the same time, relative to duration: order = concurrent
     */
    public static int order(long a, long b, int durationCycles) {
        if ((a == ETERNAL) || (b == ETERNAL))
            throw new RuntimeException("order() does not compare ETERNAL times");

        return order(b - a, durationCycles);
    }

    public static long getRelativeOccurrence(Tense tense, NAR m) {
        return getRelativeOccurrence(m.time(), tense, 1 /*m.duration()*/);
    }


    public static long getRelativeOccurrence(long creationTime, Tense tense, int duration) {
        switch (tense) {
            case Present:
                return creationTime;
            case Past:
                return creationTime - duration;
            case Future:
                return creationTime + duration;
            default:
                return ETERNAL;
        }
    }

    public static long dither(long dt, int dither) {
        if (dither > 1) {
            if (dt == ETERNAL) return ETERNAL;
            else if (dt == TIMELESS) return TIMELESS;
            else return Math.abs(dt) < dither ? 0 : Util.round(dt, dither); //collapse to simultaneous if less than the dither
        } else {
            return dt;
        }
    }

    public static int dither(int dt, NAR nar) {
        return dither(dt, nar.dtDitherCycles());
    }

    public static long dither(long t, NAR nar) {
        return dither(t, nar.dtDitherCycles());
    }

    public static int dither(int dt, int dither) {
        switch (dt) {
            case DTERNAL:
                return DTERNAL;
            case XTERNAL:
                return XTERNAL;
            case 0:
                return 0;
            default:
                if (dither > 1)
                    //collapse to simultaneous if less than the dither
                    return Math.abs(dt) < dither ? 0 : Util.round(dt, dither);
                else
                    return dt; //unaffected
        }
    }


    @Override
    public String toString() {
        return symbol;
    }


}
