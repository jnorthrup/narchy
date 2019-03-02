package jcog.tree.radix;

import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ConcurrentRadixTree<X> extends MyRadixTree<X> {

    @Nullable
    private final Lock readLock, writeLock;

    /**
     * essentially a version number which increments each acquired write lock, to know if the tree has changed
     */
    final static AtomicInteger writes = new AtomicInteger();

    public ConcurrentRadixTree() {
        this(false);
    }

    /**
     * * @param restrictConcurrency If true, configures use of a {@link ReadWriteLock} allowing
     * *                            concurrent reads, except when writes are being performed by other threads, in which case writes block all reads;
     * *                            if false, configures lock-free reads; allows concurrent non-blocking reads, even if writes are being performed
     * *                            by other threads
     *
     * @param restrict
     */
    public ConcurrentRadixTree(boolean restrictConcurrency) {
        super();
        ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
        this.writeLock = readWriteLock.writeLock();
        this.readLock = restrictConcurrency ? readWriteLock.readLock() : null;
    }


    @Override
    protected int beforeWrite() {
        return writes.intValue();
    }

    public final int acquireWriteLock() {
        writeLock.lock();
        return writes.incrementAndGet();
    }

    public final void releaseWriteLock() {
        writeLock.unlock();
    }


    public final void acquireReadLockIfNecessary() {
        if (readLock != null)
            readLock.lock();
    }

    public final void releaseReadLockIfNecessary() {
        if (readLock != null)
            readLock.unlock();
    }

}
