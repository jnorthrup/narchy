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
        //data = new ConcurrentLinkedDeque<>();

        setCapacity(initialCapacity);
        //data = new CircularArrayList<>(initialCapacity);

        for (int i = 0; i < preallocate; i++)
            take(create());

    }

    public DequePool(int initialCapacity) {
        this(initialCapacity, initialCapacity);
    }

    public void setCapacity(int c) {
        capacity = c;
    }

    @Override
    public void take(X i) {
        assert (i != null);
        data.offer(i);
    }

    @Override
    public final X get() {
        //synchronized (data) {

        Deque<X> d = data;

        if (d.isEmpty()) {
            //System.err.println(this + " emptied, initialize larger or plug leaks");
            return create();
        }
        return d.poll();
        //}
    }

    public boolean isEnabled() {
        return capacity != 0;
    }

    @Override
    public void delete() {
        capacity = 0;
        data.clear();
    }

    public void take(Collection<X> c) {
        c.forEach(this::take);
        c.clear();
    }

    public void take(Iterator<X> c) {
        while (c.hasNext()) {
            take(c.next());
            c.remove();
        }
    }
}
