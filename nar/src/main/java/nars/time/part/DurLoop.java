package nars.time.part;

import jcog.Util;
import jcog.event.Off;
import jcog.math.FloatRange;
import jcog.math.FloatSupplier;
import nars.$;
import nars.NAR;
import nars.control.NARPart;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.time.RecurringTask;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;

import java.util.concurrent.atomic.AtomicBoolean;

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

//    protected DurLoop(@Nullable NAR n, float durs) {
//        super((NAR) null); //dont call through super constructor
//        durations.set(durs);
//        if (n != null) {
//            n.start(this);
//        }
//    }


    protected DurLoop(Term id) {
        super(id);
    }


    /**
     * creates a duration-cached float range that is automatically destroyed when its parent context is
     */
    public static FloatRange cache(FloatSupplier o, float min, float max, DurLoop parent, @Deprecated NAR nar) {
        Pair<FloatRange, Off> p = cache(o, min, max, 1, nar);
        parent.on(p.getTwo());
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
        at.nextStart = nar.time();
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



    public static final class DurRunnable extends DurLoop {
        private final Runnable r;

        public DurRunnable(Runnable r) {
            super($.identity(r));
            this.r = r;
        }

        @Override
        protected void run(NAR n, long dt) {
            r.run();
        }

    }


    public class WhenDur extends RecurringTask {

        /**
         * when the last cycle ended
         */
        private volatile long lastStarted = Long.MIN_VALUE;

        final AtomicBoolean busy = new AtomicBoolean(false);

        @Override
        public void accept(NAR nar) {

            if (!isOn())
                return; //cancelled between previous iteration and this iteration
            //assert(nar == DurLoop.this.nar);

            if (!busy.compareAndSet(false, true))
                return;

            long atStart = nar.time();
            try {

                long lastStarted = this.lastStarted;
                if (lastStarted == Long.MIN_VALUE)
                    lastStarted = atStart;

                this.lastStarted = atStart;

                long delta = atStart - lastStarted;

                DurLoop.this.run(nar, delta);

            } finally {
                //TODO catch Exception, option for auto-stop on exception

                @Deprecated NAR nnar = DurLoop.this.nar; //prevent NPE in durCycles()
                if (nnar!=null && DurLoop.this.isOn()) {
                    this.nextStart = scheduleNext(durCycles(), atStart);

                    //System.out.println(lastStarted + " -> "  + atStart + " -> " + nar.time() + " -> " + nextStart);

                    nar.runAt(this);
                }

                busy.set(false);
            }

        }
        long scheduleNext(long durCycles, long started) {

            long now = nar.time();

            final long idealNext = started + durCycles;
            long lag = now - idealNext;
            if (lag > 0) {
                /** LAG - compute a correctional shift period, so that it attempts to maintain a steady rhythm and re-synch even if a frame is lagged*/

                nar.emotion.durLoopLag.add(lag);

//            if (Param.DEBUG) {
//                long earliest = started + durCycles;
//                assert (nextStart >= earliest) : "starting too soon: " + nextStart + " < " + earliest;
//                long latest = now + durCycles;
//                assert (nextStart <= latest) : "starting too late: " + nextStart + " > " + earliest;
//            }

                //async immediate:
                return now;

                //balanced
//                long phaseLate = (now - idealNext) % durCycles;
//                long delayToAlign = durCycles - phaseLate;
//                if (delayToAlign < durCycles/2) {
//                    return now + delayToAlign;
//                } else
//                    return now;

                //skip to keep aligned with phase:
                //long phaseLate = (now - idealNext) % durCycles;
                //return now + Math.max(1, durCycles - phaseLate);

            } else
                return idealNext;
        }


        @Override
        public final Term term() {
            return $.identity(this); //globally unique
        }
    }


    
    public static String toString(Object r) {
        return WhenDur.class.getSimpleName() + '(' + r + ')';
    }
}
