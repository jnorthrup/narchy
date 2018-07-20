package nars.experiment.minicraft.side;


import jcog.Util;

/**
 * A wrapper class that provides timing methods. This class
 * provides us with a central location where we can add
 * our current timing implementation. Initially, we're going to
 * rely on the GAGE timer. (@see http:
 *
 * @author Kevin Glass
 */
public class SystemTimer {
    /** Our link into the GAGE timer library */

    /**
     * The number of "timer ticks" per second
     */
    private static long timerTicksPerSecond;

    /** A little initialization at startup, we're just going to get the GAGE timer going */
    static {


    }

    /**
     * Get the high resolution time in milliseconds
     *
     * @return The high resolution time in milliseconds
     */
    public static long getTime() {


        return System.currentTimeMillis();

    }

    /**
     * Sleep for a fixed number of milliseconds.
     *
     * @param duration The amount of time in milliseconds to sleep for
     */
    public static void sleep(long duration) {
        if (duration <= 0) return;
        Util.sleep(duration);

    }
}