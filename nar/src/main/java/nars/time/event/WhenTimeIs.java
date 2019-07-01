package nars.time.event;

import jcog.math.FloatSupplier;
import nars.$;
import nars.NAR;
import nars.attention.What;
import nars.derive.model.Derivation;
import nars.task.util.Answer;
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

    public static When<NAR> range(long subStart, long subEnd, Answer a) {
        return new When<>(subStart, subEnd, a.dur, a.nar);
    }

    public static When<NAR> eternal(Timed n) {
        return new When(Tense.ETERNAL, Tense.ETERNAL, n.dur(), n);
    }

    /** generates a default 'now' moment: current X clock time with dur/2 radius.
     *  the equal-length past and future periods comprising the extent of the present moment. */
    public static When<NAR> now(float dur, Timed nar) {
        long now = nar.time();
        return new When(Math.round(now - dur/2), Math.round(now + dur/2), dur, nar);
    }
    private static When<NAR> now(FloatSupplier dur, Timed nar) {
        return now(dur.asFloat(), nar);
    }
    public static When<NAR> now(Timed t) {
        return now(t.dur(), t);
    }
    public static When<NAR> now(Derivation d) {
        return now(d::dur, d.nar());
    }
    public static When<NAR> now(What w) {
        return now(w::dur, w.nar);
    }

    public static When<NAR> since(long when, Timed t) {
        long now = t.time();
        return new When(Math.min(when, now), now, t.dur(), t);
    }

    public static When<NAR> until(long when, Timed t) {
        long now = t.time();
        return new When(now, Math.max(when, now), t.dur(), t);
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
