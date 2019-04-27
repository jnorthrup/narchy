package nars.exe;

import jcog.Log;
import jcog.data.iterator.ArrayIterator;
import jcog.data.list.FasterList;
import jcog.data.list.MetalConcurrentQueue;
import jcog.pri.Prioritizable;
import nars.NAR;
import nars.control.NARPart;
import nars.control.channel.ConsumerX;
import nars.task.ITask;
import nars.time.ScheduledTask;
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
abstract public class Exec extends NARPart implements Executor, ConsumerX<ITask> {

    public static final Logger logger = Log.logger(Exec.class);

    private final static int TIME_QUEUE_CAPACITY = 2 * 1024;
    final MetalConcurrentQueue<ScheduledTask> incoming = new MetalConcurrentQueue<>(TIME_QUEUE_CAPACITY);
    final FasterList<ScheduledTask> intermediate = new FasterList<>(TIME_QUEUE_CAPACITY);
    final PriorityQueue<ScheduledTask> scheduled = new PriorityQueue<>(TIME_QUEUE_CAPACITY /* estimate capacity */);

    /**
     * busy mutex
     */
    private final AtomicBoolean busy = new AtomicBoolean(false);


    private final int concurrencyMax;

    protected Exec(int concurrencyMax) {
        this.concurrencyMax = concurrencyMax; //TODO this will be a value like Runtime.getRuntime().availableProcessors() when concurrency can be adjusted dynamically
    }

    abstract public void input(Object t);

    private static void taskError(Prioritizable t, Prioritizable x, Throwable ee, NAR nar) {
        //TODO: if(RELEASE)
//        if (t == x)
//            nar.logger.error("{} {}", x, ee);
//        else
//            nar.logger.error("{}->{} {}", t, x, ee);
//
//        if (Param.DEBUG)
            throw new RuntimeException(ee);
    }

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
    @Override
    abstract public int concurrency();

    /**
     * maximum possible concurrency; should remain constant
     */
    protected final int concurrencyMax() {
        return concurrencyMax;
    }

    protected final void executeNow(Consumer<NAR> t) {
        try {
            t.accept(nar);
        } catch (Throwable e) {
            logger.error("{} {}", t, e);
        }
    }
    private final void executeNow(Runnable t) {
        try {
            t.run();
        } catch (Throwable e) {
            logger.error("{} {}", t, e);
        }
    }

    /**
     * immediately execute a Task
     */
    @Override
    public final void accept(ITask x) {
        executeNow(x, nar);
    }

    private static void executeNow(ITask x, NAR nar) {
        ITask t = x;
        try {
            ITask.run(t, nar);
        } catch (Throwable e) {
            logger.error("{} {}", t, e);
        }
    }

    /**
     * inline, synchronous
     */
    protected final void executeNow(Object t) {
        if (t instanceof ITask)
            accept((ITask) t);
        else if (t instanceof Consumer)
            executeNow((Consumer)t);
        else
            executeNow((Runnable) t);
    }

    abstract protected void next(NAR nar);

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
            incoming.clear();
            scheduled.clear();
        }
    }



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


    public final void runAt(ScheduledTask event) {
        incoming.add(event);
//        long w = event.start();
//        scheduledNext.accumulateAndGet(w, Math::min);
    }


    /**
     * drain scheduled tasks ready to be executed
     */
    public void schedule(Consumer<ScheduledTask> each) {

        if (busy.compareAndSet(false, true)) {

            try {
                long now = nar.time();

                {
                    //fire previously scheduled
                    ScheduledTask next;

                    while (((next = scheduled.peek()) != null) && (next.start() <= now)) {
                        each.accept(scheduled.poll()); //assert (next == actualNext);
                    }


                }
                {
                    //drain incoming queue
                    incoming.clear(next -> {
                        if (next.start() <= now)
                            each.accept(next);
                        else {
                            scheduled.offer(next);
                        }
                    });


                }


            } finally {
                busy.set(false);
            }
        }


    }




    /**
     * flushes the pending work queued for the current time
     */
    public final void synch(NAR n) {
        schedule(x -> x.accept(n));
    }
}
