package jcog.data.list.linked;


import jcog.data.pool.MetalPool;

/**
* Created by me on 1/20/15.
*/
public class DDNodePool<E> extends MetalPool<DD<E>> {

    public DDNodePool(int initialCapacity) {
        super(initialCapacity, Integer.MAX_VALUE);
    }

    @Override
    public DD<E> create() {
        return new DD<>();
    }

    @Override
    public void put(DD<E> i) {
        i.owner = -1;
        i.next = i.prev = null;
        i.item = null;
        super.put(i);
    }

    public DD<E> get(E item, int owner) {
        DD<E> x = get();
        x.item = item;
        x.owner = owner;
        return x;
    }



}
