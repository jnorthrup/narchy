package nars.control;

import nars.NAR;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * executes approximately once every N durations
 */
abstract public class DurService extends NARService implements Runnable {

    static final Logger logger = LoggerFactory.getLogger(DurService.class);

    /**
     * ideal duration multiple to be called, since time after implementation's procedure finished last
     */
    public final MutableFloat durations;

    protected final NAR nar;
    private long now;
    final AtomicBoolean busy = new AtomicBoolean(false);
    private boolean enabled;


    protected DurService(NAR n, float durs) {
        this(n, new MutableFloat(durs));
    }

    protected DurService(NAR n, MutableFloat durations) {
        super(n);
        this.durations = durations;
        this.now = n.time() - 1;
        this.nar = n;
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
        };
    }

    public static DurService on(NAR nar, Consumer<NAR> r) {
        return new DurService(nar) {
            @Override
            protected void run(NAR n, long dt) {
                r.accept(n);
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
        };
    }

    protected DurService(@NotNull NAR nar) {
        this(nar, new MutableFloat(1f));
    }

    public DurService durs(int durations) {
        this.durations.set(durations);
        return this;
    }

    @Override
    protected synchronized void start(NAR nar) {
        super.start(nar);
        enabled = true;
        nar.runLater(this); //initial
    }

    @Override
    public synchronized void off() {
        enabled = false;
        super.off();
    }

    @Override
    public void run() {
        //long lastNow = this.now;
        //long now = nar.time();
        //if (now - lastNow >= durations.floatValue() * nar.dur()) {
        if (enabled && busy.compareAndSet(false, true)) {

            long last = this.now;
            int dur = nar.dur();
            long durCycles = Math.round(durations.floatValue() * nar.dur());
            long now = nar.time();

            try {

                long delta = (now - last);
                if (delta >= durCycles) {
                    try {
                        run(nar, delta);
                    } catch (Throwable t) {
                        logger.error("{} {}", this, t);
                    }
                } else {
                    //too soon, reschedule
                }

            } catch (Exception e) {
                logger.error("{} {}", this, e);
            } finally {
                nar.at((this.now = nar.time()) + durCycles, this);
                busy.set(false);
            }
        }
    }

    /**
     * time (raw cycles, not durations) which elapsed since run was scheduled last
     */
    abstract protected void run(NAR n, long dt);


}
