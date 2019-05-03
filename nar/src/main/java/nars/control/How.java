package nars.control;

import jcog.Log;
import jcog.Skill;
import jcog.pri.Prioritizable;
import nars.NAR;
import nars.attention.PriNode;
import nars.attention.What;
import nars.term.Term;
import nars.time.event.WhenInternal;
import org.slf4j.Logger;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;

import static nars.time.Tense.TIMELESS;

/**
 *
 * instances of How represent a "mental strategy" of thought.
 * a mode of thinking/perceiving/acting,
 * which the system can learn to
 * deliberately apply.
 *
 * a How also implies the existence for a particular reason Why it Should.
 * so there is functional interaction between How's and Why's
 * and their combined role in thinking What-ever.
 *
 * see: https://cogsci.indiana.edu/pub/parallel-terraced-scan.pdf
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
@Skill({
    "Effects_unit",
    "Utility_maximization_problem",
    "Optimal_decision",
    "Action_axiom",
    "Norm_(artificial_intelligence)"
})
abstract public class How extends NARPart implements Prioritizable {

    public static final Logger logger = Log.logger(How.class);

    public abstract void next(What w, BooleanSupplier kontinue);

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

    /** for miscellaneous external controller use */
    public volatile Object governor = null;

    protected How() {
        this(null, null);
    }

    /**
     * if using this constructor, make sure to call 'nar.on(this);' in the callee
     */
    protected How(Term id) {
        this(id, null);
    }

    protected How(Term id, NAR nar) {
        super(id);
        this.pri = new PriNode(this.id);
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


    /** by default, causable are singleton.
     * */
    @Deprecated @Override public boolean singleton() {
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

    public boolean inactive(NAR nar) {
        if (sleepUntil == TIMELESS)
            return false;
        return inactive(nar.time());
    }

    boolean inactive(long now) {
        if (sleepUntil < now) {
            sleepUntil = TIMELESS;
            return sleeping = true;
        } else {
            return sleeping = false;
        }
    }

    @Deprecated public boolean inactive() {
        return sleeping;
    }





    /**
     * returns a system estimated instantaneous-sampled value of invoking this. between 0..1.0
     */
    public abstract float value();


    private void use(long t) {
        used.addAndGet(t);
    }

    public long used() {
        return used.getAndSet(0);
    }

    @Override public final float pri() {
        return pri.pri();
    }

    public final float pri(float p) {
        pri.pri(p); return p;
    }



    @Deprecated
    public WhenInternal event() {
        return myCause;
    }

    private final WhenInternal myCause = new AtCause(id);

//    /**
//     * 0..+1
//     */
//    public float amp() {
//        return Math.max(Float.MIN_NORMAL, gain() / 2f);
//    }
//
//    /**
//     * 0..+2
//     */
//    private float gain() {
//        float v = this.valueRate;
//        return v == v ? Util.tanhFast(v) + 1f : 0;
//    }

    static private class AtCause extends WhenInternal {

        private final Term id;

        AtCause(Term id) {
            this.id = id;
        }

        @Override
        public Term term() {
            return id;
        }
    }


    public final void runFor(What w, long durationNS) {
        long start = System.nanoTime();
        long deadline = start + durationNS;
        try {
            next(w, () -> System.nanoTime() < deadline);
        } catch (Throwable t) {
            logger.error("{} {}", this, t.getMessage());
        } finally {
            long end = System.nanoTime();
            use(end - start);
        }
    }

}
