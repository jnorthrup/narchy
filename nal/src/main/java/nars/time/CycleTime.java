package nars.time;

import nars.NAR;
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
        super.cycle(n);
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

    /** used to ensure that the next system stamp serial is beyond the range of any input */
    protected final void validate(long s) {
        if (s == Long.MAX_VALUE) //ignore cyclic indicator
            return;
        long nextStamp = this.nextStamp.longValue();
        if (s == Long.MAX_VALUE) //ignore cyclic indicator
            s = 0; //wraparound skipping MAX_VALUE which is reserved for cyclic, but ignore the negative spectrum

        if (nextStamp < s)
            this.nextStamp.set(s+1);
    }

    public final void validate(@NotNull long[] s) {
        //assume that the evidence is sorted, and that the max value is in the last position in addition to 1 or more preceding values
        if (s.length > 1)
            validate(s[s.length-1]);
    }


}
