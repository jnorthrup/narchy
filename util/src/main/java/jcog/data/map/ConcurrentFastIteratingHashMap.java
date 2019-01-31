package jcog.data.map;

import jcog.data.iterator.ArrayIterator;
import jcog.data.list.FastCoWList;
import jcog.data.list.FasterList;
import jcog.random.SplitMix64Random;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.*;

public class ConcurrentFastIteratingHashMap<X, Y> extends AbstractMap<X, Y>  {

    final Y[] emptyArray;

    final Map<X, Y> map =
            //new ConcurrentOpenHashMap<>();
            new ConcurrentHashMap<>();

    /** maximum allowed extra null space at suffix of array before reallocating smaller */
    static final int extraThreshold = 16;

    /** double buffer live copy */
    //private volatile Y[] list;
    private final FastCoWList<Y> list;
    private AtomicBoolean invalid = new AtomicBoolean(false);

//    /** double buffer backup copy */
//    final AtomicReference<T[]> lists = new AtomicReference<>();



    public ConcurrentFastIteratingHashMap(Y[] emptyArray) {
        this.emptyArray = emptyArray;
        this.list = new FastCoWList<>((x)->Arrays.copyOf(emptyArray, x));
        //lists.setAt(this.list = this.emptyArray = emptyArray);
    }

    /**
     * without synchronizing this entire method, the best this can do is
     * a near atomic invalidation of the list after the hashmap method returns
     */
    @Override
    public Y putIfAbsent(X key, Y value) {
        Y r;
        if ((r = map.putIfAbsent(key, value)) != value) {
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
    public Y remove(Object key) {
        Y r = map.remove(key);
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
        Y[] y = valueArray();
        return y.length;
//        Y[] y = list.readOK();
//        if (y == null)
//            return map.size();
//        else
//            return y.length;
    }

    @Override
    public boolean isEmpty() {
        return size()==0;
    }

    public List<Y> asList() {
        return new MyAbstractList();
    }

    /**
     * this is the fast value iterating method
     */
    public final void forEachValue(Consumer<? super Y> action) {
        for (Y y : valueArray()) {
            if (y !=null)
                action.accept(y);
        }
    }

    @Override
    public Y compute(X key, BiFunction<? super X, ? super Y, ? extends Y> remappingFunction) {
        final boolean[] changed = {false};
        Y v = map.compute(key, (k, pv) -> {
            Y next = remappingFunction.apply(k, pv);
            if (next != pv)
                changed[0] = true;
            return next;
        });
        if (changed[0])
            invalidate();
        return v;
    }

    @Override
    public Y computeIfAbsent(X key, Function<? super X, ? extends Y> mappingFunction) {
        final boolean[] changed = {false};
        Y v = map.computeIfAbsent(key, (p) -> {
            Y next = mappingFunction.apply(p);
            changed[0] = true;
            return next;
        });
        if (changed[0])
            invalidate();
        return v;
    }


    public final void invalidate() {
        invalid.set(true);
    }

    public boolean whileEachValue(Predicate<? super Y> action) {
        Y[] x = valueArray();
        for (Y xi : x) {
            if (xi != null && !action.test(xi))
                return false;
        }
        return true;
    }

    public boolean whileEachValueReverse(Predicate<? super Y> action) {
        Y[] x = valueArray();
        for (int i = x.length - 1; i >= 0; i--) {
            Y xi = x[i];
            if (xi!=null && !action.test(xi))
                return false;
        }
        return true;
    }

    @Override
    public Y get(Object key) {
        return map.get(key);
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


    public Iterator<Y> valueIterator() {
        return ArrayIterator.get(valueArray());
    }


    public final Y[] valueArray() {
        //return list.readValid(true, this::_valueArray);
        if (invalid.compareAndSet(true,false)) {
            list.set(map.values());
        }
        return list.array();
    }

//    private Y[] _valueArray(Y[] prev) {
//        Y[] next;
//        if (map instanceof ConcurrentOpenHashMap) { //HACK @Deprecated
//            next = ((ConcurrentOpenHashMap<?, Y>) map).values(prev, i -> Arrays.copyOf(emptyArray, i));
//        } else {
//            int s = map.size();
//            if (s== 0)
//                return emptyArray;
//
//
//            if (s > prev.length || (prev.length - extraThreshold > s)) {
//                next = Arrays.copyOf(emptyArray, s);
//            } else {
//                next = prev;
//            }
//
//            final int[] j = {0};
//            ((ConcurrentHashMap<X,Y>)map).forEachValue(1, x->{
//                assert(x!=null);
//                //if (x!=null) {
//                    int jj = j[0];
//                    if (jj < next.length) {
//                        next[jj] = x;
//                        j[0]++;
//                    }
//                //}
//            });
//
//            if (j[0] < next.length-1)
//                Arrays.fill(next, j[0], next.length, null);
//
//            //next = map.values().toArray(prev);
//        }
//        return next;
//    }


    @Override
    public Y put(X key, Y value) {
        Y prev = value == null ? map.remove(key) : map.put(key, value);
        if (prev!=value)
            invalidate();
        return prev;
    }

    public boolean removeIf(Predicate<? super Y> filter) {
        if (map.values().removeIf(filter)) {
            invalidate();
            return true;
        }
        return false;
    }


    public boolean removeIf(BiPredicate<X, ? super Y> filter) {
        if (map.entrySet().removeIf((e) -> filter.test(e.getKey(), e.getValue()))) {
            invalidate();
            return true;
        }
        return false;
    }

    public void clear(Consumer<Y> each) {
        removeIf((y)-> {
            each.accept(y);
            return true;
        });
    }



    private final class MyAbstractList extends AbstractList<Y> {

        @Override
        public int size() {
            return ConcurrentFastIteratingHashMap.this.size();
        }

        @Override
        public Y get(int i) {
           return getIndex(i);
        }
    }

    public Y getIndex(int i) {
        Y[] l = valueArray();
        return l != null && l.length > i ? l[i] : null;
    }
    public Y getIndex(Random rng) {
        Y[] l = valueArray();
        return l != null && l.length > 0 ? l[rng.nextInt(l.length)] : null;
    }

    public Y getIndex(SplitMix64Random rng) {
        Y[] l = valueArray();
        return l != null && l.length > 0 ? l[rng.nextInt(l.length)] : null;
    }
}
