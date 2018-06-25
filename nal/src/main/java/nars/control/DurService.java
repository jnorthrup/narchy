package nars.control;

import nars.NAR;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.Nullable;
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

    protected DurService(@Nullable NAR n, MutableFloat durations) {
        super(n);
        this.durations = durations;
        this.lastStarted = Long.MIN_VALUE;
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
        nar.run(this); 
    }

    @Override
    public final void accept(NAR nar) {
        

        if (!busy.compareAndSet(false, true))
            return;

        long atStart = nar.time();
        int dur = nar.dur();
        long durCycles = Math.round(durations.floatValue() * dur);


        try {
            if (lastStarted == Long.MIN_VALUE)
                lastStarted = atStart;

            long delta = atStart - this.lastStarted;

            this.lastStarted = atStart;
            if (delta >= durCycles) {
                run(nar, delta);
            } else {
                
            }
        } catch (Throwable e) {
            logger.error("{} {}", this, e);
        } finally {
            if (!isOff()) {
                if (busy.compareAndSet(true, false))
                    nar.runAt(atStart + durCycles, this);
            } else {
                busy.set(false);
            }
        }
    }

    /**
     * time (raw cycles, not durations) which elapsed since run was scheduled last
     */
    abstract protected void run(NAR n, long dt);


}
