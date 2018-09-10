package nars.exe;

import jcog.exe.Can;
import nars.NAR;
import nars.control.NARService;
import nars.term.Term;

import java.util.concurrent.Semaphore;
import java.util.function.BooleanSupplier;

/**
 * instruments the runtime resource consumption of its iteratable procedure.
 * this determines a dynamically adjusted strength parameter
 * that the implementation can use to modulate its resource needs.
 * these parameters are calculated in accordance with
 * other instances in an attempt to achieve a fair and
 * predictable (ex: linear) relationship between its scalar value estimate
 * and the relative system resources it consumes.
 */
abstract public class Causable extends NARService {

//    private static final Logger logger = LoggerFactory.getLogger(Causable.class);

    public final Can can;

    /** id as set by the scheduler to identify it */
    @Deprecated public volatile int scheduledID = -1;

    final Semaphore instance;

    @Deprecated
    protected Causable(NAR nar) {
        this(nar, null);
    }

    protected Causable() {
        this((Term)null);
    }

    /** if using this constructor, make sure to call 'nar.on(this);' in the callee */
    protected Causable(Term id) {
        this(null, id);
    }

    private Causable(NAR nar, Term id) {
        super(id);
        can = new Can(term().toString());
        if (nar != null)
            nar.on(this);
        this.nar = nar;
        this.instance = new Semaphore(singleton() ?  1 : Runtime.getRuntime().availableProcessors());
    }

    @Override
    public String toString() {
        return can.toString();
    }

    /**
     * if false, allows multiple threads to execute this instance
     * otherwise it is like being synchronized
     */
    protected boolean singleton() {
        return true;
    }

    /**
     * returns iterations actually completed
     * returns 0 if no work was done, although the time taken will still be recorded
     * if returns -1, then it signals there is no work availble
     * and time will not be recorded. further a scheduler can assume
     * this will remain true for the remainder of the cycle, so it can be
     * removed from the eligible execution list for the current cycle.
     */
    protected abstract void next(NAR n, BooleanSupplier kontinue);

    protected final int nextCounted(NAR n, BooleanSupplier kontinue) {
        final int[] count = {0};
        next(n, ()-> {
            count[0]++;
            return kontinue.getAsBoolean();
        });
        return count[0];
    }

    /**
     * returns a system estimated instantaneous-sampled value of invoking this. between 0..1.0
     */
    public abstract float value();

}
