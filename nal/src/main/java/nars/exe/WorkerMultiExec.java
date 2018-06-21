//package nars.exe;
//
//import com.conversantmedia.util.concurrent.ConcurrentQueue;
//import com.conversantmedia.util.concurrent.MultithreadConcurrentQueue;
//import jcog.Util;
//import jcog.exe.BusyPool;
//import jcog.math.MutableInteger;
//import jcog.math.random.SplitMix64Random;
//import nars.$;
//import nars.NAR;
//import nars.Task;
//import nars.task.ITask;
//import org.eclipse.collections.api.set.primitive.LongSet;
//import org.eclipse.collections.impl.factory.primitive.LongSets;
//import org.eclipse.collections.impl.set.mutable.primitive.LongHashSet;
//
//import java.util.Iterator;
//import java.util.List;
//import java.util.Random;
//import java.util.concurrent.ForkJoinPool;
//import java.util.function.Consumer;
//import java.util.function.LongPredicate;
//import java.util.stream.Stream;
//
///**
// * instantiates a fixed set of worker threads
// */
//public class WorkerMultiExec extends AbstractExec {
//
//    private final int qSize;
//    BusyPool pool;
//
//    private final Revaluator revaluator;
//
//    /**
//     * TODO make this adjust in realtime
//     */
//    private final MutableInteger threads = new MutableInteger();
//
//
//
//    public Focus focus;
//    private Consumer exe;
//
//
//    public WorkerMultiExec(Revaluator revaluator, int conceptCapacity, int qSize) {
//        this(revaluator, Util.concurrencyDefault(), conceptCapacity, qSize);
//    }
//
//    public WorkerMultiExec(Revaluator r, int threads, int conceptCapacity, int qSize) {
//        super(conceptCapacity);
//        this.revaluator = r;
//        this.threads.set(threads);
//        this.qSize = qSize;
//        this.exe = this::executeNow;
//    }
//
//    @Override
//    public final void execute(Consumer<NAR> r) {
//        execute((Object) r);
//    }
//
//    @Override
//    public final void execute(Runnable r) {
//        execute((Object) r);
//    }
//
//
//    @Override
//    public final void execute(Object t) {
//
//
//    }
//
//    @Override
//    public void execute(Stream<? extends ITask> input) {
//        input.forEach( x -> x.run(nar) );
//    }
//
//    @Override
//    public void execute(Iterator<? extends ITask> input) {
//        input.forEachRemaining( x -> x.run(nar) );
//    }
//
//
//
//
//    @Override
//    public void start(NAR nar) {
//        synchronized (this) {
//            this.focus = new Focus(nar, revaluator, threads);
//
//            super.start(nar);
//
//            this.pool = new BusyPool(threads.intValue(),
//                    new MultithreadConcurrentQueue(qSize)
//
//
//            ) {
//                @Override
//                protected WorkLoop newWorkLoop(ConcurrentQueue<Runnable> q) {
//                    return new MyWorkLoop(q);
//                }
//
//                @Override
//                protected void queueOverflow(Object x) {
//
//
//                    int qSize = q.size();
//                    Object next;
//                    while (((next = q.poll())!=null) && qSize-- > 0) {
//                        executeNow(next);
//                        if (q.offer(x))
//                            return;
//                    }
//                    if (!q.offer(x)) {
//                        Thread.yield();
//
//                        ForkJoinPool.commonPool().execute(x instanceof Runnable ? ((Runnable)x) : ()->executeNow(x));
//                    }
//
//                }
//            };
//            pool.workers.forEach(this::register);
//            this.exe = pool::queue;
//        }
//    }
//
//    @Override
//    public void stop() {
//        synchronized (this) {
//            exe = this::executeNow;
//            super.stop();
//            pool.shutdownNow();
//            pool = null;
//            focus = null;
//        }
//    }
//
//    @Override
//    public final boolean concurrent() {
//        return true;
//    }
//
//
//    private class MyWorkLoop extends BusyPool.WorkLoop {
//
//        /**
//         * TODO use non-atomic version of this, slightly faster
//         */
//        final Random rng;
//
//        private final Object[] qBatch;
//
//        public MyWorkLoop(ConcurrentQueue q) {
//            super(q);
//
//
//            qBatch = new Object[64];
//
//            rng =
//                    new SplitMix64Random(System.nanoTime());
//        }
//
//        @Override
//        public void run() {
//
//            focus.decide(rng, x -> {
//
//                int next = q.remove(qBatch);
//
//
//                for (int i = 0; i < next; i++) {
//                    Object qqq = qBatch[i];
//                    qBatch[i] = null;
//                    executeNow(qqq);
//                }
//
//
//
//
//
//
//
//
//
//
//
//                focus.tryRun(x);
//
//
//
//
//
//                return true;
//            });
//
//        }
//    }
//}
