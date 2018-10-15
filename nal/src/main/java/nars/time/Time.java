package nars.time;

import com.netflix.servo.util.Clock;
import jcog.data.list.MetalConcurrentQueue;
import nars.NAR;
import nars.task.AbstractTask.SchedTask;
import org.jetbrains.annotations.Nullable;

import javax.measure.Quantity;
import java.io.Serializable;
import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Time state
 */
public abstract class Time implements Clock, Serializable {


    final AtomicLong scheduledNext = new AtomicLong(Long.MIN_VALUE);

    final static int MAX_INCOMING = 4 * 1024;
    final MetalConcurrentQueue<SchedTask> incoming =
            new MetalConcurrentQueue<>(MAX_INCOMING);


    final PriorityQueue<SchedTask> scheduled = new PriorityQueue<>(MAX_INCOMING /* estimate capacity */);


    public void clear(NAR n) {
        synchronized (scheduled) {
            synch(n);
            incoming.clear();
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


    public void runAt(long whenOrAfter, Runnable then) {
        runAt(new SchedTask(whenOrAfter, then));
    }



    private void runAt(SchedTask event) {
        if (!incoming.offer(event)) {
            throw new RuntimeException(this + " overflow");
        }

        scheduledNext.updateAndGet(z -> Math.min(z, event.when));
    }


    /**
     * drain scheduled tasks ready to be executed
     */
    @Nullable
    public void schedule(Consumer<SchedTask> each) {

        long now = now();
        int s = incoming.size();
        if (s > 0) {
            //SchedTask p;
            //while ((p = preSched.poll()) != null && s-- > 0) {
            for (int i = 0; i < s; i++) {
                SchedTask p = incoming.poll();
                if (p.when <= now)
                    each.accept(p);
                else
                    scheduled.offer(p);
            }
        }

        long nextScheduled = scheduledNext.getOpaque();
        if ((now >= nextScheduled) && scheduledNext.compareAndSet(nextScheduled, Long.MAX_VALUE)) {
            SchedTask next;
            while (((next = scheduled.peek()) != null) && (next.when <= now)) {
                SchedTask actualNext = scheduled.poll();
                each.accept(actualNext);
                //assert (next == actualNext);
            }
            long nextNextWhen = next != null ? next.when : Long.MAX_VALUE;
            scheduledNext.updateAndGet(z -> Math.min(z, nextNextWhen));
        }





    }

    abstract public void cycle(NAR n);


    /**
     * flushes the pending work queued for the current time
     */
    public synchronized void synch(NAR n) {
        schedule(n.exe::execute);
    }


//    /**
//     * returns a string containing the time elapsed/to the given time
//     */
//    public String durationToString(long target) {
//        long now = now();
//        return durationString(now - target);
//    }

    /** produces a string representative of the amount of time (cycles, not durs) */
    public abstract String timeString(long time);

    public long toCycles(Quantity q) {
        throw new UnsupportedOperationException("Only in RealTime implementations");
    }
}
