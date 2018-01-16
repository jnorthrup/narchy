package nars.exe;

import jcog.Util;
import jcog.exe.BusyPool;
import jcog.math.MutableInteger;
import jcog.math.random.XoRoShiRo128PlusRandom;
import nars.$;
import nars.NAR;
import nars.Task;
import nars.task.ITask;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.eclipse.collections.api.set.primitive.LongSet;
import org.eclipse.collections.impl.factory.primitive.LongSets;
import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;

import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.LongPredicate;
import java.util.stream.Stream;

/**
 * uses a common forkjoin pool for execution
 */
public class PoolMultiExec extends AbstractExec {

    static final int IDLE_PERIOD_MS = 50;

    private final int qSize;
    BusyPool pool;

    private final Revaluator revaluator;

    /**
     * TODO make this adjust in realtime
     */
    private final MutableInteger threads = new MutableInteger();
    final List<Thread> activeThreads = $.newArrayList();
    LongSet activeThreadIds = new LongHashSet();
    LongPredicate isActiveThreadId = (x) -> false;


    private Focus focus;
    private Consumer exe;


    public PoolMultiExec(Revaluator revaluator, int capacity, int qSize) {
        this(revaluator, Runtime.getRuntime().availableProcessors() - 1, capacity, qSize);
    }

    protected PoolMultiExec(Revaluator r, int threads, int capacity, int qSize) {
        super(capacity);
        this.revaluator = r;
        this.threads.set(threads);
        this.qSize = qSize;
        this.exe = this::executeInline;
    }

    @Override
    public void execute(Consumer<NAR> r) {
        execute((Object) r);
    }

    @Override
    public void execute(Runnable r) {
        execute((Object) r);
    }


    @Override
    public void execute(Object t) {

        if (t instanceof Task || isWorker(Thread.currentThread())) {
            executeInline(t);
        } else {
            exe.accept(t);
        }

    }

    @Override
    protected synchronized void clear() {
        super.clear();
        activeThreads.forEach(Thread::interrupt);
        activeThreads.clear();
        activeThreadIds = new LongHashSet();
        isActiveThreadId = (x) -> false;
    }


    final Consumer immediate = this::executeInline;

    final Consumer deferred = x -> {
        if (x instanceof Task)
            executeInline(x);
        else
            exe.accept(x);
    };

    /**
     * the input procedure according to the current thread
     */
    protected Consumer add() {
        return isWorker(Thread.currentThread()) ? immediate : deferred;
    }

    @Override
    public void execute(Stream<? extends ITask> input) {
        input.forEach(add());
    }

    @Override
    public void execute(Iterator<? extends ITask> input) {
        input.forEachRemaining(add());
    }






    protected boolean isWorker(Thread t) {
        return isActiveThreadId.test(t.getId());
    }


    /**
     * to be called in initWorkers() impl for each thread constructed
     */
    protected synchronized void register(Thread t) {
        activeThreads.add(t);
        activeThreadIds = LongSets.mutable.ofAll(activeThreadIds).with(t.getId()).toImmutable();
        long max = activeThreadIds.max();
        long min = activeThreadIds.min();
        if (max - min == activeThreadIds.size() - 1) {
            //contiguous id's, use fast id tester
            isActiveThreadId = (x) -> x >= min && x <= max;
        } else {
            isActiveThreadId = activeThreadIds::contains;
        }
    }

    @Override
    public synchronized void start(NAR nar) {

        this.focus = new Focus(nar, revaluator);

        super.start(nar);

        this.pool = new BusyPool(threads.intValue(),
                Util.blockingQueue(qSize)
                //new ArrayBlockingQueue(qSize)
        ) {

            @Override
            protected BusyPool.Worker newWorker(Queue<Runnable> q) {
                return new Worker(q) {

                    final Random rng = new XoRoShiRo128PlusRandom(System.nanoTime());

                    protected double next() {

                        int loopMS = nar.loop.periodMS.intValue();
                        if (loopMS < 0) {
                            loopMS = IDLE_PERIOD_MS;
                        }
                        long dutyMS =
                                Math.round(nar.loop.throttle.floatValue() * loopMS);

                        //if (rng.nextInt(100) == 0)
                        //    System.out.println(this + " " + Texts.timeStr(timeSinceLastBusyNS) + " since busy, " + Texts.timeStr(dutyMS*1E6) + " loop time" );

                        if (dutyMS > 0) {

                            double t = nar.loop.jiffy.doubleValue() *
                                    dutyMS / 1000.0;
                            //Math.min(dutyMS / 1000.0, timeSinceLastBusyNS / 1.0E9);

                            //runUntil = System.currentTimeMillis() + dutyMS;

                            return t;
                        } else {
                            return 0; //empty batch
                        }

                    }

                    protected boolean kontinue() {
                        //drain();
                        pollWhileNotEmpty();

                        return true;
                        //q.size() < qSize/2;

                        //&& System.currentTimeMillis() <= runUntil;
                    }

                    @Override
                    protected void run(Object next) {
                        executeInline(next);
                    }

                    @Override
                    public void run() {

                        focus.runDeadline(
                                this::next,
                                this::kontinue,
                                rng, nar);

                    }
                };
            }
        };
        pool.workers.forEach(this::register);
        this.exe = pool::queue;
    }

    @Override
    public synchronized void stop() {
        exe = this::executeInline;
        super.stop();
        pool.shutdownNow();
        pool = null;
        focus = null;

    }

    @Override
    public boolean concurrent() {
        return true;
    }


}
