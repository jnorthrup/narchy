package nars.time.clock;

import nars.NAR;
import nars.time.Time;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicLong;

/** increments time on each frame */
public class CycleTime extends Time {

    private final AtomicLong nextStamp = new AtomicLong(0);

    long t;
    final int dt;

    int dur;


    CycleTime(int dt, int dur) {
        this.dt = dt;
        this.dur = dur;
        reset();
    }

    public CycleTime() {
        this(1, 1);
    }

    @Override
    public int dur() {
        return dur;
    }

    @Override
    public CycleTime dur(int d) {
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
    public final void cycle(NAR n) {
        t += dt;
    }

    @Override
    protected String durationString(long time) {
        return time + " cyc";
    }

    @NotNull
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
