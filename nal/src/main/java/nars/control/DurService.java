package nars.control;

import jcog.Util;
import jcog.data.NumberX;
import jcog.data.atomic.AtomicFloat;
import jcog.event.Off;
import jcog.math.FloatRange;
import jcog.math.FloatSupplier;
import nars.NAR;
import nars.task.AbstractTask;
import nars.term.Term;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;
import java.util.function.Predicate;

import static nars.time.Tense.TIMELESS;

/**
 * executes approximately once every N durations
 */
abstract public class DurService extends NARService  {

    private static final Logger logger = LoggerFactory.getLogger(DurService.class);

    /**
     * ideal duration multiple to be called, since time after implementation's procedure finished last
     */
    private final NumberX durations = new AtomicFloat(1f);

    /** when the last cycle ended */
    private volatile long lastStarted = Long.MIN_VALUE;
    //private volatile long lastFinished = Long.MIN_VALUE;
    //private final AtomicBoolean busy = new AtomicBoolean(false);


    protected DurService(NAR n, float durs) {
        super((NAR)null); //dont call through super constructor
        durations.set(durs);
        if (n!=null) {
            (this.nar = n).on(this);
        }
    }


    protected DurService(Term id) {
        super(id);
    }

    protected DurService(NAR nar) {
        this(nar, 1f);
    }

    /** if using this constructor, a subclass must call nar.on(this) manually */
    protected DurService() {
        this((NAR)null);
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
            @Override protected void run(NAR n, long dt) {
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

    /** creates a duration-cached float range that is automatically destroyed when its parent context is */
    public static FloatRange cache(FloatSupplier o, float min, float max, DurService parent, @Deprecated NAR nar) {
        Pair<FloatRange, Off> p = cache(o, min, max, 1, nar);
        parent.on(p.getTwo());
        return p.getOne();
    }

    public static Pair<FloatRange, Off> cache(FloatSupplier o, float min, float max, float durPeriod, NAR n) {
        assert(min < max);
        FloatRange r = new FloatRange((min + max) / 2, min, max);
        DurService d = DurService.on(n, () -> {
            r.set(
                Util.clampSafe(o.asFloat(), min, max)
            );
        });
        d.durs(durPeriod);
        return Tuples.pair(r, d);
    }

    /** set period (in durations) */
    public DurService durs(float durations) {
        this.durations.set(durations);
        return this;
    }

    @Override
    protected void starting(NAR nar) {

        long now = nar.time();
        //long durCycles = durCycles();
        lastStarted = now;// - durCycles;
        //lastFinished = lastStarted - durCycles;
        //spawn(nar, now + durCycles);
        run();
    }

    private void run() {

//        if (!busy.compareAndSet(false, true))
//            return;

//        try {

            long lastStarted = this.lastStarted;

            long atStart = nar.time();

            this.lastStarted = atStart;

            long delta = atStart - lastStarted;

            run(nar, delta);

            scheduleNext( atStart );

//        }  finally {
//
//            busy.set(false);
//        }
    }

    private transient long next = TIMELESS;

    private void scheduleNext(long atStart) {



            long d = durCycles();
            long idealNext = atStart + d;
            long now = nar.time();
            if (idealNext < now) {
                //LAG
                //compute a correctional shift period, so that it attempts to maintain a steady rhythm and re-synch even if a frame is lagged
                long phaseLate = (now - idealNext) % d;
                //idealNext = now + 1; //immediate
                idealNext = now + Math.max(1, d - phaseLate);
            }

            next = idealNext;

            nar.runAt(myScheduledTask);
    }

    private final AbstractTask.ScheduledTask myScheduledTask = new AbstractTask.ScheduledTask() {

        @Override
        public void run() {
            DurService.this.run();
        }

        @Override
        public long when() {
            return next;
        }
    };

    public long durCycles() {
        return Math.round((double)(durations.floatValue()) * this.nar.dur());
    }

    /**
     * time (raw cycles, not durations) which elapsed since run was scheduled last
     */
    abstract protected void run(NAR n, long dt);



}
