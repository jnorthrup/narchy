package jcog.data.list;

import jcog.TODO;
import jcog.data.iterator.ArrayIterator;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;

/** be careful about synchronizing to instances of this class
 * because the class synchronizes on itself and not a separate lock object
 * (for efficiency purposes)
 *
 * TODO size inherited from FasterList is not volatile
 */
public class FastCoWList<X> extends FasterList<X> {

    private final IntFunction<X[]> arrayBuilder;

    @Nullable
    public volatile X[] copy;


    public FastCoWList(IntFunction<X[]> arrayBuilder) {
        this(0, arrayBuilder);
    }

    public FastCoWList(int capacity, IntFunction<X[]> arrayBuilder) {
        super(0, arrayBuilder.apply(capacity));
        this.copy = (this.arrayBuilder = arrayBuilder).apply(0);
    }

    @Override
    protected Object[] newArray(int newCapacity) {
        return arrayBuilder.apply(newCapacity);
    }

    protected void commit() {
        this.copy = toArrayCopy(copy, arrayBuilder);
    }

    @Override
    public Iterator<X> iterator() {
        return ArrayIterator.get(this.copy);
    }

    @Override
    public final int size() {
        X[] x = this.copy;
        return (this.size = (x != null ? x.length : 0));
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

        synchronized (this) {
            if (size <= index) {
                ensureCapacity(index + 1);
                if (element != null) {
                    super.setFast(index, element);
                    size = index + 1;
                    commit();
                }
                return null;
            }
        }

        X[] ii = items;
        X old = ii[index];
        if (old!=element) {
            ii[index] = element;
            commit();
        }
        return old;
    }

    public void set(Collection<X> newContent) {
        synchronized (this) {
            super.clear();
            super.addAll(newContent);
            commit();
        }
    }


    @Override
    public void forEach(Consumer c) {
        X[] copy = this.copy;
        
            for (X x : copy)
                c.accept(x);
        
    }

    @Override
    public Stream<X> stream() {
        return ArrayIterator.stream(copy);
    }

    @Override
    public void reverseForEach(Procedure c) {
        X[] copy = this.copy;
        if (copy != null) {
            for (int i = copy.length-1; i >= 0; i--) {
                c.accept(copy[i]);
            }
        }
    }

    @Override
    public boolean remove(Object o) {
        synchronized (this) {
            if (removeDirect(o)) {
                commit();
                return true;
            }
            return false;
        }
    }

    protected boolean addDirect(X o) {
        return super.add(o);
    }
    protected boolean removeDirect(Object o) {
        return super.remove(o);
    }

    @Override
    public boolean add(X o) {
        synchronized (this) {
            if (addDirect(o)) {
                commit();
                return true;
            }
            return false;
        }
    }

    @Override
    public boolean contains(Object object) {
        return ArrayUtils.indexOf(copy, object)!=-1;
    }

    @Override
    public boolean addAll(Collection<? extends X> source) {
        if (source.isEmpty())
            return false;
        synchronized (this) {
            source.forEach(this::addDirect);
            commit();
            return true;
        }
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        throw new TODO();
    }

    @Override
    public void add(int index, X element) {
        throw new TODO();
    }


    @Override public X get(int index) {
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
        if (newValues.length == 0) {
            clear();
            return;
        }

        synchronized (this) {
            items = newValues;
            size = newValues.length;
            commit();
        }
    }

    public boolean whileEach(Predicate<X> o) {
        for (X x : copy) {
            if (!o.test(x))
                return false;
        }
        return true;
    }
    public boolean whileEachReverse(Predicate<X> o) {
        @Nullable X[] copy = this.copy;
        for (int i = copy.length - 1; i >= 0; i--) {
            X x = copy[i];
            if (!o.test(x))
                return false;
        }
        return true;
    }

}
