package jcog.list;

import jcog.TODO;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.IntFunction;

public class FastCoWList<X> extends FasterList<X> {

    private final IntFunction<X[]> arrayBuilder;

    @Nullable
    public X[] copy;

    public FastCoWList(int capacity, IntFunction<X[]> arrayBuilder) {
        super(capacity);
        this.copy = null;
        this.arrayBuilder = arrayBuilder;
    }

    protected final void commit() {
        this.copy = (size == 0) ? null :
                                  toArrayRecycled(arrayBuilder);
    }


    @Override
    public Iterator<X> iterator() {
        X[] copy = this.copy;
        return copy!=null ? ArrayIterator.get(copy) : Collections.emptyIterator();
    }

    @Override
    public int size() {
        X[] copy = this.copy;
        if (copy!=null) return copy.length;
        else return 0;
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
        X[] copy = this.copy;
        if (copy!=null) {
            for (X x : copy)
                c.accept(x);
        }
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
        if (c == null)
            return ArrayUtils.EMPTY_FLOAT_ARRAY;
        int n = c.length;
        if (n !=target.length) {
            target = new float[n];
        }
        for (int i = 0; i < n; i++) {
            target[i] = f.floatValueOf(c[i]);
        }
        return target;
    }

}
