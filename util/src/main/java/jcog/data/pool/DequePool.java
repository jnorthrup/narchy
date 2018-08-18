package jcog.data.pool;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.Iterator;

/**
 * Simple object pool implemented by a Deque (ex: ArrayDeque)
 */
public abstract class DequePool<X> implements Pool<X> {

    protected final Deque<X> data;
    private int capacity;



    public DequePool(int initialCapacity, int preallocate) {
        data = new ArrayDeque(initialCapacity);
        

        setCapacity(initialCapacity);
        

        for (int i = 0; i < preallocate; i++)
            put(create());

    }

    public DequePool(int initialCapacity) {
        this(initialCapacity, 0);
    }

    public void setCapacity(int c) {
        capacity = c;
    }

    @Override
    public void put(X i) {
        assert (i != null);
        data.offer(i);
    }

    @Override
    public final X get() {
        

        Deque<X> d = data;

        if (d.isEmpty()) {
            
            return create();
        }
        return d.poll();
        
    }

    public boolean isEnabled() {
        return capacity != 0;
    }

    @Override
    public void delete() {
        capacity = 0;
        data.clear();
    }

    public void put(Collection<X> c) {
        c.forEach(this::put);
    }
    public void take(Collection<X> c) {
        put(c);
        c.clear();
    }

    public void take(Iterator<X> c) {
        while (c.hasNext()) {
            put(c.next());
            c.remove();
        }
    }

    public int size() {
        return size();
    }
}
