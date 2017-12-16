package nars.time;

import com.conversantmedia.util.concurrent.DisruptorBlockingQueue;
import com.netflix.servo.util.Clock;
import nars.NAR;
import nars.task.NativeTask.SchedTask;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Time state
 */
public abstract class Time implements Clock, Serializable {

//    //TODO write this to replace LongObjectPair and cache the object's hash
//    public static class Scheduled extends LinkedList<Runnable> implements Comparable<Scheduled> {
//        long when;
//        public Scheduled(Runnable r) {
//            super();
//            super.add(r);
//        }
//
//        @Override
//        public boolean add(Runnable runnable) {
//            synchronized (getFirst()) {
//                return super.add(runnable);
//            }
//        }
//
//    }


    final AtomicLong scheduledNext = new AtomicLong(Long.MIN_VALUE);
    final static int MAX_QUEUED = 4 * 1024;
    final BlockingQueue<SchedTask> pendingSched =
            new DisruptorBlockingQueue<>(MAX_QUEUED);
    //new ArrayBlockingQueue<>(MAX_QUEUED);
    //final ConcurrentQueue<SchedTask> pendingSched =
    //new MultithreadConcurrentQueue(MAX_QUEUED);

    final PriorityQueue<SchedTask> scheduled =
            //final MinMaxPriorityQueue<SchedTask> scheduled =
            //MinMaxPriorityQueue.orderedBy((SchedTask a, SchedTask b) -> {
            new PriorityQueue<>();


    public void clear(NAR n) {
        synchronized(scheduled) {
            synch(n);
            pendingSched.clear();
            scheduled.clear();
        }
    }

    /**
     * called when memory reset
     */
    public abstract void reset();


    /**
     * time elapsed since last cycle
     */
    public abstract long sinceLast();

    /**
     * returns a new stamp evidence id
     */
    public abstract long nextStamp();


    /**
     * the default duration applied to input tasks that do not specify one
     * >0
     */
    public abstract int dur();

    /**
     * set the duration, return this
     *
     * @param d, d>0
     */
    public abstract Time dur(int d);


    public void at(long whenOrAfter, Runnable then) {
        at(new SchedTask(whenOrAfter, then));
    }

    public void at(long whenOrAfter, Consumer<NAR> then) {
        at(new SchedTask(whenOrAfter, then));
    }

    private final void at(SchedTask event) {
        pendingSched.add(event);
        long w = event.when;
        scheduledNext.updateAndGet((z) -> Math.min(z, w));
    }


    @Nullable
    public List<SchedTask> exeScheduled() {

        //        now = now();
//        SchedTask firstQuick = scheduled.peek(); //it's safe to call this outside synchronized block for speed
//        if (firstQuick == null || firstQuick.when > now)
//            return null; //too soon for the next one


        long nextScheduled = scheduledNext.get();
        if ((now() < nextScheduled) || !(scheduledNext.compareAndSet(nextScheduled, Long.MAX_VALUE)))
            return null;


        try  {

            pendingSched.drainTo(scheduled);


            List<SchedTask> pending = new LinkedList();

            SchedTask next;
            while (((next = scheduled.peek()) != null) && (next.when <= now())) {
                SchedTask actualNext = scheduled.poll();
                assert (next == actualNext);
                pending.add(next);
            }

            long nextNextWhen = next!=null ? next.when : Long.MAX_VALUE;
            scheduledNext.updateAndGet(z -> Math.min(z, nextNextWhen ));
            return pending;

        } catch (Throwable t) {
            t.printStackTrace();
            scheduledNext.set(now()); //try again immediately
            return null;
        }


    }

    public void cycle(NAR n) {
        synch(n);
    }


    /**
     * flushes the pending work queued for the current time
     */
    public void synch(NAR n) {
        n.input(exeScheduled());
    }

    public long[] nextInputStamp() {
        return new long[]{nextStamp()};
    }


}
