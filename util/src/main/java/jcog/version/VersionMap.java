package jcog.version;

import jcog.data.set.ArrayUnenforcedSet;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.jetbrains.annotations.Nullable;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Function;


public class VersionMap<X, Y> extends AbstractMap<X, Y> {

    private final Versioning context;
    public final Map<X, Versioned<Y>> map;
    private final int elementStackSizeDefault;


    public VersionMap(Versioning context) {
        this(context, 0);
    }

    private VersionMap(Versioning context, int mapCap) {
        this(context, mapCap, 1);
    }

    /**
     * @param context
     * @param mapCap  initial capacity of map (but can grow
     * @param eleCap  initial capacity of map elements (but can grow
     */
    private VersionMap(Versioning context, int mapCap, int eleCap) {
        this(context,
                
                
                new UnifiedMap(mapCap)
                , eleCap
        );
    }

    public VersionMap(Versioning<Y> context, Map<X, Versioned<Y>/*<Y>*/> map, int elementStackSizeDefault) {
        this.context = context;
        this.map = map;
        this.elementStackSizeDefault = elementStackSizeDefault;
    }


    @Nullable
    @Override
    public Y remove(Object key) {
        Versioned<Y> x = map.remove(key);
        return x != null ? x.get() : null;
    }

    @Override
    public boolean remove(Object key, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public final int size() {
        int count = 0;
        for (Versioned<Y> e : map.values()) {
            if (e.get()!=null)
                count++;
        }
        return count;
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }


    /**
     * avoid using this if possible because it involves transforming the entries from the internal map to the external form
     */
    @Override
    public Set<Entry<X, Y>> entrySet() {
        ArrayUnenforcedSet<Entry<X, Y>> e = new ArrayUnenforcedSet<>();
        map.forEach((k, v) -> {
            Y vv = v.get();
            if (vv != null) {
                
                e.add(new SimpleEntry<>(k, vv));
            }
        });
        return e;
    }



















    /**
     * records an assignment operation
     * follows semantics of set()
     */
    @Override
    public final Y put(X key, Y value) {
        throw new UnsupportedOperationException("use tryPut(k,v)");
    }

    public boolean tryPut(X key, Y value) {
        return getOrCreateIfAbsent(key).set(value) != null;
    }

    public final Versioned<Y> getOrCreateIfAbsent(X key) {
        return map.computeIfAbsent(key, this::newEntry);






    }

    protected Versioned<Y> newEntry(X x) {
        return new Versioned<>(context, elementStackSizeDefault);
        
        
    }

    public void forEach(BiConsumer<? super X, ? super Y> each) {
        map.forEach((x,yy)->{
            Y y = yy.get();
            if (y!=null)
                each.accept(x, y);
        });
    }


    public boolean forEachVersioned(BiPredicate<? super X, ? super Y> each) {
        Set<Entry<X, Versioned<Y>>> ee = map.entrySet();
        for (Entry<X, Versioned<Y>> e : ee) {
            Y y = e.getValue().get();
            if (y != null) {
                if (!each.test(e.getKey(), y)) {
                    return false;
                }
            }
        }
        return true;
    }














    @Override
    public Y get(/*X*/Object key) {
        Versioned<Y> v = map.get(key);
        return v != null ? v.get() : null;
    }

    /** TODO test */
    public boolean compute(/*X*/X key, Function<Y,Y> f) {
        final boolean[] result = {false};
        map.compute(key, (k, v)->{

            Y prev, next;

            prev = v == null ? null : v.get();

            next = f.apply(prev);

            if (next!=null) {
                if (v == null)
                    v = newEntry(k);
                result[0] = v.set(next)!=null;
            } else {
                result[0] = false;
            }
            return v;
        });
        return result[0];
    }



    @Override
    public final boolean containsKey(Object key) {
        return map.containsKey(key);
    }


    @Override
    public Set<X> keySet() {
        throw new UnsupportedOperationException(); 
        
    }

    public static final VersionMap Empty = new VersionMap(new Versioning<>(1), 0, 0) {

        @Override
        public boolean tryPut(Object key, Object value) {
            return false;
        }

        @Override
        public Object get(Object key) {
            return null;
        }
    };



}
