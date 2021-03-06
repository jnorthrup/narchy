package jcog.version;

import jcog.TODO;
import jcog.data.list.FasterList;
import jcog.data.set.ArrayUnenforcedSet;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;


public class VersionMap<X, Y> implements Map<X, Y>, Function<X,Versioned<Y>> {

    public final Versioning context;
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
        throw new UnsupportedOperationException();
    }

    public boolean replace(BiFunction<? super X, ? super Y, ? extends Y> function) {
        List<Object[]> replacements = new FasterList();
        for (Entry<X, Versioned<Y>> entry : map.entrySet()) {
            X v = entry.getKey();
            Versioned<Y> val = entry.getValue();
            if (val != null) {
                Y x = val.get();
                if (x != null) {
                    Y y = function.apply(v, x);
                    if (!x.equals(y)) {
                        replacements.add(new Object[]{v, y});
                    }
                }
            }
        }
        if (!replacements.isEmpty()) {
            for (Object[] r : replacements) {
                if (!set((X) r[0], (Y) r[1])) {
                    return false;
                }
            }
            return true;
        }
        return true;
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

    @Override
    public @Nullable Y remove(Object key) {
        Versioned<Y> x = map.remove(key);
        return x != null ? x.get() : null;
    }

    @Override
    public void putAll(Map<? extends X, ? extends Y> map) {
        throw new TODO();
    }

    @Override
    public boolean remove(Object key, Object value) {
        throw new UnsupportedOperationException();
    }


    /** clears the unifier while simultaneously fast iterating assignments (in reverse) before they are pop()'d */
    @Override public final void clear() {
        throw new UnsupportedOperationException();
    }

    @Override public Versioned<Y> apply(X x) {
        return new KeyUniVersioned<>(x);
    }

    @Override
    public int size() {
        return context.size;
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    /** copied from AbstractMap.java */
    @Override public String toString() {
        Iterator<Entry<X, Y>> i = this.entrySet().iterator();
        if (!i.hasNext()) {
            return "{}";
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append('{');

            while(true) {
                Entry<X, Y> e = i.next();
                X key = e.getKey();
                Y value = e.getValue();
                sb.append(key == this ? "(this Map)" : key);
                sb.append('=');
                sb.append(value == this ? "(this Map)" : value);
                if (!i.hasNext()) {
                    return sb.append('}').toString();
                }

                sb.append(',').append(' ');
            }
        }
    }

    /**
     * avoid using this if possible because it involves transforming the entries from the internal map to the external form
     */
    @Override
    public Set<Entry<X, Y>> entrySet() {
        int s = size();
        if (s == 0)
            return Set.of();
        else {

            ArrayUnenforcedSet<Entry<X, Y>> e = new ArrayUnenforcedSet<>(0, new Entry[s]);
            for (Entry<X, Versioned<Y>> entry : map.entrySet()) {
                X k = entry.getKey();
                Versioned<Y> v = entry.getValue();
                Y vv = v.get();
                if (vv != null)
                    e.add(new AbstractMap.SimpleImmutableEntry<>(k, vv));
            }
            return e;
        }
    }

    @Override
    public Set<X> keySet() {
        int s = size();
        if (s == 0)
            return Collections.EMPTY_SET;
        //else if (s == 1) ...
        else {
            ArrayUnenforcedSet<X> e = new ArrayUnenforcedSet(0, new Object[s]);
            for (Entry<X, Versioned<Y>> entry : map.entrySet()) {
                X k = entry.getKey();
                Versioned<Y> v = entry.getValue();
                if (v != null)
                    e.add(k);
            }
            return e;
        }
    }

    /** @noinspection SimplifyStreamApiCallChains*/
    @Override
    public Collection<Y> values() {
        List<Y> list = new ArrayList<>();
        for (Entry<X, Y> xyEntry : entrySet()) {
            Y value = xyEntry.getValue();
            list.add(value);
        }
        return list; //HACK
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
        return getOrCreateIfAbsent(key).set(value, context);
    }

    public final Versioned<Y> getOrCreateIfAbsent(X key) {
        return map.computeIfAbsent(key, this);
    }


    public void forEach(BiConsumer<? super X, ? super Y> each) {
        for (Entry<X, Versioned<Y>> entry : map.entrySet()) {
            X x = entry.getKey();
            Versioned<Y> yy = entry.getValue();
            Y y = yy.get();
            if (y != null)
                each.accept(x, y);
        }
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

//    /** TODO test */
//    public boolean compute(/*X*/X key, UnaryOperator<Y> f) {
//        final boolean[] result = {false};
//        map.compute(key, (k, v)->{
//
//            Y prev, next;
//
//            prev = v != null ? v.get() : null;
//
//            next = f.apply(prev);
//
//            result[0] = (next != null) &&
//                    context.set((v != null ? v : (v = newEntry(k))), next);
//
//            return v;
//        });
//        return result[0];
//    }



    @Override
    public final boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object o) {
        throw new TODO();
    }


    public static final VersionMap Empty = new VersionMap(new Versioning<>(1)) {

        @Override
        public Object get(Object key) {
            return null;
        }
    };



}
