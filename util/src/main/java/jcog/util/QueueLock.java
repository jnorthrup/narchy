package jcog.util;

import jcog.Util;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * ex:
 * toAdd = new QueueLock<T>(writeLock, 4, this::add);
 */
public class QueueLock<X> implements Consumer<X> {

    //public final Lock lock;
    public final AtomicInteger busy;
    public final BlockingQueue<X> queue;
    private final Consumer<X> proc;

    private IntConsumer afterBatch;

    final static Logger logger = LoggerFactory.getLogger(QueueLock.class);

    /*
    //new EvictingQueue<>(capacity);
    //new DisruptorBlockingQueue<>(capacity);
    //new ArrayBlockingQueue<X>(capacity);
    //new LinkedBlockingQueue
     */


    public static QueueLock<Runnable> get(int capacity) {
        return new QueueLock<>(capacity, Runnable::run, null);
    }

    //TODO
    public static QueueLock<Runnable> getReentrant(int capacity) {
        final AtomicLong currentThread = new AtomicLong(-1);
        return new QueueLock<>(capacity, (r)->{
            currentThread.set(Thread.currentThread().getId());
            r.run();
            currentThread.set(-1);
        }, null) {
            @Override
            public void accept(Runnable runnable) {
                if (currentThread.get()==Thread.currentThread().getId()) {
                    runnable.run();
                    return; //elide queuing
                }
                super.accept(runnable);
            }
        };
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

        //this.lock = lock;
        this.busy = new AtomicInteger(0);
        this.proc = each;
        this.afterBatch = afterBatch;
    }

    @Override
    public void accept(X x) {
        int count = 0;

        if (!queue.offer(x)) {
            proc.accept(x); //bypass queue, requires that the procedure be thread-safe otherwise it must block here
            count = 1;
        } else {
            //        try {
            //
            //        } catch (InterruptedException e) {
            //            throw new RuntimeException(e);
            //        }

            boolean responsible = busy.compareAndSet(0, 1);
            if (!responsible)
                return; //to be processed by another thread

            final X[] next = (X[]) new Object[1];
            while (busy.updateAndGet((y) -> (next[0] = queue.poll()) != null ? 1 : 0) == 1) {
                X n = next[0];
                try {
                    proc.accept(n);
                } catch (Throwable t) {
                    onException(n, t);
                }
                count++;
            }
        }

        if (afterBatch != null) {
            try {
                afterBatch.accept(count);
            } catch (Throwable t) {
                onException(null, t);
            }
        }
    }

    protected void onException(X x, Throwable t) {
        logger.error("{} {}", x, t);
    }

}
