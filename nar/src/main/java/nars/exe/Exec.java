package nars.exe;

import com.google.common.flogger.FluentLogger;
import jcog.WTF;
import jcog.data.iterator.ArrayIterator;
import jcog.data.list.FasterList;
import jcog.data.list.MetalConcurrentQueue;
import jcog.util.ConsumerX;
import nars.NAR;
import nars.Task;
import nars.attention.What;
import nars.control.NARPart;
import nars.control.op.Perceive;
import nars.control.op.Remember;
import nars.table.dynamic.SeriesBeliefTable;
import nars.task.AbstractTask;
import nars.task.UnevaluatedTask;
import nars.time.ScheduledTask;

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
abstract public class Exec extends NARPart implements Executor, ConsumerX<AbstractTask> {

    public static final FluentLogger logger = FluentLogger.forEnclosingClass();

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

    /** HACK this needs better */
    public static void run(Task t0, What w) {

        Task x = t0;

//        try {
            while (x!=null && !(x instanceof AbstractTask)) {
                Task y;
                if (x instanceof UnevaluatedTask) {
                    if (x instanceof SeriesBeliefTable.SeriesTask) {
                        y = null; //already added directly by the table to itself
                    } else {
                        y = Remember.the(x, w.nar);
                    }
                } else {
                    y = Perceive.perceive(x, w);
                }
                if (y!=null && y.equals(x))
                    throw new WTF(); //HACK
                x = y;
            }

            if (x instanceof AbstractTask) {
                if (x instanceof AbstractTask.TasksArray) {
                    //HACK
                    for (Task tt : ((AbstractTask.TasksArray) x).tasks) {
                        if (tt.equals(t0))
                            throw new WTF();//cycle
                        run(tt, w);
                    }
                } else {
                    do {
                        x = x.next(w);
                    } while (x != null);
                }
            } else if (x != null) {
                throw new WTF("unrecognized task type: " + x.getClass() + '\t' + x);
            }
//        } catch (Throwable e) {
//            logger.atSevere().withCause(e).log(t0.toString());
//        }

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
            logger.atSevere().withCause(e).log(t.toString());
        }
    }
    private void executeNow(Runnable t) {
        try {
            t.run();
        } catch (Throwable e) {
            logger.atSevere().withCause(e).log(t.toString());
        }
    }

    /**
     * immediately execute a Task
     */
    @Override
    public final void accept(AbstractTask x) {
        executeNow(x, nar);
    }

    private static void executeNow(/*Abstract*/Task x, NAR nar) {
        Task t = x;
        try {
            Task.run(t, nar);
        } catch (Throwable e) {
            logger.atSevere().withCause(e).log(t.toString());
        }
    }

    /**
     * inline, synchronous
     */
    protected final void executeNow(Object t) {
        if (t instanceof AbstractTask)
            accept((AbstractTask) t);
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
        this.nar = n; //HACK
        schedule(x -> x.accept(n));
    }
}
