package nars.time.part;

import jcog.Util;
import jcog.WTF;
import jcog.event.Off;
import jcog.math.FloatRange;
import jcog.math.FloatSupplier;
import nars.$;
import nars.NAR;
import nars.control.NARPart;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atomic;
import nars.time.RecurringTask;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static nars.time.Tense.TIMELESS;

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
abstract public class DurLoop extends NARPart {

    /**
     * ideal duration multiple to be called, since time after implementation's procedure finished last
     */
    public final FloatRange durations = new FloatRange(1, 0.25f, 16f);

    private final WhenDur at = new WhenDur();

    @Override
    public final WhenDur event() {
        return at;
    }

    protected DurLoop(@Nullable NAR n, float durs) {
        super((NAR) null); //dont call through super constructor
        durations.set(durs);
        if (n != null) {
            n.start(this);
        }
    }


    protected DurLoop(Term id) {
        super(id);
    }


    /**
     * creates a duration-cached float range that is automatically destroyed when its parent context is
     */
    public static FloatRange cache(FloatSupplier o, float min, float max, DurLoop parent, @Deprecated NAR nar) {
        Pair<FloatRange, Off> p = cache(o, min, max, 1, nar);
        parent.whenDeleted(p.getTwo());
        return p.getOne();
    }

    public static Pair<FloatRange, Off> cache(FloatSupplier o, float min, float max, float durPeriod, NAR n) {
        assert (min < max);
        FloatRange r = new FloatRange((min + max) / 2, min, max);
        //r.set(Float.NaN);
        DurLoop d = n.onDur(() -> {
            float x = o.asFloat();
            if (x == x) {
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
    public DurLoop durs(float durations) {
        this.durations.set(durations);
        return this;
    }

    @Override protected final void starting(NAR nar) {
        //intial trigger
        nar.runLater(at);
    }


    public long durCycles() {
        return Math.max(1, Math.round((double) (durations.floatValue()) * this.nar.dur()));
    }

    /**
     * time (raw cycles, not durations) which elapsed since run was scheduled last
     */
    abstract protected void run(NAR n, long dt);



    private static final Atomic DUR = Atomic.the("dur");

//    public static String name(Object r) {
//        String n;
//        Class c = r.getClass();
//        String className = c.getSimpleName();
//        if (c.isSynthetic())
//            n = className;
//        else
//            n = className + ':' + r;
//        return n;
//    }

    static Term term(Object x) {
        return x instanceof Termed ? ((Termed)x).term() : $.identity(x);
    }

    public static final class DurRunnable extends DurLoop {
        private final Runnable r;

        public DurRunnable(Runnable r) {
            super(term(r));
            this.r = r;
        }

        @Override
        protected void run(NAR n, long dt) {
            r.run();
        }

        @Override
        public String toString() {
            return toString(r);
        }

    }

    public static final class DurNARConsumer extends DurLoop {

        final Consumer<NAR> r;

        public DurNARConsumer(Consumer<NAR> r) {
            super(term(r));
            this.r = r;
        }

        @Override
        protected void run(NAR n, long dt) {
            r.accept(n);
        }


        @Override
        public String toString() {
            return toString(r);
        }


    }


    public class WhenDur extends RecurringTask {

        /**
         * when the last cycle ended
         */
        private volatile long lastStarted = Long.MIN_VALUE;

        final AtomicBoolean busy = new AtomicBoolean(false);

        @Override
        public void run() {

            if (!busy.compareAndSet(false, true))
                throw new WTF(); //return false;

            long atStart = TIMELESS;
            try {

                NAR nar = DurLoop.this.nar;
                if (nar == null || !isOn())
                    return; //cancelled between previous iteration and this iteration

                atStart = nar.time();

                long lastStarted = this.lastStarted;
                if (lastStarted == Long.MIN_VALUE)
                    lastStarted = atStart;

                this.lastStarted = atStart;

                long delta = atStart - lastStarted;

//                try {
                    DurLoop.this.run(nar, delta);
//                } catch (Throwable t) {
//                    logger.error("{} {}", this, t);
//                }


            } finally {
                //TODO catch Exception, option for auto-stop on exception

                NAR n = DurLoop.this.nar;
                if (n!=null && DurLoop.this.isOn()) {
                    scheduleNext(durCycles(), atStart, n);
                }

                busy.set(false);
            }

        }


        @Override
        public final Term term() {
            return $.identity(this); //globally unique
        }
    }


    
    public static String toString(Object r) {
        return WhenDur.class.getSimpleName() + "(" + r + ")";
    }
}
