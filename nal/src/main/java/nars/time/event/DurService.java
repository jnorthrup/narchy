package nars.time.event;

import jcog.Util;
import jcog.WTF;
import jcog.data.NumberX;
import jcog.data.atomic.AtomicFloat;
import jcog.event.Off;
import jcog.math.FloatRange;
import jcog.math.FloatSupplier;
import nars.$;
import nars.NAR;
import nars.Param;
import nars.control.NARService;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.time.ScheduledTask;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * executes approximately once every N durations
 */
abstract public class DurService extends NARService {

    private static final Logger logger = LoggerFactory.getLogger(DurService.class);

    /**
     * ideal duration multiple to be called, since time after implementation's procedure finished last
     */
    private final NumberX durations = new AtomicFloat(1f);

    @Override
    public InternalEvent event() {
        return at;
    }

    protected DurService(NAR n, float durs) {
        super((NAR) null); //dont call through super constructor
        durations.set(durs);
        if (n != null) {
            (this.nar = n).on(this);
        }
    }


    protected DurService(Term id) {
        super(id);
    }

    protected DurService(NAR nar) {
        this(nar, 1f);
    }

    /**
     * if using this constructor, a subclass must call nar.on(this) manually
     */
    protected DurService() {
        this((NAR) null);
    }

    /**
     * simple convenient adapter for Runnable's
     */
    public static DurService on(NAR nar, Runnable r) {
        return new DurService(nar) {
            @Override
            protected void run(NAR n, long dt) {
                r.run();
            }

            @Override
            public String toString() {
                return r.toString();
            }
        };
    }

    public static DurService on(NAR nar, Consumer<NAR> r) {
        return new DurService(nar) {
            @Override
            protected void run(NAR n, long dt) {
                r.accept(n);
            }

            @Override
            public String toString() {
                return r.toString();
            }
        };
    }

    public static DurService onWhile(NAR nar, Predicate<NAR> r) {
        return new DurService(nar) {
            @Override
            protected void run(NAR n, long dt) {
                if (!r.test(n)) {
                    off();
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
    public static FloatRange cache(FloatSupplier o, float min, float max, DurService parent, @Deprecated NAR nar) {
        Pair<FloatRange, Off> p = cache(o, min, max, 1, nar);
        parent.on(p.getTwo());
        return p.getOne();
    }

    public static Pair<FloatRange, Off> cache(FloatSupplier o, float min, float max, float durPeriod, NAR n) {
        assert (min < max);
        FloatRange r = new FloatRange((min + max) / 2, min, max);
        DurService d = DurService.on(n, () -> {
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
    public DurService durs(float durations) {
        this.durations.set(durations);
        return this;
    }

    @Override
    protected void starting(NAR nar) {
        at.run();
    }


    private final AtDur at = new AtDur();

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
                    DurService.this.run(nar, delta);
                } catch (Throwable t) {
                    logger.error("{} {}", this, t);
                }

                if (!DurService.this.isOff()) {
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
