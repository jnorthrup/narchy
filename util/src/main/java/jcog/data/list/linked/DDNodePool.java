package jcog.data.list.linked;


import jcog.data.pool.DequePool;

/**
* Created by me on 1/20/15.
*/
public class DDNodePool<E> extends DequePool<DD<E>> {

    public DDNodePool(int initialCapacity) {
        super(initialCapacity);
    }

    @Override
    public DD<E> create() {
        return new DD<>();
    }

    @Override
    public void take(DD<E> i) {
        i.owner = -1;
        i.next = i.prev = null;
        i.item = null;
        super.take(i);
    }

    public DD<E> get(E item, int owner) {
        DD<E> x = get();
        x.item = item;
        x.owner = owner;
        return x;
    }



}
