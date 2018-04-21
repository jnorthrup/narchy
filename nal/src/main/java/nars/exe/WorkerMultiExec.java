package nars.exe;

import com.conversantmedia.util.concurrent.ConcurrentQueue;
import com.conversantmedia.util.concurrent.MultithreadConcurrentQueue;
import jcog.Util;
import jcog.exe.BusyPool;
import jcog.math.MutableInteger;
import jcog.math.random.SplitMix64Random;
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
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.function.LongPredicate;
import java.util.stream.Stream;

/**
 * instantiates a fixed set of worker threads
 */
public class WorkerMultiExec extends AbstractExec {

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


    public WorkerMultiExec(Revaluator revaluator, int conceptCapacity, int qSize) {
        this(revaluator, Util.defaultConcurrency(), conceptCapacity, qSize);
    }

    public WorkerMultiExec(Revaluator r, int threads, int conceptCapacity, int qSize) {
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
    public final void execute(Object t) {

        if (t instanceof Task || isWorker()) {
            executeNow(t);
        } else {
            exe.accept(t);
        }

    }

    @Override
    public void execute(Stream<? extends ITask> input) {
        input.forEach( x -> x.run(nar) );
    }

    @Override
    public void execute(Iterator<? extends ITask> input) {
        input.forEachRemaining( x -> x.run(nar) );
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
                    return new MyWorkLoop(q);
                }

                @Override
                protected void queueOverflow(Object x) {

                    //help clear the queue
                    int qSize = q.size();
                    Object next;
                    while (((next = q.poll())!=null) && qSize-- > 0) {
                        executeNow(next);
                        if (q.offer(x))
                            return; //ok
                    }
                    if (!q.offer(x)) {
                        Thread.yield();
                        //emergency defer to ForkJoin commonPool
                        ForkJoinPool.commonPool().execute(x instanceof Runnable ? ((Runnable)x) : ()->executeNow(x));
                    }

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

        /**
         * TODO use non-atomic version of this, slightly faster
         */
        final Random rng;

        public MyWorkLoop(ConcurrentQueue q) {
            super(q);

            rng = //new XoRoShiRo128PlusRandom(System.nanoTime());
                    new SplitMix64Random(System.nanoTime());
        }

        @Override
        public void run() {

            focus.decide(rng, x -> {

                Object next;
                while ((next = pollNext())!=null) {
                    executeNow(next);
                }


                //TODO throttling
//            long next = nar.time();
//            if (next != now) {
//                now = next;
//                long throttleNS = nar.loop.throttleNS();
//                if (throttleNS > 0) {
//                    Util.sleepNS(throttleNS);
//                }
//            }

                focus.tryRun(x);


//                if (done == 0 && idles++ > 0)
//                    Util.pauseNext(idles);

                return true;
            });

        }
    }
}
