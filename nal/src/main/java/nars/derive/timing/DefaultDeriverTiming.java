package nars.derive.timing;

import nars.NAR;
import nars.Task;
import nars.term.Term;
import nars.time.Tense;
import nars.time.Time;

import java.util.Random;
import java.util.function.BiFunction;

import static nars.time.Tense.ETERNAL;

/** naively applies a variety of methods for calculating time focus targets */
public class DefaultDeriverTiming implements BiFunction<Task, Term, long[]> {

    private final Time clock;
    private final Random rng;
    private final NAR nar;

    public DefaultDeriverTiming(NAR n) {
        this.nar = n;
        this.clock = n.time;
        this.rng = n.random();
    }

    @Override
    public long[] apply(Task task, Term term) {
        long[] tt;
        switch (rng.nextInt(4)) {
            case 0:
                tt = presentDuration(); break;
            case 1:
                tt = taskTime(task); break;
            case 2:
                tt = pastFutureRadius(task, false); break;
            case 3:
                tt = pastFutureRadius(task, true); break;
            default:
                throw new UnsupportedOperationException();
        }
        if (tt[0]!=ETERNAL) {
            int d = nar.dtDither();
            tt[0] = Tense.dither(tt[0], d);
            tt[1] = Tense.dither(tt[1], d);
        }

        return tt;
    }



    private long[] presentDuration() {
        long now = clock.now();
        int dur = Math.max(2, clock.dur());
        return new long[] { now - dur/2, now + dur/2 };
    }

    private long[] taskTime(Task t) {
        return new long[] {t.start(), t.end() };
    }

    private long[] pastFutureRadius(Task t, boolean future) {
        long[] tt = taskTime(t);
        if (tt[0]==ETERNAL) {
            return rng.nextBoolean() ? tt : presentDuration();
        } else {

            long now = nar.time();
            long range = Math.max(Math.abs(tt[0] - now), Math.abs(tt[1] - now));
            if (future) {
                return new long[]{now, now + range}; //future span
            } else {
                return new long[]{now - range, now}; //past span
            }
        }

    }

}
