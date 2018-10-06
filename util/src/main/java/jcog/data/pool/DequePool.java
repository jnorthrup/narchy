package jcog.data.pool;

import jcog.pri.bag.util.Treadmill;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple object pool implemented by a Deque (ex: ArrayDeque)
 * guarded by a SpinMutex
 * no synchronizes; but should be thread safe
 */
public abstract class DequePool<X> implements Pool<X> {

    private final Queue<X> data;
    private int capacity;

    private static final Treadmill mutex = new Treadmill();
    private static final AtomicInteger serial = new AtomicInteger();
    final int poolID;

    public DequePool() {
        this(Integer.MAX_VALUE);
    }

    public DequePool(int initialCapacity) {
        this.poolID = serial.getAndIncrement();

        data = new ArrayDeque<>(initialCapacity < Integer.MAX_VALUE ? initialCapacity : 0);
                //new ConcurrentLinkedDeque<>();

        capacity(initialCapacity);
    }

    public DequePool prepare(int preallocate) {
        for (int i = 0; i < preallocate; i++)
            put(create());
        return this;
    }


    public DequePool<X> capacity(int c) {
        capacity = c;
        return this;
    }

    @Override
    public void put(X i) {
        assert (i != null);

        mutex.run(poolID, 1, ()->{
            if (capacity == Integer.MAX_VALUE || data.size() < capacity)
                data.offer(i);
        });
    }

    @Override
    public void delete() {
        capacity = 0;
        mutex.run(poolID, 1, data::clear);
    }

    @Override
    public final X get() {
        X e = mutex.run(poolID, 1, data::poll);
        return e == null ? create() : e;
    }

    public boolean isEnabled() {
        return capacity != 0;
    }



    public void putAll(Collection<X> c) {
        c.forEach(this::put);
    }

    public void take(Collection<X> c) {
        putAll(c);
        c.clear();
    }

    public void take(Iterator<X> c) {
        while (c.hasNext()) {
            put(c.next());
            c.remove();
        }
    }

    public int size() {
        return data.size();
    }
}
