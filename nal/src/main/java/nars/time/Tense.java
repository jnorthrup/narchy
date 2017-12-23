package nars.time;

import nars.NAR;


/** tense modes, and various Temporal utility functions and constants */
public enum Tense  {

    Eternal(":-:"),
    Past(":\\:"),
    Present(":|:"),
    Future(":/:");

    /**
     * default for atemporal events
     * means "always" in Judgment/Question, but "current" in Goal/Quest
     */
    public static final long ETERNAL = Long.MIN_VALUE;
    public static final long TIMELESS = Long.MAX_VALUE;

    /** integer version of long ETERNAL */
    public static final int DTERNAL = Integer.MIN_VALUE;

    /** a dt placeholder value for preventing premature flattening during derivation term construction */
    public static final int XTERNAL = Integer.MAX_VALUE;



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


    @Override
    public String toString() {
        return symbol;
    }
    


}
