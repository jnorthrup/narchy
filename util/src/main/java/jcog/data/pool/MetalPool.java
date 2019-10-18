package jcog.data.pool;

import jcog.data.list.FasterList;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.Supplier;

/**
 * Simple object pool implemented by a Deque (ex: ArrayDeque)
 * guarded by a SpinMutex
 * no synchronizes; but should be thread safe
 */
public abstract class MetalPool<X> implements Pool<X> {

    protected final FasterList<X> data;
    private int capacity;


    public MetalPool() {
        this(64, Integer.MAX_VALUE);
    }

    /** if maxCapacity==Integer.MAX_VALUE, capacity is not tested on inserts */
    public MetalPool(int initialCapacity, int maxCapacity) {
        data = new FasterList<>(initialCapacity);
                //new ConcurrentLinkedDeque<>();

        capacity(maxCapacity);
    }

    /** note: ThreadLocal obviously doesnt require the thread-safe version */
    public static <X> ThreadLocal<MetalPool<X>> threadLocal(Supplier<X> o) {
        //noinspection Convert2Diamond
        return ThreadLocal.withInitial(() -> new MetalPool<X>() {
            @Override
            public X create() {
                return o.get();
            }
        });
    }

    public void prepare(int preallocate) {
        for (int i = 0; i < preallocate; i++)
            put(create());
    }


    public MetalPool<X> capacity(int c) {
        capacity = c;
        return this;
    }

    @Override
    public void put(X i) {
        //assert (i != null);

        int c = this.capacity;
        if (c == Integer.MAX_VALUE || data.size() < c) {
            data.add(i);
        }

    }
    public void put(X[] items, int size) {
        int c = this.capacity;
        if (c != Integer.MAX_VALUE) {
            size = c - data.size();
            if (size <= 0)
                return;
        }

        data.ensureCapacityForAdditional(size);
        data.addFast(items, size);
    }

    @Override
    public void delete() {
        capacity = 0;
        data.clear();
    }

    @Override
    public X get() {
        X e = data.poll();
        return e == null ? create() : e;
    }

    public boolean isEnabled() {
        return capacity != 0;
    }


    public void putAll(Collection<X> c) {
        for (X x : c) {
            put(x);
        }
    }

    public void steal(Collection<X> c) {
        putAll(c);
        c.clear();
    }

    public void steal(Iterator<X> c) {
        while (c.hasNext()) {
            put(c.next());
            c.remove();
        }
    }

    public int size() {
        return data.size();
    }



}
