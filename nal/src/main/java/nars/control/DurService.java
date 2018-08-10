package nars.control;

import jcog.data.NumberX;
import jcog.data.atomic.AtomicFloat;
import nars.NAR;
import nars.term.Term;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * executes approximately once every N durations
 */
abstract public class DurService extends NARService implements Consumer<NAR> {

    private static final Logger logger = LoggerFactory.getLogger(DurService.class);

    /**
     * ideal duration multiple to be called, since time after implementation's procedure finished last
     */
    private final NumberX durations = new AtomicFloat(1f);

    /** when the last cycle ended */
    private volatile long lastStarted = Long.MIN_VALUE;
    //private volatile long lastFinished = Long.MIN_VALUE;
    private final AtomicBoolean busy = new AtomicBoolean(false);


    protected DurService(NAR n, float durs) {
        super((NAR)null);
        durations.set(durs);
        if (n!=null)
            n.on(this);
    }


    protected DurService(Term id) {
        super(id);
    }

    protected DurService(NAR nar) {
        this(nar, 1f);
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
        long now = nar.time();
        long durCycles = durCycles();
        lastStarted = now - durCycles;
        //lastFinished = lastStarted - durCycles;
        //spawn(nar, now + durCycles);
        accept(nar);
    }

    @Override
    public final void accept(NAR nar) {


        //System.out.println(this + " " + lastStarted + ".." + lastFinished);

        if (!busy.weakCompareAndSetAcquire(false, true))
            return;


        long durCycles = durCycles();

        long atStart = nar.time();

        long lastStarted = this.lastStarted;

        this.lastStarted = atStart;

        long delta = atStart - lastStarted;

        //assert(delta >= durCycles): delta + " delta, " + durCycles + " durCycles";
        //System.out.println(this + "\t" + delta + " delta, " + durCycles + " durCycles");


        try {


            run(nar, delta);

        } catch (Throwable e) {
            logger.error("{} {}", this, e);
        } finally {
            //long lastFinished = this.lastFinished;
            long atEnd = nar.time();
            //this.lastFinished = atEnd;

            if (!isOff()) {

                long next = Math.max(
                        atEnd + 1 /* next cycle */,
                        atStart + durCycles);

                //System.out.println(this + "\tnext=" + next);

                nar.runAt(next, this);

            }
            busy.setRelease(false);
        }
    }

    public long durCycles() {
        return Math.round(durations.floatValue() * this.nar.dur());
    }

    /**
     * time (raw cycles, not durations) which elapsed since run was scheduled last
     */
    abstract protected void run(NAR n, long dt);



}
