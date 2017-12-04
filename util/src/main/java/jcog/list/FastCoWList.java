package jcog.list;

import jcog.TODO;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.IntFunction;

public class FastCoWList<X> extends FasterList<X> {

    private static final Object[] EMPTY = new Consumer[0];

    private final IntFunction<X[]> arrayBuilder;

    public X[] copy = (X[]) FastCoWList.EMPTY;

    public FastCoWList(int capacity, IntFunction<X[]> arrayBuilder) {
        super(capacity);
        this.arrayBuilder = arrayBuilder;
    }

    protected final void commit() {
        this.copy = (size == 0) ? (X[]) EMPTY :
                                  toArrayRecycled(arrayBuilder);
    }

    @Override
    public Iterator<X> iterator() {
        return copy!=EMPTY ? new ArrayIterator<>(copy) : Collections.emptyIterator();
    }

    @Override
    public int size() {
        return copy.length;
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
        if (n !=target.length) {
            target = new float[n];
        }
        for (int i = 0; i < n; i++) {
            target[i] = f.floatValueOf(c[i]);
        }
        return target;
    }

}
