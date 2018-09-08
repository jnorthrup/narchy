package nars.time;

import jcog.Util;
import jcog.WTF;
import jcog.math.LongInterval;
import nars.NAR;
import nars.task.util.TaskRegion;
import nars.util.TimeAware;


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


    private final String symbol;

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

    public static long getRelativeOccurrence(Tense tense, TimeAware m) {
        return getRelativeOccurrence(m.time(), tense, 1 /*m.duration()*/);
    }


    private static long getRelativeOccurrence(long creationTime, Tense tense, int duration) {
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

    public static long dither(double t, int dither) {
        if (dither > 1) {
            if (t == ETERNAL) return ETERNAL;
            else if (t == TIMELESS) return TIMELESS;
            else return Math.round(Util.round(t, dither)); 
        } else {
            return Math.round(t);
        }
    }

    public static long dither(long t, int dither) {
        if (dither > 1) {
            //if (Param.DEBUG) {
                //HACK
                if (t == DTERNAL || t==XTERNAL)
                    throw new WTF("maybe you meant ETERNAL or TIMELESS");
            //}
            if (t == ETERNAL) return ETERNAL;
            else if (t == TIMELESS) return TIMELESS;
            else return Util.round(t, dither); 
        } else {
            return t;
        }
    }

    public static int dither(int dt, NAR nar) {
        return dither(dt, nar.dtDither());
    }

    public static long dither(long t, NAR nar) {
        return dither(t, nar.dtDither());
    }

    /** modifies the input array */
    public static long[] dither(long[] t, NAR nar) {
        long s = t[0];
        if (s == ETERNAL) {
            assert(t[1] == ETERNAL);
            return t;
        }

        int d = nar.dtDither();
        t[0] = dither(s, d);
        t[1] = dither(t[1], d);
        return t;
    }

    public static int dither(int dt, int dither) {
        if (dither > 1)
            switch (dt) {
                case DTERNAL:
                    return DTERNAL;
                case XTERNAL:
                    return XTERNAL;
                case 0:
                    return 0;
                default:
                    
                    return Util.round(dt, dither);
        }

        return dt; 
    }

    public static long[] union(TaskRegion... tt) {
        long start = Long.MAX_VALUE;
        long end = Long.MIN_VALUE;
        
        for (TaskRegion x : tt) {
            if (x == null)
                continue;
            long xs = x.start();
            if (xs == ETERNAL)
                continue;
            start = Math.min(xs, start);

            long xe = x.end();
            end = Math.max(xe, end);
        }
        if (start == Long.MAX_VALUE) {
            
            start = end = ETERNAL;
        }
        return new long[] { start, end };
    }

    public static boolean dtSpecial(int dt) {
        switch (dt) {
            case 0:
            case DTERNAL:
            case XTERNAL:
                return true;

            default:
                return false;
        }
    }

    /** safely transform occ (64-bit) to dt (32-bit) */
    public static int occToDT(long occ) {
        if (occ == ETERNAL)
            return DTERNAL; //HACK
        if (occ == TIMELESS)
            return XTERNAL; //HACK
        if (occ > Integer.MAX_VALUE-1 || occ < Integer.MIN_VALUE+1)
            throw new WTF(occ + " can not be DT");
        return (int)occ;
    }

    public static long[] intersection(TaskRegion[] t) {
        //HACK
        long[] u = Tense.union(t);
        if (u[0] == ETERNAL)
            return u;


        //find the min range
        long minRange = Long.MAX_VALUE;
        for(TaskRegion x : t) {
            if (x == null || x.task().isEternal())
                continue;
            long r = x.range();
            if (r < minRange)
                minRange = r;
        }
        long range = minRange-1;
        if (u[1]-u[0] > range) {
            //shrink range around estimated center point
            long mid = (u[1] + u[0])/2L;
            return new long[] { mid - range/2L, mid + range/2L };
        }
        return u;
    }


    @Override
    public String toString() {
        return symbol;
    }


}
