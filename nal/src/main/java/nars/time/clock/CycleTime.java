package nars.time.clock;

import nars.time.Time;

import java.util.concurrent.atomic.AtomicLong;

/** increments time on each frame */
public class CycleTime extends Time {

    private final AtomicLong nextStamp = new AtomicLong(0);

    long t;
    final int dt;

    float dur;


    CycleTime(int dt, int dur) {
        this.dt = dt;
        this.dur = dur;
        reset();
    }

    public CycleTime() {
        this(1, 1);
    }

    @Override
    public float dur() {
        return dur;
    }

    @Override
    public CycleTime dur(float d) {
        this.dur = d;
        return this;
    }

    @Override
    public void reset() {
        t = 0;
    }

    @Override
    public final long now() {
        return t;
    }

    @Override
    public long sinceLast() {
        return dt;
    }

    @Override
    public final void next() {
        t += dt;
    }

    @Override
    public String timeString(long time) {
        return time + " cyc";
    }

    @Override
    public String toString() {
        return Long.toString(t);
    }

    /**
     * produces a new stamp serial #, used to uniquely identify inputs
     */
    @Override public final long nextStamp() {
        return nextStamp.incrementAndGet();
    }

}
