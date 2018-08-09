package jcog.data.map;

import jcog.TODO;
import jcog.data.iterator.ArrayIterator;
import jcog.data.list.FasterList;
import jcog.util.FlipArray;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class ConcurrentFastIteratingHashMap<X, Y> extends AbstractMap<X, Y>  {

    final Y[] emptyArray;

    final Map<X, Y> map =
            //new ConcurrentOpenHashMap<>();
            new ConcurrentHashMap<>();

    /** maximum allowed extra null space at suffix of array before reallocating smaller */
    static final int extraThreshold = 16;

    /** double buffer live copy */
    private volatile FlipArray<Y> list;

//    /** double buffer backup copy */
//    final AtomicReference<T[]> lists = new AtomicReference<>();



    public ConcurrentFastIteratingHashMap(Y[] emptyArray) {
        this.emptyArray = emptyArray;
        this.list = new FlipArray<>(emptyArray, emptyArray);
        //lists.set(this.list = this.emptyArray = emptyArray);
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
    public void forEachValue(Consumer<? super Y> action) {
        Y[] x = valueArray();
        for (Y y : x) {
            if (y !=null)
                action.accept(y);
        }
    }

    @Override
    public Y compute(X key, BiFunction<? super X, ? super Y, ? extends Y> remappingFunction) {
        throw new TODO();
    }

    @Override
    public Y computeIfAbsent(X key, Function<? super X, ? extends Y> mappingFunction) {
        final boolean[] changed = {false};
        Y prev = map.computeIfAbsent(key, (p) -> {
            Y next = mappingFunction.apply(p);
            if (next != p)
                changed[0] = true;
            return next;
        });
        if (changed[0]) {
            invalidate();
        }

//        Y prev = map.computeIfAbsent(key, mappingFunction);
//        invalidate();


        return prev;
    }


    public void invalidate() {
        list.invalidate();
    }

    public boolean whileEachValue(Predicate<? super Y> action) {
        Y[] x = valueArray();
        for (int i = 0, xLength = x.length; i < xLength; i++) {
            Y xi = x[i];
            if (xi!=null && !action.test(xi))
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
        return list.readValid(this::_valueArray);
    }

    private Y[] _valueArray(Y[] prev) {
        Y[] next;
        if (map instanceof ConcurrentOpenHashMap) { //HACK @Deprecated
            next = ((ConcurrentOpenHashMap<?, Y>) map).values(prev, i -> Arrays.copyOf(emptyArray, i));
        } else {
            int s = map.size();
            if (s== 0)
                return emptyArray;


            if (s > prev.length || (prev.length - extraThreshold > s)) {
                next = Arrays.copyOf(emptyArray, s);
            } else {
                next = prev;
            }

            final int[] j = {0};
            ((ConcurrentHashMap<X,Y>)map).forEachValue(1, x->{
                assert(x!=null);
                //if (x!=null) {
                    int jj = j[0];
                    if (jj < next.length) {
                        next[jj] = x;
                        j[0]++;
                    }
                //}
            });

            if (j[0] < next.length-1)
                Arrays.fill(next, j[0], next.length, null);

            //next = map.values().toArray(prev);
        }
        return next;
    }


    @Override
    public Y put(X key, Y value) {
        Y prev;
        if (value == null)
            prev = map.remove(key);
        else
            prev = map.put(key, value);
        if (prev!=value)
            invalidate();
        return prev;
    }

    public boolean removeIf(Predicate<? super Y> filter) {
        FasterList toRemove = new FasterList(1);
        map.forEach((k,v)->{
            if (filter.test(v))
                toRemove.add(k);
        });
        if (toRemove.isEmpty())
            return false;
        if (toRemove.anySatisfy((x -> map.remove(x)!=null)))
            invalidate();
        return true;
    }



    private final class MyAbstractList extends AbstractList {

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
}
