package nars.time.event;

import nars.$;
import nars.NAR;
import nars.attention.What;
import nars.term.Term;
import nars.time.ScheduledTask;
import nars.time.Tense;
import nars.time.When;
import nars.util.Timed;

import java.util.function.Consumer;

abstract public class WhenTimeIs extends ScheduledTask {

    public final long whenOrAfter;

    public static WhenTimeIs then(long whenOrAfter, Object then) {
        if (then instanceof Runnable)
            return new WhenTimeIs_Run(whenOrAfter, (Runnable)then);
        else
            return new WhenTimeIs_Consume(whenOrAfter, (Consumer)then);
    }

    private static final class WhenTimeIs_Consume extends WhenTimeIs {
        private final Consumer<NAR> then;

        private WhenTimeIs_Consume(long whenOrAfter, Consumer<NAR> then) {
            super(whenOrAfter);
            this.then = then;
        }

        @Override
        public void accept(NAR nar) {
            then.accept(nar);
        }

        @Override
        protected Object _id() {
            return then;
        }
    }

    private static final class WhenTimeIs_Run extends WhenTimeIs {
        private final Runnable then;

        private WhenTimeIs_Run(long whenOrAfter, Runnable then) {
            super(whenOrAfter);
            this.then = then;
        }


        @Override
        public void accept(NAR nar) {
            then.run();
        }

        @Override
        protected Object _id() {
            return then;
        }
    }

    WhenTimeIs(long whenOrAfter) {
        this.whenOrAfter = whenOrAfter;
    }

    public static <T extends Timed> When<T>  eternal(T n) {
        return new When<>(Tense.ETERNAL, Tense.ETERNAL, n.dur(), n);
    }

    public static <T extends Timed> When<T> now(float dur, T t) {
        return now(t, dur, 1);
    }

    /** generates a default 'now' moment: current X clock time with dur/2 radius.
     *  the equal-length past and future periods comprising the extent of the present moment. */
    public static <T extends Timed> When<T> now(T t, float dur, int dither) {
        return now(t, dur, t.time(), dur/2, dur/2, dither);
    }

    /** dur doesnt necesarily need to have any relation to timeBefore, timeAfter */
    public static <T extends Timed> When<T> now(T t, float dur, long now, float before, float after, int dither) {
        return now(t, dur,
            (long) Math.floor(now - before),
            (long) Math.ceil(now + after), dither);
    }

    public static <T extends Timed> When<T> now(T t, float dur, long s, long e, int dither) {
        if (dither > 1) {
            s = Tense.dither(s, dither, -1);
            e = Tense.dither(e, dither, +1);
        }
        return new When<>(s, e, dur, t);
    }

    public static <T extends Timed> When<T> now(T t) {
        return now(t.dur(), t);
    }

    public static When<NAR> now(What w) {
        return now(w, w.dur());
    }

    public static When<NAR> now(What w, float dur) {
        return now(w.nar, dur, w.nar.dtDither());
    }

    public static <T extends Timed> When<T> since(long when, T t) {
        long now = t.time();
        return new When<>(Math.min(when, now), now, t.dur(), t);
    }

    public static <T extends Timed> When<T> until(long when, T t) {
        long now = t.time();
        return new When<>(now, Math.max(when, now), t.dur(), t);
    }

    @Override
    public long start() {
        return whenOrAfter;
    }

    @Override
    public Term term() {
        return $.p($.identity(_id()), $.the(whenOrAfter));
    }

    protected abstract Object _id();


}
