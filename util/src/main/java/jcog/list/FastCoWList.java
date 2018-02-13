package jcog.list;

import jcog.TODO;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.IntFunction;

public class FastCoWList<X> extends FasterList<X> {

    private final IntFunction<X[]> arrayBuilder;

    @Nullable
    public volatile X[] copy;

    public FastCoWList(IntFunction<X[]> arrayBuilder) {
        this(0, arrayBuilder);
    }

    public FastCoWList(int capacity, IntFunction<X[]> arrayBuilder) {
        super(capacity);
        this.copy = (this.arrayBuilder = arrayBuilder).apply(0);

    }

    private final void commit() {
        this.copy = //(size == 0) ? null :
                toArrayRecycled(arrayBuilder);
    }


    @Override
    public Iterator<X> iterator() {
        X[] copy = this.copy;
        return ArrayIterator.get(copy);
    }

    @Override
    public final int size() {
        X[] x = this.copy;
        return x != null ? x.length : 0;
    }

    @Override
    public final boolean isEmpty() {
        return copy.length == 0;
    }

    @Override
    public void clear() {
        synchronized (this) {
            if (super.clearIfChanged())
                commit();
        }
    }

    @Override
    public X set(int index, X element) {
        X old;
        synchronized (this) {
            if (size <= index) {
                ensureCapacity(index + 1);
                if (element!=null) {
                    super.setFast(index, element);
                    size = index+1;
                    commit();
                }
                return null;
            } else {
                old = get(index);
                if (old!=element) {
                    super.setFast(index, element);
                    commit();
                }
                return old;
            }

        }
    }

    public void set(Collection<X> newContent) {
        synchronized (this) {
            super.clear();
            super.addAll(newContent);
            commit();
        }
    }

    @Override
    public boolean add(X o) {
        synchronized (this) {
            if (super.add(o)) {
                commit();
                return true;
            }
            return false;
        }
    }

    @Override
    public void forEach(Consumer c) {
        X[] copy = this.copy;
        if (copy != null) {
            for (X x : copy)
                c.accept(x);
        }
    }


    @Override
    public boolean remove(Object o) {
        synchronized (this) {
            if (super.remove(o)) {
                commit();
                return true;
            }
            return false;
        }
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
        if (n != target.length) {
            target = new float[n];
        }
        for (int i = 0; i < n; i++) {
            target[i] = f.floatValueOf(c[i]);
        }
        return target;
    }

    /**
     * directly set
     */
    public void set(X[] newValues) {
        synchronized (this) {
            items = newValues;
            size = newValues.length;
            commit();
        }
    }

}
