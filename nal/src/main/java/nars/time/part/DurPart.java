package nars.time.part;

import jcog.Util;
import jcog.WTF;
import jcog.event.Off;
import jcog.math.FloatRange;
import jcog.math.FloatSupplier;
import nars.$;
import nars.NAR;
import nars.Param;
import nars.control.NARPart;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.time.ScheduledTask;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * a part that executes a given procedure once every N durations (approximate)
 * N is an adjustable duration factor (float, so fractional values are ok)
 * the part is retriggered asynchronously so it is not guaranteed consistent
 * timing although some adjustment is applied to help correct for expectable
 * amounts of lag, jitter, latency, etc.
 *
 * these are scheduled by the NAR's Time which holds a priority queue of
 * temporal events.  at any given time it will contain zero or one of this
 * Dur's immutable and re-usable AtDur event.
 */
abstract public class DurPart extends NARPart {

    private static final Logger logger = Util.logger(DurPart.class);

    /**
     * ideal duration multiple to be called, since time after implementation's procedure finished last
     */
    public final FloatRange durations = new FloatRange(1f, 0.1f, 100f);

    private final AtDur at = new AtDur();

    @Override
    public final AtDur event() {
        return at;
    }

    protected DurPart(NAR n, float durs) {
        super((NAR) null); //dont call through super constructor
        durations.set(durs);
        if (n != null) {
            (this.nar = n).start(this);
        }
    }


    protected DurPart(Term id) {
        super(id);
    }

    protected DurPart(NAR nar) {
        this(nar, 1f);
    }

    /**
     * if using this constructor, a subclass must call nar.on(this) manually
     */
    protected DurPart() {
        this((NAR) null);
    }

    /**
     * simple convenient adapter for Runnable's
     */
    public static DurPart on(NAR nar, @NotNull Runnable r) {
        return new MyDurRunnable(nar, r);
    }

    public static DurPart on(NAR nar, @NotNull Consumer<NAR> r) {
        return new MyDurNARConsumer(nar, r);
    }

    public static DurPart onWhile(NAR nar, Predicate<NAR> r) {
        return new DurPart(nar) {
            @Override
            protected void run(NAR n, long dt) {
                if (!r.test(n)) {
                    pause();
                } else {
                    resume();
                }
            }

            @Override
            public String toString() {
                return r.toString();
            }
        };
    }

    /**
     * creates a duration-cached float range that is automatically destroyed when its parent context is
     */
    public static FloatRange cache(FloatSupplier o, float min, float max, DurPart parent, @Deprecated NAR nar) {
        Pair<FloatRange, Off> p = cache(o, min, max, 1, nar);
        parent.on(p.getTwo());
        return p.getOne();
    }

    public static Pair<FloatRange, Off> cache(FloatSupplier o, float min, float max, float durPeriod, NAR n) {
        assert (min < max);
        FloatRange r = new FloatRange((min + max) / 2, min, max);
        DurPart d = DurPart.on(n, () -> {
            float x = o.asFloat();
            if (x==x) {
                r.set(
                        Util.clampSafe(x, min, max)
                );
            } else {
                //r.set(Float.NaN);
            }
        });
        d.durs(durPeriod);
        return Tuples.pair(r, d);
    }

    /**
     * set period (in durations)
     */
    public DurPart durs(float durations) {
        this.durations.set(durations);
        return this;
    }

    @Override protected void starting(@NotNull NAR nar) {
        this.nar = nar;
        at.run();
    }



    public long durCycles() {
        return Math.max(1, Math.round((double) (durations.floatValue()) * this.nar.dur()));
    }

    /**
     * time (raw cycles, not durations) which elapsed since run was scheduled last
     */
    abstract protected void run(NAR n, long dt);


    abstract public static class RecurringTask extends ScheduledTask {

        volatile long next;

        @Override
        public long start() {
            return next;
        }
    }

    private static final Atomic DUR = Atomic.the("dur");

    private static final class MyDurRunnable extends DurPart {
        private final Runnable r;

        MyDurRunnable(NAR nar, @NotNull Runnable r) {
            super();
            this.r = r;
            nar.start(this);
        }

        @Override
        protected void run(NAR n, long dt) {
            r.run();
        }

        @Override
        public String toString() {
            return getClass().getSimpleName() + ':' + r;
        }
    }

    private static final class MyDurNARConsumer extends DurPart {

        final Consumer<NAR> r;

        MyDurNARConsumer(NAR nar, @NotNull Consumer<NAR> r) {
            super();
            this.r = r;
            nar.start(this);
        }

        @Override
        protected void run(NAR n, long dt) {
            r.accept(n);
        }


        @Override
        public String toString() {
            return getClass().getSimpleName() + ':' + r;
        }
    }

    public class AtDur extends RecurringTask {

        /**
         * when the last cycle ended
         */
        private volatile long lastStarted = Long.MIN_VALUE;

        final AtomicBoolean busy = new AtomicBoolean(false);

        @Override
        public Term term() {
            return $.p(id, $.p(DUR, $.the(durations.floatValue())));
        }

        @Override
        public void run() {

            if (!busy.compareAndSet(false, true))
                throw new WTF(); //return false;

            try {

                long atStart = nar.time();

                long lastStarted = this.lastStarted;
                if (lastStarted == Long.MIN_VALUE)
                    lastStarted = atStart;

                this.lastStarted = atStart;

                long delta = atStart - lastStarted;
                long d = durCycles(); //get prior in case dur changes during execution

                try {
                    DurPart.this.run(nar, delta);
                } catch (Throwable t) {
                    logger.error("{} {}", this, t);
                }

                if (!DurPart.this.isOff()) {
                    scheduleNext(d, atStart);
                }

            } finally {
                busy.set(false);
            }
        }

        private void scheduleNext(long d, long started) {

            long now = nar.time();

            long idealNext = started + d;
            if (idealNext <= now) {
                /** LAG - compute a correctional shift period, so that it attempts to maintain a steady rhythm and re-synch even if a frame is lagged*/
                long phaseLate = (now - idealNext) % d;
                //idealNext = now + 1; //immediate
                idealNext = now + Math.max(1, d - phaseLate);

                if (Param.DEBUG) {
                    long earliest = started + d;
                    assert (next >= earliest) : "starting too soon: " + next + " < " + earliest;
                    long latest = now + d;
                    assert (next <= latest) : "starting too late: " + next + " > " + earliest;
                }
            }

            next = idealNext;

            nar.runAt(at);
        }
    }
}
