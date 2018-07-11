package nars.time;

import com.netflix.servo.util.Clock;
import jcog.list.MetalConcurrentQueue;
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

    public void runAt(long whenOrAfter, Consumer<NAR> then) {
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
    public void scheduled(Consumer<SchedTask> each) {


        long now = now();
        long nextScheduled = scheduledNext.get();
        if ((now < nextScheduled) || !scheduledNext.compareAndSet(nextScheduled, Long.MAX_VALUE))
            return;


        SchedTask next;
        while (((next = scheduled.peek()) != null) && (next.when <= now)) {
            SchedTask actualNext = scheduled.poll();
            each.accept(actualNext);
            //assert (next == actualNext);
        }

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

        long nextNextWhen = next != null ? next.when : Long.MAX_VALUE;
        scheduledNext.updateAndGet(z -> Math.min(z, nextNextWhen));


    }

    abstract public void cycle(NAR n);


    /**
     * flushes the pending work queued for the current time
     */
    public void synch(NAR n) {
        scheduled(n::input);
    }


    /**
     * returns a string containing the time elapsed/to the given time
     */
    public String durationToString(long target) {
        long now = now();
        return durationString(now - target);
    }

    protected abstract String durationString(long time);

    public long toCycles(Quantity q) {
        throw new UnsupportedOperationException("Only in RealTime implementations");
    }
}
