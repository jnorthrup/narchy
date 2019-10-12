package nars.exe;

import jcog.Log;
import jcog.data.iterator.ArrayIterator;
import nars.NAR;
import nars.control.NARPart;
import nars.derive.Deriver;
import nars.time.ScheduledTask;
import org.jctools.queues.MpscArrayQueue;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.PriorityQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * manages low level task scheduling and execution
 */
abstract public class Exec extends NARPart implements Executor {

    public static final Logger logger = Log.logger(Exec.class);

    private final static int TIME_QUEUE_CAPACITY = 2 * 1024;
    final MpscArrayQueue<ScheduledTask> toSchedule = new MpscArrayQueue<>(TIME_QUEUE_CAPACITY);
    final PriorityQueue<ScheduledTask> scheduled = new PriorityQueue<>(TIME_QUEUE_CAPACITY /* estimate capacity */);

    /**
     * busy mutex
     */
    private final AtomicBoolean busy = new AtomicBoolean(false);

    /**
     * maximum possible concurrency; should remain constant
     */
    public final int concurrencyMax;

    protected Exec(int concurrencyMax) {
        this.concurrencyMax = concurrencyMax; //TODO this will be a value like Runtime.getRuntime().availableProcessors() when concurrency can be adjusted dynamically

    }

    protected Deriver deriver = null;

    /** sets the deriver */
    public void deriver(Deriver deriver) {

        Deriver prev = this.deriver;
        if (prev!=null) {
            //prev.stop
        }
        this.deriver = deriver;

    }



//    private static void taskError(Prioritizable t, Prioritizable x, Throwable ee, NAR nar) {
//        //TODO: if(RELEASE)
////        if (t == x)
////            nar.logger.error("{} {}", x, ee);
////        else
////            nar.logger.error("{}->{} {}", t, x, ee);
////
////        if (Param.DEBUG)
//            throw new RuntimeException(ee);
//    }

    /** execute later */
    @Override public void execute(Runnable async) {
        if (concurrent()) {
            ForkJoinPool.commonPool().execute(async);
        } else {
            async.run();
        }
    }

    public void input(Consumer<NAR> r) {
        if (concurrent()) {
            ForkJoinPool.commonPool().execute(() -> r.accept(nar));
        } else {
            r.accept(nar);
        }
    }

    /**
     * true if this executioner executes procedures concurrently.
     * in subclasses, if this is true but concurrency()==1, it will use
     * concurrent data structures to be safe.
     */
    public boolean concurrent() {
        return concurrencyMax > 1;
    }

    /**
     * current concurrency level; may change
     */
    //@Override
    abstract public int concurrency();

    protected final void executeNow(Consumer<NAR> t) {
        try {
            t.accept(nar);
        } catch (Throwable e) {
            logger.warn("{} {}", e, t);
        }
    }
    private void executeNow(Runnable t) {
        try {
            t.run();
        } catch (Throwable e) {
            logger.warn("{} {}", e, t);
        }
    }



    /**
     * inline, synchronous
     */
    protected final void executeNow(Object t) {
        if (t instanceof Consumer)
            executeNow((Consumer)t);
        else
            executeNow((Runnable) t);
    }

    abstract protected void next();

    public void print(Appendable out) {
        try {
            out.append(this.toString());
            out.append('\n');
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public void synch() {

    }


    public void clear(NAR n) {
        synchronized (scheduled) {
            synch(n);
            toSchedule.clear();
            scheduled.clear();
        }
    }



    public final Stream<ScheduledTask> events() {
        ScheduledTask[] s;
        synchronized (scheduled) {
            s = scheduled.toArray(ScheduledTask[]::new);
        }
        return //Stream.concat(
                //toSchedule.stream(),
                ArrayIterator.stream(s) //a copy
        //)
        ;
    }


    public final void runAt(ScheduledTask event) {
        toSchedule.add(event);
//        long w = event.start();
//        scheduledNext.accumulateAndGet(w, Math::min);
    }


    /**
     * drain scheduled tasks ready to be executed
     */
    public void schedule(Consumer<ScheduledTask> each) {

        if (!busy.compareAndSet(false, true))
            return;

        try {

            long now0 = nar.time();
            toSchedule.drain(x -> {
                if (x.scheduled)
                    return; //ignore

                if (x.start() <= now0)
                    each.accept(x);
                else {
                    x.scheduled = true;
                    if (!scheduled.offer(x)) {
                        x.scheduled = false;
                        throw new RuntimeException("scheduled priority queue overflow");
                    }
                }
            });

            long now = nar.time();
            ScheduledTask t;
            while (((t = scheduled.peek()) != null) && (t.start() <= now)) {
                ScheduledTask s = scheduled.poll();
                s.scheduled = false;
                each.accept(s); //assert (next == actualNext);
            }




        } finally {
            busy.set(false);
        }


    }




    /**
     * flushes the pending work queued for the current time
     */
    public final void synch(NAR n) {
        schedule(this::executeNow);
    }

    public void throttle(float t) {
        nar.loop.throttle.set(t);
    }
}
