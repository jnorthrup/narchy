package jcog.data.pool;

import jcog.mutex.Treadmill64;

import java.util.concurrent.atomic.AtomicInteger;

/** thread-safe version of MetalPool guarded by spin mutex */
public abstract class SpinMetalPool<X> extends MetalPool<X> {
    private static final Treadmill64 mutex = new Treadmill64();
    private static final AtomicInteger serial = new AtomicInteger();
    final int poolID = serial.getAndIncrement();

    public SpinMetalPool() {
        super();
    }

    public SpinMetalPool(int initialCapacity, int maxCapacity) {
        super(initialCapacity, maxCapacity);
    }

    @Override
    public void put(X i) {
        mutex.runWith(poolID, 1, super::put, i);
    }

    @Override
    public void delete() {
        mutex.run(poolID, 1, super::delete);
    }

    @Override
    public X get() {
        X e = mutex.run(poolID, 1, data::poll);
        return e == null ? create() : e;
    }
}
