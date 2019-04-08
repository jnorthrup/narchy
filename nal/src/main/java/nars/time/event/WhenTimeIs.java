package nars.time.event;

import nars.$;
import nars.NAR;
import nars.attention.What;
import nars.derive.Derivation;
import nars.task.util.Answer;
import nars.term.Term;
import nars.time.ScheduledTask;
import nars.time.Tense;
import nars.time.When;

import java.util.function.IntSupplier;

public final class WhenTimeIs extends ScheduledTask {
    private final long whenOrAfter;
    private final Runnable then;

    public WhenTimeIs(long whenOrAfter, Runnable then) {
        this.whenOrAfter = whenOrAfter;
        this.then = then;
    }

    public static When<NAR> range(long subStart, long subEnd, NAR n) {
        return new When<>(subStart, subEnd, n.dur(), n);
    }

    public static When<NAR> range(long subStart, long subEnd, Answer a) {
        return new When<>(subStart, subEnd, a.dur, a.nar);
    }

    public static When<NAR> eternal(NAR n) {
        return new When(Tense.ETERNAL, Tense.ETERNAL, n.dur(), n);
    }

    /** generates a default 'now' moment: current X clock time with dur/2 radius.
     *  the equal-length past and future periods comprising the extent of the present moment. */
    public static When<NAR> now(int dur, NAR nar) {
        long now = nar.time();
        return new When(now - dur/2, now + dur/2, dur, nar);
    }
    private static When<NAR> now(IntSupplier dur, NAR nar) {
        return now(dur.getAsInt(), nar);
    }
    public static When<NAR> now(NAR nar) {
        return now(nar.dur(), nar);
    }
    public static When<NAR> now(Derivation d) {
        return now(d::dur, d.nar());
    }
    public static When<NAR> now(What w) {
        return now(w::dur, w.nar);
    }

    public static When<NAR> since(long when, NAR nar) {
        long now = nar.time();
        return new When(Math.min(when, now), now, nar.dur(), nar);
    }

    public static When<NAR> until(long when, NAR nar) {
        long now = nar.time();
        return new When(now, Math.max(when, now), nar.dur(), nar);
    }

    @Override
    public long start() {
        return whenOrAfter;
    }

    @Override
    public void run() {
        then.run();
    }

    @Override
    public Term term() {
        return $.p($.identity(then), $.the(whenOrAfter));
    }
}
