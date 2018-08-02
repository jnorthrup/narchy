//package jcog.data.graph;
//
//import jcog.data.iterator.ArrayIterator;
//import org.apache.commons.lang3.ArrayUtils;
//
//import java.util.Iterator;
//import java.util.LinkedHashSet;
//import java.util.function.Consumer;
//import java.util.stream.Stream;
//
///** buffers a lazily updated array-backed cache of the values
// * for fast iteration and streaming */
//public class FastIteratingHashSet<X> extends LinkedHashSet<X> {
//
//    volatile Object[] cache = ArrayUtils.EMPTY_OBJECT_ARRAY;
//
//    public FastIteratingHashSet() {
//        super();
//    }
//
//    public FastIteratingHashSet(int initialCap) {
//        super(initialCap);
//    }
//
//    @Override
//    public boolean add(X x) {
//        if (super.add(x)) {
//            cache = null;
//            return true;
//        }
//        return false;
//    }
//
//    @Override
//    public boolean remove(Object o) {
//        if (super.remove(o)) {
//            cache = null;
//            return true;
//        }
//        return false;
//    }
//
//
//    protected int update() {
//        int s = size();
//        if (cache == null) {
//            if (s == 0) {
//                cache = ArrayUtils.EMPTY_OBJECT_ARRAY;
//            } else {
//                Object[] c = cache = new Object[s];
//
//                Iterator<X> xx = super.iterator();
//                int i = 0;
//                while (xx.hasNext())
//                    c[i++] = xx.next();
//            }
//        }
//        return s;
//    }
//
//    @Override
//    public void clear() {
//        super.clear();
//        cache = ArrayUtils.EMPTY_OBJECT_ARRAY;
//    }
//
//    @Override
//    public void forEach(Consumer<? super X> action) {
//        if (update() > 0) {
//            for (Object x : cache) {
//                action.accept((X) x);
//            }
//        }
//    }
//
//    @Override
//    public Iterator iterator() {
//        update();
//        return ArrayIterator.get(cache);
//    }
//
//    @Override
//    public Stream<X> stream() {
//        switch (update()) {
//            case 0:
//                return Stream.empty();
//            case 1:
//                return Stream.of((X)cache[0]);
//            default:
//                return Stream.of(cache).map(x -> (X) x);
//        }
//    }
//}
