package jcog.util;

import jcog.Util;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * ex:
 * toAdd = new QueueLock<T>(writeLock, 4, this::add);
 */
public class QueueLock<X> implements Consumer<X> {

    public final BlockingQueue<X> queue;
    private final Consumer<X> proc;

    private IntConsumer afterBatch;

    final static Logger logger = LoggerFactory.getLogger(QueueLock.class);

    /** exclusive current working thread */
    volatile private AtomicLong exe = new AtomicLong(Long.MIN_VALUE);


    /*
    //new EvictingQueue<>(capacity);
    //new DisruptorBlockingQueue<>(capacity);
    //new ArrayBlockingQueue<X>(capacity);
    //new LinkedBlockingQueue
     */


    public static QueueLock<Runnable> get(int capacity) {
        return new QueueLock<>(capacity, Runnable::run, null);
    }

    public QueueLock(int capacity, Consumer<X> each, @Nullable IntConsumer afterBatch) {
        this(Util.blockingQueue(capacity), each, afterBatch);
    }

    /**
     * @param queue      holds the queue of items to be processed
     * @param each       procedure to process each queued item
     * @param afterBatch if non-null, called after the batch is finished
     */
    public QueueLock(BlockingQueue<X> queue, Consumer<X> each, @Nullable IntConsumer afterBatch) {
        this.queue = queue;
        this.proc = each;
        this.afterBatch = afterBatch;
    }

    /** when false, re-entrant enqueuing by the accepted thread are elided and proceeds synchronously.
     *  when true, re-entrant enqueue will actually be enqueued to be executed later as if it were another thread attempting access. */
    public void accept(X x, boolean forceQueue) {
        if (!forceQueue && this.exe.get() == Thread.currentThread().getId()) {
            //re-entrant invocation
            proc.accept(x);
            return;
        }

        try {
            queue.put(x);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

//        if (!queue.offer(x)) {
//            proc.accept(x); //bypass queue, requires that the procedure be thread-safe otherwise it must block here
//            count = 1;
//        } else {

        //        try {
        //
        //        } catch (InterruptedException e) {
        //            throw new RuntimeException(e);
        //        }

        long threadID = Thread.currentThread().getId();
        boolean responsible = exe.compareAndSet(Long.MIN_VALUE, threadID);
        if (responsible) {

            try {


//                int s = queue.size();
//
//                if (s > 2) {
//                    List<X> buffer = new FasterList(s);
//
//                    queue.drainTo(buffer);
//
//                    int done = buffer.size();
//
//                    buffer.forEach(proc);
//                } else {
//                    X next;
//                    while ((next = queue.poll())!=null)
//                        proc.accept(next);
//                }



                int done = 0;

                final X[] next = (X[]) new Object[1];
                while (exe.updateAndGet((y) -> (next[0] = queue.poll()) != null ? threadID : Long.MIN_VALUE) != Long.MIN_VALUE) {
                    X n = next[0];
                    try {
                        proc.accept(n);
                    } catch (Throwable t) {
                        onException(n, t);
                    }
                    done++;
                }

                if (afterBatch != null) {
                    try {
                        afterBatch.accept(done);
                    } catch (Throwable t) {
                        onException(null, t);
                    }
                }
            } catch (Exception e) {
                this.exe.set(Long.MIN_VALUE);
                throw e;
            }
        }

    }

    @Override
    public void accept(X x) {

        accept(x, false);

    }

    protected void onException(X x, Throwable t) {
        logger.error("{} {}", x, t);
    }

//    //TODO
//    public static QueueLock<Runnable> getReentrant(int capacity) {
//        final AtomicLong currentThread = new AtomicLong(-1);
//        return new QueueLock<>(capacity, (r)->{
//            currentThread.set(Thread.currentThread().getId());
//            try {
//                r.run();
//            } finally {
//                currentThread.set(-1);
//            }
//        }, null) {
//            @Override
//            public void accept(Runnable runnable) {
//                if (currentThread.get()==Thread.currentThread().getId()) {
//                    runnable.run();
//                    return; //elide queuing
//                }
//                super.accept(runnable);
//            }
//        };
//    }

}
