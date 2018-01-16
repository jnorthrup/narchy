package jcog.exe;

import jcog.data.LongCounter;
import jcog.util.Flip;
import org.eclipse.collections.api.block.procedure.primitive.LongLongProcedure;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * potentially executable procedure of some value N >=1 iterations per invocation.
 * represents a functional skill or ability the system is able to perform, particularly
 * once it has learned how, why, and when to invoke it.
 */
public class Can extends Flip<Can.WorkPerformance> {

    private final static AtomicInteger serial = new AtomicInteger();


    public static class WorkPerformance {
        /** time spent, in nanoseconds */
        public final LongCounter time = new LongCounter();

        /** iterations completed (in the time recorded) */
        public final LongCounter done = new LongCounter();

        public void add(long timeInc, long done) {
            time.add(timeInc);
            this.done.add(done);
        }

        public void commit(LongLongProcedure take) {
            long t = time.getThenZero();
            long s = done.getThenZero();
            take.value(t, s);
        }

    }


    public final String id;

    public Can() {
        this(String.valueOf(serial.incrementAndGet()));
    }

    public Can(String id) {
        super(WorkPerformance::new);
        this.id = id;
    }


    /** receives pair of long's: time (ns) and iterations completed in that time */
    public final void commit(LongLongProcedure take) {
        commit().commit(take);
    }


    /**
     * totalTime in sec
     */
    public void add(long totalTimeNS, int done) {
        write().add(totalTimeNS, done);
    }

    @Override
    public String toString() {
        return id;
    }

}
