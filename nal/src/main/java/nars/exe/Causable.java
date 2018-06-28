package nars.exe;

import jcog.exe.Can;
import jcog.pri.NLink;
import nars.NAR;
import nars.control.NARService;
import nars.term.Term;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

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

    private static final Logger logger = LoggerFactory.getLogger(Causable.class);

    public final Can can;

    /** id as set by the scheduler to identify it */
    public volatile int scheduledID = -1;


    @Deprecated
    protected Causable(NAR nar) {
        this(nar, null);
    }

    /** if using this constructor, make sure to call 'nar.on(this);' in the callee */
    protected Causable(Term id) {
        this(null, id);
    }

    protected Causable(NAR nar, Term id) {
        super(id);
        can = new Can(term().toString());
        if (nar != null)
            nar.on(this);
        this.nar = nar;
    }

    @Override
    public String toString() {
        return can.toString();
    }

    /**
     * if false, allows multiple threads to execute this instance
     * otherwise it is like being synchronized
     */
    public boolean singleton() {
        return true;
    }



//    final ThreadLocal<MiniPID> rate = ThreadLocal.withInitial(()->
//        new MiniPID(0.5, 0.3, 0.4)
//                .outLimit(1, Double.MAX_VALUE)
//    );

    @Deprecated public void run(NAR n, int workRequested, Consumer<NLink<Runnable>> buffer) {
        assert(workRequested> 0);
        buffer.accept(new NLink<>(()->{
            next(n, workRequested);
        }, workRequested));
    }



//
//        Throwable error = null;
//
////        MiniPID r = rate.get();
//
////        int iterations = Math.max(1, (int)Math.round(r.setpoint(workRequested).out()));
//
//        long start = System.nanoTime(), end;
//        int workDone = 0;
//
//        try {
//
//            workDone = next(n, iterations);
//
//        } catch (Throwable t) {
//            error = t;
//        } finally {
//            end = System.nanoTime();
//        }
//
//        record(iterations, workDone, start, end/*, r*/);
//
//        if (error != null) {
//            report(error);
//        }
//
//        return workDone;
//    }

//    public void record(int iterations, int workDone, long start, long end/*, MiniPID r*/) {
//        if (workDone >= 0) {
//            can.add((end - start), iterations, workDone);
////            r.out(workDone);
//        }
//
//        Exe.profiled(can, start, end);
//    }

//    public void report(Throwable error) {
//        if (Param.DEBUG) {
//            if (error instanceof RuntimeException) {
//                throw ((RuntimeException) error);
//            } else {
//                throw new RuntimeException(error);
//            }
//        } else
//            logger.error("{} {}", this, error);
//    }


    /**
     * returns iterations actually completed
     * returns 0 if no work was done, although the time taken will still be recorded
     * if returns -1, then it signals there is no work availble
     * and time will not be recorded. further a scheduler can assume
     * this will remain true for the remainder of the cycle, so it can be
     * removed from the eligible execution list for the current cycle.
     */
    protected abstract int next(NAR n, int iterations);

    /**
     * returns a system estimated instantaneous-sampled value of invoking this. between 0..1.0
     */
    public abstract float value();

}
