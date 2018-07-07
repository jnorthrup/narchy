package jcog.data.map;

import jcog.TODO;
import jcog.list.FasterList;
import jcog.util.ArrayIterator;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class ConcurrentFastIteratingHashMap<X, T> extends AbstractMap<X, T>  {

    final T[] emptyArray;

    final Map<X, T> map = new ConcurrentOpenHashMap<>();

    private volatile T[] list = null;


    public ConcurrentFastIteratingHashMap(T[] emptyArray) {
        this.emptyArray = emptyArray;
    }

    /**
     * without synchronizing this entire method, the best this can do is
     * a near atomic invalidation of the list after the hashmap method returns
     */
    @Override
    public T putIfAbsent(X key, T value) {
        T r;
        if ((r = map.putIfAbsent(key, value)) == null) {
            invalidate();
            return null;
        }
        return r;
    }

    /**
     * without synchronizing this entire method, the best this can do is
     * a near atomic invalidation of the list after the hashmap method returns
     */
    @Override
    public T remove(Object key) {
        T r = map.remove(key);
        if (r != null)
            invalidate();
        return r;
    }

    @Override
    public boolean remove(Object key, Object value) {
        if (map.remove(key, value)) {
            invalidate();
            return true;
        }
        return false;
    }

    @Override
    public void clear() {
        map.clear();
        invalidate();
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    public List<T> asList() {
        return new MyAbstractList();
    }
    /**
     * this is the fast value iterating method
     */
    public void forEachValue(Consumer<? super T> action) {
        T[] x = valueArray();
        for (T t : x)
            action.accept(t);
    }

    @Override
    public T compute(X key, BiFunction<? super X, ? super T, ? extends T> remappingFunction) {
        throw new TODO();
    }

    @Override
    public T computeIfAbsent(X key, Function<? super X, ? extends T> mappingFunction) {
        final boolean[] changed = {false};
        T prev = map.computeIfAbsent(key, (p) -> {
            T next = mappingFunction.apply(p);
            if (next != p)
                changed[0] = true;
            return next;
        });

        if (changed[0]) {
            invalidate();
        }

        return prev;
    }

    public void invalidate() {
        list = null;
    }

    public boolean whileEachValue(Predicate<? super T> action) {
        T[] x = valueArray();
        for (int i = 0, xLength = x.length; i < xLength; i++) {
            if (!action.test(x[i]))
                return false;
        }
        return true;
    }

    public boolean whileEachValueReverse(Predicate<? super T> action) {
        T[] x = valueArray();
        for (int i = x.length - 1; i >= 0; i--) {
            if (!action.test(x[i]))
                return false;
        }
        return true;
    }

    @Override
    public T get(Object key) {
        return map.get(key);
    }

    @Override
    public Set<Entry<X, T>> entrySet() {
        return map.entrySet();
    }

    @Override
    public Set<X> keySet() {
        return map.keySet();
    }

    @Override
    public Collection<T> values() {
        return new FasterList(valueArray());
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }


    public Iterator<T> valueIterator() {
        return ArrayIterator.get(valueArray());
    }


    public T[] valueArray() {
        T[] x = list;
        if (x == null) {
            return this.list = updateValues();
        } else {
            return x;
        }
    }

    public T[] updateValues() {
        return ((FasterList<T>)(((ConcurrentOpenHashMap<?, T>)map)
                .values(this.emptyArray)))
                .toArrayRecycled(i -> Arrays.copyOf(emptyArray, i));
    }


    @Override
    public T put(X key, T value) {
        T prev = map.put(key, value);
        if (prev!=value)
            invalidate();
        return prev;
    }

    public boolean removeIf(Predicate<? super T> filter) {
        FasterList toRemove = new FasterList(1);
        map.forEach((k,v)->{
            if (filter.test(v))
                toRemove.add(k);
        });
        if (toRemove.isEmpty())
            return false;
        toRemove.forEach(map::remove /* direct, not the .remove() which automatically invalidates */);
        invalidate();
        return true;
    }

    private final class MyAbstractList extends AbstractList {

        @Override
        public int size() {
            return ConcurrentFastIteratingHashMap.this.size();
        }

        @Override
        public T get(int i) {
           return getIndex(i);
        }
    }

    public T getIndex(int i) {
        T[] l = valueArray();
        if (l!=null && l.length > i)
            return l[i];
        else
            return null;
    }
}
