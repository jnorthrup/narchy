package nars.time;

import jcog.Texts;
import nars.NAR;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by me on 7/2/15.
 */
public abstract class RealTime extends Time {


    private final int unitsPerSecod;
    long t;
    private long start;

    final long seed = Math.abs(UUID.randomUUID().getLeastSignificantBits() ) & 0xffff0000; //???

    final AtomicLong nextStamp = new AtomicLong(seed);

    private int dur = 1;
    private long t0;

    protected RealTime(int unitsPerSecond, boolean relativeToStart) {
        super();
        this.unitsPerSecod = unitsPerSecond;
        this.t = this.t0 = this.start = relativeToStart ? realtime() : 0L;
        reset();
    }


    public final double secondsToUnits(double s) {
        return s / unitsToSeconds(1);
    }

    @Override
    public long nextStamp() {
        return nextStamp.getAndIncrement();
    }

    @Override
    public void reset() {
        long rt = realtime();

        if (start!=0)
            start = rt;

        t = (rt - start);
    }

    @Override
    public final void cycle(NAR n) {
        super.cycle(n);
        t0 = t;
        t = (realtime()-start);
    }

    @Override
    public final long now() {
        return t;
    }


    protected abstract long realtime();

    double secondsSinceStart() {
        return unitsToSeconds(t - start);
    }

    protected final double unitsToSeconds(long l) {
        return l / ((double)unitsPerSecod);
    }

    @Override
    public long sinceLast() {
        return t - t0;
    }

    @NotNull
    @Override
    public String toString() {
        return secondsSinceStart() + "s";
    }

    @Override
    public Time dur(int cycles) {
        assert(cycles > 0);
        this.dur = cycles;
        return this;
    }

    public Time durSeconds(double seconds) {
        return dur((int) Math.ceil(secondsToUnits(seconds)));
    }


    @Override
    public int dur() {
        return dur;
    }

    public Time durFPS(double fps) {
        durSeconds(1.0/fps);
        return this;
    }

    @Override
    protected String durationString(long time) {
        return Texts.timeStr(unitsToSeconds(time) * 1.0E9);
    }

    /** decisecond (0.1) accuracy */
    public static class DS extends RealTime {


        public DS() {
            this(false);
        }

        public DS(boolean relativeToStart) {
            super(10, relativeToStart);
        }

        @Override
        protected long realtime() {
            return System.currentTimeMillis() / 100;
        }

    }

    /** half-decisecond (50ms ~ 20hz) accuracy */
    public static class DSHalf extends RealTime {


        public DSHalf() {
            this(false);
        }

        public DSHalf(boolean relativeToStart) {
            super(50, relativeToStart);
        }

        @Override
        protected long realtime() {
            return System.currentTimeMillis() / 20;
        }

    }

    /** centisecond (0.01) accuracy */
    public static class CS extends RealTime {


        public CS() {
            this(false);
        }

        public CS(boolean relativeToStart) {
            super(100, relativeToStart);
        }

        @Override
        protected long realtime() {
            return System.currentTimeMillis() / 10;
        }

    }

    /** millisecond accuracy */
    public static class MS extends RealTime {


        public MS() {
            this(false);
        }


        public MS(boolean relativeToStart) {
            super(1000, relativeToStart);
        }

        @Override
        protected long realtime() {
            return System.currentTimeMillis();
        }

    }

    /** nanosecond accuracy */
    public static class NS extends RealTime {


        protected NS(boolean relativeToStart) {
            super(1000*1000*1000, relativeToStart);
        }

        @Override
        protected long realtime() {
            return System.nanoTime();
        }

    }
}


//package nars.clock;
//
///**
// * hard realtime does not cache the value and will always update when time()
// * is called
// */
//public class HardRealtimeClock extends RealtimeClock {
//
//    private final boolean msOrNano;
//
//    public HardRealtimeClock(boolean msOrNano) {
//        super(false);
//        this.msOrNano = msOrNano;
//    }
//
//    /** default: ms resolution */
//    public HardRealtimeClock() {
//        this(true);
//    }
//
//
//    @Override
//    protected long getRealTime() {
//        if (msOrNano) {
//            return System.currentTimeMillis();
//        }
//        else {
//            return System.nanoTime();
//        }
//    }
//
//    @Override
//    protected float unitsToSeconds(long l) {
//        if (msOrNano) {
//            return (l / 1000f);
//        }
//        else {
//            return (l / 1e9f);
//        }
//    }
//
//    @Override
//    public long time() {
//        return getRealTime();
//    }
//
//}
