package jcog.exe;

import jcog.meter.event.AtomicLongGuage;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * potentially executable procedure of some value N >=1 iterations per invocation.
 * represents a functional skill or ability the system is able to perform, particularly
 * once it has learned how, why, and when to invoke it.
 *
 * accumulates, in nanoseconds (long) the time spent, and the # of work items (int)
 */
public class Can extends AtomicLongGuage {

    private final static AtomicInteger serial = new AtomicInteger();


//    /** TODO atomically update the pair the two numbers (sum and count).  this isnt 100% synch'd */
//    public static class WorkPerformance {
//        /** time spent, in nanoseconds */
//        public final LongCounter time = new LongCounter();
//
//        /** iterations completed (in the time recorded) */
//        public final LongCounter done = new LongCounter();
//
//        public void add(long timeInc, long done) {
//            time.add(timeInc);
//            this.done.add(done);
//        }
//
//        public void commit(LongLongProcedure take) {
//            long t = time.getThenZero();
//            long s = done.getThenZero();
//            take.value(t, s);
//        }
//
//    }


    public final String id;

    public Can() {
        this(String.valueOf(serial.incrementAndGet()));
    }

    public Can(String id) {
        super();
        this.id = id;
    }

    @Override
    public String toString() {
        return id;
    }

}
