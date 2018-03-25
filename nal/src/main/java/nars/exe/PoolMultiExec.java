package nars.exe;

import com.conversantmedia.util.concurrent.ConcurrentQueue;
import com.conversantmedia.util.concurrent.MultithreadConcurrentQueue;
import jcog.Util;
import jcog.exe.BusyPool;
import jcog.math.MutableInteger;
import jcog.math.random.XoRoShiRo128PlusRandom;
import nars.$;
import nars.NAR;
import nars.Task;
import nars.task.ITask;
import org.eclipse.collections.api.set.primitive.LongSet;
import org.eclipse.collections.impl.factory.primitive.LongSets;
import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;

import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.LongPredicate;
import java.util.stream.Stream;

/**
 * uses a common forkjoin pool for execution
 */
public class PoolMultiExec extends AbstractExec {

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


    public Focus focus;
    private Consumer exe;


    public PoolMultiExec(Revaluator revaluator, int conceptCapacity, int qSize) {
        this(revaluator,
                Math.max(1, Runtime.getRuntime().availableProcessors() - 1), conceptCapacity, qSize);
    }

    public PoolMultiExec(Revaluator r, int threads, int conceptCapacity, int qSize) {
        super(conceptCapacity);
        this.revaluator = r;
        this.threads.set(threads);
        this.qSize = qSize;
        this.exe = this::executeNow;
    }

    @Override
    public final void execute(Consumer<NAR> r) {
        execute((Object) r);
    }

    @Override
    public final void execute(Runnable r) {
        execute((Object) r);
    }


    @Override
    public void execute(Object t) {

        if (t instanceof Task || isWorker()) {
            executeNow(t);
        } else {
            exe.accept(t);
        }

    }

    private void executeLater(ITask x) {
        if (x instanceof Task)
            executeNow(x);
        else
            exe.accept(x);
    }

    @Override
    public void execute(Stream<? extends ITask> input) {
        input.forEach(isWorker() ?
                this::executeNow : this::executeLater);
    }

    @Override
    public void execute(Iterator<? extends ITask> input) {
        input.forEachRemaining(isWorker() ?
                this::executeNow : this::executeLater);
    }

    private boolean isWorker() {
        return isWorker(Thread.currentThread());
    }

    private boolean isWorker(Thread t) {
        return isActiveThreadId.test(t.getId());
    }


    /**
     * to be called in initWorkers() impl for each thread constructed
     */
    private void register(Thread t) {
        synchronized (this) {
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
    }

    @Override
    protected void clear() {
        synchronized (this) {
            super.clear();
            activeThreads.forEach(Thread::interrupt);
            activeThreads.clear();
            activeThreadIds = new LongHashSet();
            isActiveThreadId = (x) -> false;
        }
    }


    @Override
    public void start(NAR nar) {
        synchronized (this) {
            this.focus = new Focus(nar, revaluator);

            super.start(nar);

            this.pool = new BusyPool(threads.intValue(),
                    new MultithreadConcurrentQueue(qSize) //(disruptor) fastest but unsafe (overflow trainwrecks)
                    //Util.blockingQueue(qSize) //(disruptor) fast, and safe (overflows gracefully, with warning too)
            ) {
                @Override
                protected WorkLoop newWorkLoop(ConcurrentQueue<Runnable> q) {
                    return new MyWorkLoop(q, nar);
                }
            };
            pool.workers.forEach(this::register);
            this.exe = pool::queue;
        }
    }

    @Override
    public void stop() {
        synchronized (this) {
            exe = this::executeNow;
            super.stop();
            pool.shutdownNow();
            pool = null;
            focus = null;
        }
    }

    @Override
    public final boolean concurrent() {
        return true;
    }


    private class MyWorkLoop extends BusyPool.WorkLoop {

        /** dummy Causable each worker schedules at the 0th position of the process table,
         * in which a worker will attempt to drain some or all of the queued work before returning to playing */

        /**
         * TODO use non-atomic version of this, slightly faster
         */
        final Random rng;
        private final NAR nar;

        public MyWorkLoop(ConcurrentQueue q, NAR nar) {
            super(q);
            this.nar = nar;

            rng = new XoRoShiRo128PlusRandom(System.nanoTime());
        }

        @Override
        protected void run(Object next) {
            executeNow(next);
        }

        int idles = 0;

        protected void idle() {
            int done = 0;
            while (pollNext()) {
                done++;
                this.idles = 0;
            }

            if (done == 0)
                Util.pauseNext(idles++);
        }

        @Override
        public void run() {

            final long[] now = {nar.time()};
            focus.decide(rng, (Causable cx) -> {

                long next = nar.time();
                if (next != now[0]) {
                    now[0] = next;

                    long throttleNS = nar.loop.throttleNS();
                    if (throttleNS > 0) {
                        Util.sleepNS(throttleNS);
                        return true; //re-loop
                    }

                }

                int x;
                if (cx == null || (x = cx.id) <= 0) {
                    idle();
                    return true;
                }

                int sliceIters[] = focus.sliceIters;
                if (sliceIters.length <= x)
                    return true;

                /** temporarily withold priority */

                boolean singleton = cx.singleton();
                int pri;
                if (singleton) {
                    if (!cx.busy.compareAndSet(false, true))
                        return true; //someone else got this singleton

                    pri = focus.priGetAndSet(x, 0);
                } else {
                    pri = focus.pri(x);
                }

                //TODO this growth limit value should decrease throughout the cycle as each execution accumulates the total work it is being compared to
                //this will require doneMax to be an atomic accmulator for accurac


                int itersNext = sliceIters[x];

                //System.out.println(cx + " x " + iters + " @ " + n4(iterPerSecond[x]) + "iter/sec in " + Texts.timeStr(subTime*1E9));

                idles = 0; //reset idle count, get ready to actually do something

                int completed = -1;
                try {
                    completed = cx.run(nar, itersNext);
                } finally {
                    if (singleton) {

                        cx.busy.set(false);

                        if (completed >= 0) {
                            focus.priGetAndSetIfEquals(x, 0, pri); //release for another usage unless it's already re-activated in a new cycle
                        } else {
                            //leave suspended until next commit in the next cycle
                        }

                    } else {
                        if (completed < 0) {
                            focus.priGetAndSet(x, 0); //suspend
                        }
                    }
                }

                return true;
            });

        }


//                        protected long next() {
//
//                            int loopMS = nar.loop.periodMS.intValue();
//                            if (loopMS < 0) {
//                                loopMS = IDLE_PERIOD_MS;
//                            }
//                            long dutyMS =
//                                    Math.round(nar.loop.throttle.floatValue() * loopMS);
//
//                            //if (rng.nextInt(100) == 0)
//                            //    System.out.println(this + " " + Texts.timeStr(timeSinceLastBusyNS) + " since busy, " + Texts.timeStr(dutyMS*1E6) + " loop time" );
//
//                            if (dutyMS > 0) {
//                                return Math.round(nar.loop.jiffy.doubleValue() * dutyMS * 1E6);
//                            } else {
//                                return 0; //empty batch
//                            }
//
//                        }


    }
}
