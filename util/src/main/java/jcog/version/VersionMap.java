package jcog.version;

import jcog.data.set.ArrayUnenforcedSet;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.jetbrains.annotations.Nullable;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;


public class VersionMap<X, Y> extends AbstractMap<X, Y> {

    protected final Versioning context;
    public final Map<X, Versioned<Y>> map;


    /**
     * @param context
     * @param mapCap  initial capacity of map (but can grow
     */
    public VersionMap(Versioning<Y> context) {
        this(context, new UnifiedMap<>(0));
    }

    public VersionMap(Versioning<Y> context, Map<X, Versioned<Y>/*<Y>*/> map) {
        this.context = context;
        this.map = map;
    }

    @Override
    public void replaceAll(BiFunction<? super X, ? super Y, ? extends Y> function) {
        map.forEach((v,val)->{
           if (val!=null) {
               Y x = val.get();
               if (x!=null) {
                   Y y = function.apply(v, x);
                   if (x != y) {
                       val.replace(y, context);
                   }
               }
           }
        });
    }

//    public boolean tryReplaceAll(BiFunction<? super X, ? super Y, ? extends Y> function) {
//        final boolean[] ok = {true};
//        map.forEach((v,val)->{
//            if (ok[0] && val!=null) {
//                Y x = val.get();
//                if (x!=null) {
//                    Y y = function.apply(v, x);
//                    if (x != y) {
//                        if (!val.replace(y))
//                            ok[0] = false;
//                    }
//                }
//            }
//        });
//        return ok[0];
//    }

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


    /** clears the unifier while simultaneously fast iterating assignments (in reverse) before they are pop()'d */
    @Override public final void clear() {
        throw new UnsupportedOperationException();
    }


    @Override
    public final int size() {
        int cs = context.size;
//        if (cs <= 1) // || itemVersions == 1)
            return cs;


//        int count = 0;
//        for (Versioned<Y> e : map.values()) {
//            if (e.get()!=null)
//                count++;
//        }
//        return count;
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
        int s = size();
        if (s == 0)
            return Set.of();

        ArrayUnenforcedSet<Entry<X, Y>> e = new ArrayUnenforcedSet<>(0, new Entry[s]);
        map.forEach((k, v) -> {
            Y vv = v.get();
            if (vv != null) {
                e.add(new SimpleEntry<>(k, vv));
            }
        });

        return e;
    }

    @Override
    public Set<X> keySet() {
        int s = size();
        if (s == 0)
            return Set.of();
        //else if (s == 1) ...
        else {
            ArrayUnenforcedSet e = new ArrayUnenforcedSet(0, new Object[s]);
            map.forEach((k, v) -> {
                if (v!=null)
                    e.add(k);
            });
            return e;
        }
    }

















    /**
     * records an assignment operation
     * follows semantics of setAt()
     */
    @Override
    public final Y put(X key, Y value) {
        throw new UnsupportedOperationException("use force(k,v)");
    }

    public final boolean force(X key, Y value) {
        return getOrCreateIfAbsent(key).replace(value, context);
    }

    public final boolean set(X key, Y value) {
        return context.set(getOrCreateIfAbsent(key), value);
    }


//    public boolean tryPut(X key, Supplier<Y> value) {
//        return getOrCreateIfAbsent(key).setOnly(value) != null;
//    }

    public final Versioned<Y> getOrCreateIfAbsent(X key) {
        return map.computeIfAbsent(key, this::newEntry);
    }

    protected Versioned<Y> newEntry(X x) {
        return new KeyUniVersioned<>(x);
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

            prev = v != null ? v.get() : null;

            next = f.apply(prev);

            result[0] = (next != null) &&
                    context.set((v != null ? v : (v = newEntry(k))), next);

            return v;
        });
        return result[0];
    }



    @Override
    public final boolean containsKey(Object key) {
        return map.containsKey(key);
    }




    public static final VersionMap Empty = new VersionMap(new Versioning<>(1)) {

        @Override
        public Object get(Object key) {
            return null;
        }
    };



}
