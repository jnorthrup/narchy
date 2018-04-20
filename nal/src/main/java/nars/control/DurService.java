package nars.control;

import nars.NAR;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * executes approximately once every N durations
 */
abstract public class DurService extends NARService implements Consumer<NAR> {

    static final Logger logger = LoggerFactory.getLogger(DurService.class);

    /**
     * ideal duration multiple to be called, since time after implementation's procedure finished last
     */
    public final MutableFloat durations;

    /** when the last cycle ended */
    private volatile long lastStarted;

    protected final AtomicBoolean busy = new AtomicBoolean(false);

    protected DurService(NAR n, float durs) {
        this(n, new MutableFloat(durs));
    }

    protected DurService(NAR n, MutableFloat durations) {
        super(n);
        this.durations = durations;
        this.lastStarted = n.time() - n.dur();
    }


    protected DurService(NAR nar) {
        this(nar, new MutableFloat(1f));
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
    public DurService durs(float durations) {
        this.durations.set(durations);
        return this;
    }

    @Override
    protected void starting(NAR nar) {
        nar.run(this); //initial
    }

    @Override
    public final void accept(NAR nar) {
        //long lastNow = this.now;
        //long now = nar.time();
        //if (now - lastNow >= durations.floatValue() * nar.dur()) {
        if (!busy.compareAndSet(false, true))
            return;

        long now = nar.time();
        int dur = nar.dur();
        long durCycles = Math.round(durations.floatValue() * dur);

        try {
            long delta = now - this.lastStarted;
            if (delta >= durCycles) {
                run(nar, delta);
            } else {
                //too soon, reschedule
            }
        } catch (Throwable e) {
            logger.error("{} {}", this, e);
        } finally {
            now = (this.lastStarted = nar.time());
            if (!isOff()) {
                nar.runAt(now + durCycles, this);
            }
            busy.set(false); //must be LAST
        }
    }

    /**
     * time (raw cycles, not durations) which elapsed since run was scheduled last
     */
    abstract protected void run(NAR n, long dt);


}
