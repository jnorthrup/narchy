package jcog.data.pool;

import jcog.mutex.Spin;

/** thread-safe version of MetalPool guarded by spin mutex */
public abstract class SpinMetalPool<X> extends MetalPool<X> {

    final Spin spin = new Spin();

    public SpinMetalPool() {
        super();
    }

    public SpinMetalPool(int initialCapacity, int maxCapacity) {
        super(initialCapacity, maxCapacity);
    }

    @Override
    public void put(X i) {
        spin.run(super::put, i);
    }

    @Override
    public void delete() {
        spin.run(super::delete);
    }

    @Override
    public X get() {
        X e = spin.run(data::poll);
        return e == null ? create() : e;
    }
}
