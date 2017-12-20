package jcog.list;

import jcog.TODO;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.IntFunction;

public class FastCoWList<X> extends FasterList<X> {

    private final IntFunction<X[]> arrayBuilder;

    public X[] copy;

    public FastCoWList(int capacity, IntFunction<X[]> arrayBuilder) {
        super(capacity);
        copy = arrayBuilder.apply(0);
        this.arrayBuilder = arrayBuilder;
    }

    protected final void commit() {
        this.copy = (size == 0) ? arrayBuilder.apply(0) :
                                  toArrayRecycled(arrayBuilder);
    }


    @Override
    public Iterator<X> iterator() {
        return copy.length> 0 ? ArrayIterator.get(copy) : Collections.emptyIterator();
    }

    @Override
    public int size() {
        return copy.length;
    }

    @Override
    public synchronized void clear() {
        int s = size();
        if (s > 0) {
            super.clear();
            commit();
        }
    }

    @Override
    public synchronized boolean add(X o) {
        if(super.add(o)) {
            commit();
            return true;
        }
        return false;
    }

    @Override
    public void forEach(Consumer c) {
        for (X x : copy)
            c.accept(x);
    }


    @Override
    public synchronized boolean remove(Object o) {
        if(super.remove(o)) {
            commit();
            return true;
        }
        return false;
    }
    @Override
    public boolean addAll(Collection<? extends X> source) {
        throw new TODO();
    }

    @Override
    public void add(int index, X element) {
        throw new TODO();
    }


    @Override
    public X get(int index) {
        return copy[index];
    }

    public float[] map(FloatFunction<X> f, float[] target) {
        X[] c = this.copy;
        int n = c.length;
        if (n == 0)
            return ArrayUtils.EMPTY_FLOAT_ARRAY;

        if (n !=target.length) {
            target = new float[n];
        }
        for (int i = 0; i < n; i++) {
            target[i] = f.floatValueOf(c[i]);
        }
        return target;
    }

}
