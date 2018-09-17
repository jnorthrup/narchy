package jcog.data.list;

import jcog.TODO;
import jcog.data.iterator.ArrayIterator;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.block.procedure.Procedure;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;
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

    
    public final AtomicReference<X[]> copy = new AtomicReference(null);


    public FastCoWList(IntFunction<X[]> arrayBuilder) {
        this(0, arrayBuilder);
    }

    public FastCoWList(int capacity, IntFunction<X[]> arrayBuilder) {
        super(0, arrayBuilder.apply(capacity));
        this.copy.set( (this.arrayBuilder = arrayBuilder).apply(0) );
    }

    @Override
    protected Object[] newArray(int newCapacity) {
        return arrayBuilder.apply(newCapacity);
    }

    public void commit() {
        //this.copy = //toArrayCopy(copy, arrayBuilder);
        copy.updateAndGet((mayStillBeInUseDontTouch)->fillArray(arrayBuilder.apply(super.size()), false));
    }

    @Override
    public Iterator<X> iterator() {
        return ArrayIterator.get(array());
    }



    @Override
    public final int size() {
        X[] x = this.array();
        //return (this.size = (x != null ? x.length : 0));
        return x.length;
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
        for (X x : array())
            c.accept(x);
    }

    @Override
    public <Y> void forEachWith(Procedure2<? super X, ? super Y> c, Y y) {
        for (X x : array())
            c.accept(x, y);
    }


    @Override
    public Stream<X> stream() {
        return ArrayIterator.stream(array());
    }

    @Override
    public void reverseForEach(Procedure c) {
        X[] copy = array();
        if (copy != null) {
            for (int i = copy.length-1; i >= 0; i--) {
                c.accept(copy[i]);
            }
        }
    }

    public X[] array() {
        return this.copy.get();
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

    public boolean addDirect(X o) {
        return super.add(o);
    }
    public boolean removeDirect(Object o) {
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
        return ArrayUtils.indexOf(array(), object)!=-1;
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

    @Override
    public boolean removeIf(org.eclipse.collections.api.block.predicate.Predicate<? super X> predicate) {
        if (super.removeIf(predicate)) {
            commit();
            return true;
        }
        return false;
    }

    public boolean removeFirstInstance(X x) {
        if (super.removeFirstInstance(x)) {
            commit();
            return true;
        }
        return false;
    }

    @Override public final X get(int index) {
        X[] c = array();
        return c.length > index ? c[index] : null;
    }

    public float[] map(FloatFunction<X> f, float[] target) {
        X[] c = this.array();
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
            if (newValues.length == 0) {
                clear();
            } else {
                items = newValues;
                size = newValues.length;
                commit();
            }
        }
    }

    public boolean whileEach(Predicate<X> o) {
        for (X x : array()) {
            if (x!=null && !o.test(x))
                return false;
        }
        return true;
    }
    public boolean whileEachReverse(Predicate<X> o) {
        @Nullable X[] copy = this.array();
        for (int i = copy.length - 1; i >= 0; i--) {
            X x = copy[i];
            if (x!=null && !o.test(x))
                return false;
        }
        return true;
    }

}
