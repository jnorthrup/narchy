package nars.control;

import jcog.TODO;
import jcog.Util;
import nars.NAR;
import nars.attention.PriNode;
import nars.exe.Exec;
import nars.term.Term;
import nars.time.event.InternalEvent;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;

import static nars.time.Tense.TIMELESS;

/**
 *
 * instances of How represent a "mental strategy", ie. a mode of thinking/perceiving/acting,
 * which the system can learn to deliberately apply to system activity.
 *
 * see: https://cogsci.indiana.edu/pub/parallel-terraced-scan.pdf
 *
 *
 *
 * instruments the runtime resource consumption of its iteratable procedure.
 * this determines a dynamically adjusted strength parameter
 * that the implementation can use to modulate its resource needs.
 * these parameters are calculated in accordance with
 * other instances in an attempt to achieve a fair and
 * predictable (ex: linear) relationship between its scalar value estimate
 * and the relative system resources it consumes.
 * <p>
 * records runtime instrumentation, profiling, and other telemetry for a particular Causable
 * both per individual threads, and collectively
 */
abstract public class How extends NARPart {

    /**
     * returns iterations actually completed
     * returns 0 if no work was done, although the time taken will still be recorded
     * if returns -1, then it signals there is no work availble
     * and time will not be recorded. further a scheduler can assume
     * this will remain true for the remainder of the cycle, so it can be
     * removed from the eligible execution list for the current cycle.
     */
    public abstract void next(NAR n, BooleanSupplier kontinue);

    /**
     * TODO varHandle
     */
    public final AtomicBoolean busy;
    public final PriNode pri;
    public final AtomicLong used = new AtomicLong(0);
    /**
     * cached: last calculated non-negative value rate
     */
    transient public float valueRate;
    /**
     * cached: last calculated positive, negative, or NaN value rate
     */
    transient public float value;
    private volatile long sleepUntil = TIMELESS;
    private volatile boolean sleeping;

    @Deprecated
    protected How(NAR nar) {
        this(nar, null);
    }

    protected How() {
        this((Term) null);
    }

    /**
     * if using this constructor, make sure to call 'nar.on(this);' in the callee
     */
    protected How(Term id) {
        this(null, id);
    }

    private How(NAR nar, Term id) {
        super(id);
        this.pri = new PriNode(id!=null ? id : this);
        this.nar = nar;
        this.busy = //new Semaphore(singleton() ?  1 : Runtime.getRuntime().availableProcessors());
                singleton() ? new AtomicBoolean(false) : null;
        if (nar != null)
            nar.start(this);
    }

    @Override
    protected void starting(NAR nar) {
        super.starting(nar);
        nar.control.add(pri);
    }

    @Override
    protected void stopping(NAR nar) {
        nar.control.remove(pri);
        super.stopping(nar);
    }


    /** by default, causable are singleton */
    @Override public boolean singleton() {
        return true;
    }

    /**
     * sytem time, not necessarily realtime
     */
    protected void sleepUntil(long time) {
        this.sleepUntil = time;
    }

//    protected void sleepRemainderOfCycle() {
//        sleepUntil(nar.time()+1);
//    }

    public boolean sleeping(NAR nar) {
        if (sleepUntil == TIMELESS)
            return false;
        return sleeping(nar.time());
    }

    boolean sleeping(long now) {
        if (sleepUntil < now) {
            sleepUntil = TIMELESS;
            return sleeping = true;
        } else {
            return sleeping = false;
        }
    }

    public boolean sleeping() {
        return sleeping;
    }



    /**
     * returns a system estimated instantaneous-sampled value of invoking this. between 0..1.0
     */
    public abstract float value();

    public final void pri(float p) {
        pri.pri(p);
    }

    private void use(long t) {
        used.addAndGet(t);
    }

    public long used() {
        return used.getAndSet(0);
    }

    public final float pri() {
        return pri.pri();
    }



    @Deprecated
    public InternalEvent event() {
        return myCause;
    }
    public Causation timing() {
        return new Causation();
    }

    private final InternalEvent myCause = new AtCause(id);

    static private class AtCause extends InternalEvent {

        private final Term id;

        AtCause(Term id) {
            this.id = id;
        }

        @Override
        public Term term() {
            return id;
        }
    }

    /**
     * thread-local view
     * instance of a cause invocation
     */
    public final class Causation {

        public final How can = How.this;
        /**
         * allocated time for execution;
         * may be negative when excessive time consumed
         */
        public long time = 0;

        //            public long addAt(long t) {
//
//            }
        public void use(long t) {
            How.this.use(t);
            time -= t;
        }

        public float pri() {
            return How.this.pri.priElseZero();
        }

        public void add(long t, long min, long max) {
            time = Util.clampSafe((time + t), min, max);
        }


        public void runUntilMS(long msTime) {
            throw new TODO();
        }

        public void runUntilNS(long nanoTime) {
            throw new TODO();
        }

        public final void runFor(long durationNS) {
//TODO
//            if (singleton)
//                c.busy.set(false);

            long start = System.nanoTime();
            long deadline = start + durationNS;
            try {
                can.next(nar, () -> System.nanoTime() < deadline);
            } catch (Throwable t) {
                Exec.logger.error("{} {}", can, t);
            } finally {
                long end = System.nanoTime();
                use(end - start);
            }
        }
    }


}
