package nars.derive.timing;

import nars.NAR;
import nars.Task;
import nars.term.Term;
import nars.time.Time;

import java.util.Random;
import java.util.function.BiFunction;

import static nars.time.Tense.ETERNAL;

/** naively applies a variety of methods for calculating time focus targets */
public class AdHocDeriverTiming implements BiFunction<Task, Term, long[]> {

    private final Time clock;
    private final Random rng;
    private final NAR nar;

    public AdHocDeriverTiming(NAR n) {
        this.nar = n;
        this.clock = n.time;
        this.rng = n.random();
    }

    @Override
    public long[] apply(Task task, Term term) {
        if (task.isEternal())
            return new long[] { ETERNAL, ETERNAL };

        long[] tt;
        switch (rng.nextInt(3)) {
            case 0:
                tt = presentDuration(1); break;
            case 1:
                tt = taskTime(task); break;
            case 2: {
                boolean past =
                        //(task.mid() < nar.time());
                        nar.random().nextBoolean();
                tt = pastFutureRadius(task, past, !past);
                break;
            }
            default:
                throw new UnsupportedOperationException();
        }

        return tt;
    }



    private long[] presentDuration(float factor) {
        long now = clock.now();
        int dur = Math.round(factor * clock.dur());
        return new long[] { now - dur/2, now + dur/2 };
    }

    private long[] taskTime(Task t) {
        return new long[] {t.start(), t.end() };
    }

    private long[] pastFutureRadius(Task t, boolean past, boolean future) {
        long[] tt = taskTime(t);
        if (tt[0]==ETERNAL) {
            return presentDuration(4);
        } else {

            long now = nar.time();

            long range = Math.max(Math.abs(tt[0] - now), Math.abs(tt[1] - now)) * 2;

            float factor = nar.random().nextFloat();
            factor *= factor * factor; //^3
            range = Math.round(factor * range);

            if (past && future) {
                return new long[]{now - range, now + range}; //future span
            } else if (future) {
                return new long[]{now, now + range}; //future span
            } else {
                return new long[]{now - range, now}; //past span
            }
        }

    }

}
