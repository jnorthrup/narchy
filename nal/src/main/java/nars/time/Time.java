package nars.time;

import com.netflix.servo.util.Clock;
import jcog.data.list.MetalConcurrentQueue;
import nars.NAR;
import nars.task.AbstractTask.SchedTask;

import javax.measure.Quantity;
import java.io.Serializable;
import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicBoolean;
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

    /**
     * busy mutex
     */
    private final AtomicBoolean scheduling = new AtomicBoolean(false);


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

        scheduledNext.accumulateAndGet(
                event.when,
                Math::min
        );
    }


    /**
     * drain scheduled tasks ready to be executed
     */
    public void schedule(Consumer<SchedTask> each) {


        if (scheduling.compareAndSet(false, true)) {

            try {
                long now = now();
                SchedTask p;
                for (; (p = incoming.poll()) != null; ) {
                    if (p.when <= now)
                        each.accept(p);
                    else
                        scheduled.offer(p);
                }

                //..

                now = now(); //udpate the time
                if (now >= scheduledNext.getOpaque()) {
                    SchedTask next;

                    while (((next = scheduled.peek()) != null) && (next.when <= now)) {
                        each.accept(scheduled.poll()); //assert (next == actualNext);
                    }

                    scheduledNext.accumulateAndGet(
                            next != null ? next.when : Long.MAX_VALUE,
                            Math::min
                    );

                }
            } finally {
                scheduling.set(false);
            }
        }


    }

    abstract public void cycle(NAR n);


    /**
     * flushes the pending work queued for the current time
     */
    public final void synch(NAR n) {
        schedule(n.exe::execute);
    }


//    /**
//     * returns a string containing the time elapsed/to the given time
//     */
//    public String durationToString(long target) {
//        long now = now();
//        return durationString(now - target);
//    }

    /**
     * produces a string representative of the amount of time (cycles, not durs)
     */
    public abstract String timeString(long time);

    public long toCycles(Quantity q) {
        throw new UnsupportedOperationException("Only in RealTime implementations");
    }
}
