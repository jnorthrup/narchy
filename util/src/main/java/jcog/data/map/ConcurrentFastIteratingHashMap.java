package jcog.data.map;

import jcog.util.ArrayIterator;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ConcurrentFastIteratingHashMap<X, Y> extends AbstractMap<X,Y> {

    final Y[] emptyArray;

    volatile Y[] list = null;

    final Map<X, Y> map = new ConcurrentHashMap<>() {

        /** without synchronizing this entire method, the best this can do is
         * a near atomic invalidation of the list after the hashmap method returns */
        @Override
        public Y putIfAbsent(X key, Y value) {
            Y r;
            if ((r = super.putIfAbsent(key, value)) == null) {
                list = null;
                return null;
            }
            return r;
        }

        /** without synchronizing this entire method, the best this can do is
         * a near atomic invalidation of the list after the hashmap method returns */
        @Override
        public Y remove(Object key) {
            Y r = super.remove(key);
            if (r != null)
                list = null;
            return r;
        }

        @Override
        public boolean remove(Object key, Object value) {
            if (super.remove(key, value)) {
                list = null;
                return true;
            }
            return false;
        }

        @Override
        public void clear() {
            super.clear();
            list = null;
        }
    };

    public ConcurrentFastIteratingHashMap(Y[] emptyArray) {
        this.emptyArray = emptyArray;
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    /** this is the fast value iterating method */
    public void forEachValue(Consumer<? super Y> action) {
        Y[] x = valueArray();
        for (Y t : x)
            action.accept(t);
    }

    @Override
    public Set<Entry<X, Y>> entrySet() {
        return map.entrySet();
    }

    @Override
    public Set<X> keySet() {
        return map.keySet();
    }

    @Override
    public Collection<Y> values() {
        return map.values();
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }


    public Iterator<Y> valueIterator() {
        return ArrayIterator.get(valueArray());
    }


    public Y[] valueArray() {
        Y[] x = list;
        if (x == null) {
            return this.list = values().toArray(emptyArray);
        } else {
            return x;
        }
    }


    @Override
    public Y put(X key, Y value) {
        return map.put(key, value);
    }

    @Override
    public boolean remove(Object key, Object value) {
        return map.remove(key, value);
    }

    @Override
    public Y remove(Object o) {
        return map.remove(o);
    }

    @Override
    public void clear() {
        map.clear();
    }
}
