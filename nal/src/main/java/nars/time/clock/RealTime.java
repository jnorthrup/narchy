package nars.time.clock;

import jcog.Texts;
import nars.NAR;
import nars.time.Time;
import org.jetbrains.annotations.NotNull;
import tec.uom.se.quantity.time.TimeQuantities;

import javax.measure.Quantity;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;

/**
 * Created by me on 7/2/15.
 */
public abstract class RealTime extends Time {


    private final int unitsPerSecond;

    protected long startNS;
    protected long startMS;
    public final boolean relativeToStart;
    protected long start;

    final static AtomicLongFieldUpdater<RealTime> T = AtomicLongFieldUpdater.newUpdater(RealTime.class, "t");
    private volatile long t;


    final long seed = Math.abs(UUID.randomUUID().getLeastSignificantBits() ) & 0xffff0000; 

    final AtomicLong nextStamp = new AtomicLong(seed);

    private int dur = 1, nextDur = 1;
    private long last;



    protected RealTime(int unitsPerSecond, boolean relativeToStart) {
        super();

        this.relativeToStart = relativeToStart;
        this.unitsPerSecond = unitsPerSecond;

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
        this.startMS = System.currentTimeMillis();
        this.startNS = System.nanoTime();
        this.start = relativeToStart ? Math.round((startMS/1000.0) * unitsPerSecond) : 0L;
        this.dur = nextDur;
        T.set(this, this.last = realtime());
    }

    @Override
    public final void cycle(NAR n) {
        this.dur = nextDur;
        this.last = T.getAndSet(this, realtime());
    }

    @Override
    public final long now() {
        //return t.getOpaque();
        return T.get(this);
    }


    protected abstract long realtime();

//    double secondsSinceStart() {
//        return unitsToSeconds(now() - start);
//    }

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
        return String.valueOf(now()); //TODO more descriptive
    }

    @Override
    public Time dur(int cycles) {
        assert(cycles > 0);
        this.nextDur = cycles;
        return this;
    }

    public Time durSeconds(double seconds) {
        return dur(Math.max(1, (int) Math.round(secondsToUnits(seconds))));
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
        return (float) unitsToSeconds(dur);
    }

    public double secondsPerUnit() {
        return (float) unitsToSeconds(1);
    }

//    /** get real-time frames per duration */
//    public float durRatio(Loop l) {
//        float fps = l.getFPS();
//        if (fps > Float.MIN_NORMAL)
//            return durSeconds() * fps;
//        else
//            return 1;
//    }
//    /** set real-time frames per duration */
//    public void durRatio(Loop l, float ratio) {
//        durSeconds(ratio / l.getFPS());
//    }

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
            return start + (System.nanoTime() - startNS) / (100 * 1_000_000);
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
            return start + (System.nanoTime() - startNS) / (10 * 1_000_000);
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
            return start + (System.nanoTime() - startNS) / (1 * 1_000_000);
        }

    }

    /** nanosecond accuracy */
    public static class NS extends RealTime {


        protected NS(boolean relativeToStart) {
            super(1000*1000*1000, relativeToStart);
        }

        @Override
        protected long realtime() {
            return start + (System.nanoTime() - startNS);
        }

    }
}

















































