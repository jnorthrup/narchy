package jcog.util;

import jcog.Util;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * ex:
 * toAdd = new QueueLock<T>(writeLock, 4, this::add);
 */
public class QueueLock<X> implements Consumer<X> {
    private final ThreadPoolExecutor exe;
    private final Consumer<X> proc;

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
    public QueueLock(BlockingQueue queue, Consumer<X> each, @Nullable IntConsumer afterBatch) {
        this.exe = new ThreadPoolExecutor(1, 1,
                0L, TimeUnit.MILLISECONDS,
                queue);


        this.proc = each;
        
    }





    public void accept(X x) {
        exe.submit(()->proc.accept(x));
    }




































































































































}
