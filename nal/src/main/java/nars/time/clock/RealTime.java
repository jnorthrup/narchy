package nars.time.clock;

import jcog.Texts;
import jcog.exe.Loop;
import nars.NAR;
import nars.time.Tense;
import nars.time.Time;
import org.jetbrains.annotations.NotNull;
import tec.uom.se.quantity.time.TimeQuantities;

import javax.measure.Quantity;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by me on 7/2/15.
 */
public abstract class RealTime extends Time {


    private final int unitsPerSecond;

    public final long t0;

    final AtomicLong t = new AtomicLong();
    private long start;

    final long seed = Math.abs(UUID.randomUUID().getLeastSignificantBits() ) & 0xffff0000; 

    final AtomicLong nextStamp = new AtomicLong(seed);

    private int dur = 1;
    private long last;



    protected RealTime(int unitsPerSecond, boolean relativeToStart) {
        super();
        this.unitsPerSecond = unitsPerSecond;

        long now = realtime();
        this.t0 = relativeToStart ? 0 : now;
        this.t.set(this.last = this.start = relativeToStart ? now : 0L);

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

        t.set(rt - start);
    }

    @Override
    public final void cycle(NAR n) {
        //last = t.getAndSet(realtime() - start);
        last = t.getAndSet(Tense.dither(realtime(),n) - Tense.dither(start, n));
    }

    @Override
    public final long now() {
        return t.getOpaque();
    }


    protected abstract long realtime();

    double secondsSinceStart() {
        return unitsToSeconds(now() - start);
    }

    protected final double unitsToSeconds(long l) {
        return l / ((double) unitsPerSecond);
    }

    @Override
    public long sinceLast() {
        return now() - last;
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
        return dur(Math.max(1, (int) Math.ceil(secondsToUnits(seconds))));
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
    public String timeString(long time) {
        return Texts.timeStr(unitsToSeconds(time) * 1.0E9);
    }

    /** ratio of duration to fps */
    public float durSeconds() {
        return (float) unitsToSeconds(dur());
    }

    /** get real-time frames per duration */
    public float durRatio(Loop l) {
        float fps = l.getFPS();
        if (fps > Float.MIN_NORMAL)
            return durSeconds() * fps;
        else
            return 1; 
    }
    /** set real-time frames per duration */
    public void durRatio(Loop l, float ratio) {
        durSeconds(ratio / l.getFPS());
    }

    @Override
    public long toCycles(Quantity q) {
        double s = TimeQuantities.toTimeUnitSeconds(q).doubleValue(null);
        return Math.round(s * unitsPerSecond);
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

















































