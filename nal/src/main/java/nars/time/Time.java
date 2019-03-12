package nars.time;

import com.netflix.servo.util.Clock;
import jcog.data.iterator.ArrayIterator;
import jcog.data.list.MetalConcurrentQueue;
import nars.NAR;
import nars.time.event.AtTime;

import javax.measure.Quantity;
import java.io.Serializable;
import java.util.PriorityQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Time state
 */
public abstract class Time implements Clock, Serializable {


    final AtomicLong scheduledNext = new AtomicLong(Long.MIN_VALUE);

    final static int MAX_INCOMING = 4 * 1024;
    final MetalConcurrentQueue<ScheduledTask> incoming =
            new MetalConcurrentQueue<>(MAX_INCOMING);


    final PriorityQueue<ScheduledTask> scheduled = new PriorityQueue<>(MAX_INCOMING /* estimate capacity */);

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

    public final Stream<ScheduledTask> events() {
        ScheduledTask[] s;
        synchronized (scheduled) {
            s = scheduled.toArray(ScheduledTask[]::new);
        }
        return Stream.concat(
            incoming.stream(),
            ArrayIterator.stream(s) //a copy
        );
    }

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


    public final void runAt(long whenOrAfter, Runnable then) {
        runAt(new AtTime(whenOrAfter, then));
    }


    public void runAt(ScheduledTask event) {
//        long w = event.start();
//        assert(w!=ETERNAL && w!=TIMELESS);

        incoming.add(event);


//        scheduledNext.accumulateAndGet(w, Math::min);
    }


    /**
     * drain scheduled tasks ready to be executed
     */
    public void schedule(Consumer<ScheduledTask> each) {


        if (!scheduling.compareAndExchangeAcquire(false, true)) {

            try {
                long now = now();

                {
                    //fire  previously scheduled
                    if (now >= scheduledNext.get()) {
                        ScheduledTask next;

                        synchronized (scheduled) {
                            while (((next = scheduled.peek()) != null) && (next.start() <= now)) {
                                each.accept(scheduled.poll()); //assert (next == actualNext);
                            }
                        }

                        scheduledNext.accumulateAndGet(
                                next != null ? next.start() : Long.MAX_VALUE,
                                Math::min
                        );

                    }
                }
                {
                    //drain incoming queue

                    if (!incoming.isEmpty()) {
                        synchronized (scheduled) {
                            incoming.clear(next -> {
                                if (next.start() <= now)
                                    each.accept(next);
                                else {
                                    scheduled.offer(next);
                                }
                            });
                        }
                    }
                }


            } finally {
                scheduling.setRelease(false);
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
