package jcog.data.map;

import jcog.data.pool.DequePool;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Objects;
import java.util.function.*;

/**
 * concurrent map wrapping key,value pairs in cell instances
 * that are recycled in an internal pool.
 *
 * this can be useful for managing maps with set-like semantics
 *  --add
 *  --set
 *  --remove
 *
 * uses ConcurrentFastIteratingHashMap which maintains an additional
 * list of items that is eventually updated with any map operations.
 *
 * bulk operations should defer invalidation instead of invalidating
 * on each item.
 */
public class CellMap<K, V> {

    public final ConcurrentFastIteratingHashMap<K, CacheCell<K, V>> cache =
            new ConcurrentFastIteratingHashMap<>(new CacheCell[0]);

    public final DequePool<CacheCell<K, V>> cellPool = new DequePool<>(32) {
        @Override
        public CacheCell<K, V> create() {
            return newCell();
        }
    };

    public CellMap() {

    }

    protected CacheCell<K,V> newCell() {
        return new CacheCell<>();
    }

    public final void forEachCell(Consumer<? super CacheCell<K,V>> each) {
        cache.forEachValue(each);
    }

    public final void forEachValue(Consumer<? super V> each) {
        forEachCell(e -> {
            V s = e.value;
            if (s != null)
                each.accept(s);
        });
    }

    public void forEachKeyValue(BiConsumer<K,? super V> each) {
        forEachCell(e -> {
            V s = e.value;
            if (s != null)
                each.accept(e.key, s);
        });
    }

    @Nullable
    public CellMap.CacheCell<K, V> update(K key, CellMap.CacheCell<K, V> entry, boolean keep) {
        if (!keep) {
            remove(key);
            return null;
        } else {
            added(entry);
            return entry;
        }
    }

    protected void added(CacheCell<K, V> entry) {
        
    }

    public boolean whileEach(Predicate<CacheCell<K,V>> o) {
        return cache.whileEachValue(o::test);
    }

    public boolean whileEachReverse(Predicate<CacheCell<K,V>> o) {
        return cache.whileEachValueReverse(o::test);
    }

    public void removeAll(Iterable<K> x) {
        final boolean[] changed = {false};
        x.forEach(xx -> changed[0] |= removeSilently(xx));
        if (changed[0])
            invalidated();
    }

    @Nullable
    public V getValue(Object x) {
        CacheCell<K, V> y = cache.get(x);
        if (y != null)
            return y.value;
        return null;
    }

    public CacheCell<K, V> compute(K key, Function<V, V> builder) {
        CacheCell<K, V> entry = cache.computeIfAbsent(key, k -> cellPool.get());
        return update(key, entry, entry.update(key, builder));
    }
    public CacheCell<K, V> compute(K key, BiFunction<K, V, V> builder) {
        CacheCell<K, V> entry = cache.computeIfAbsent(key, k -> cellPool.get());
        return update(key, entry, entry.update(key, builder));
    }


    public boolean remove(K key) {
        CacheCell<K, V> entry = cache.remove(key);
        if (entry != null) {
            removed(entry);
            invalidated();
            return true;
        }
        return false;
    }

    /** removes without immediately signaling invalidation, for use in batch updates */
    public boolean removeSilently(K key) {
        CacheCell<K, V> entry = cache.remove(key);
        if (entry != null) {
            removed(entry);
            return true;
        }
        return false;
    }

    protected final void removed(CacheCell<K, V> entry) {
        unmaterialize(entry);
        entry.clear();
        cellPool.put(entry);
    }

    protected void unmaterialize(CacheCell<K, V> entry) {

    }

    protected void invalidated() {
        cache.invalidate();
    }

    public void getValues(Collection<V> l) {
        forEachValue(l::add);
    }

    public int size() {
        return this.cache.size();
    }

    public Collection<CacheCell<K,V>> cells() {
        return cache.values();
    }

    public void clear() {
        cache.removeIf(e -> {
            removed(e);
            return true;
        });
    }

    @Nullable public V get(Object from) {
        CacheCell<K, V> v = cache.get(from);
        return v != null ? v.value : null;
    }

    /**
     * (key, value, surface) triple
     */
    public static class CacheCell<K, V> {

        public transient volatile K key;
        public transient volatile V value;

        protected CacheCell() {

        }

        protected void set(V next) {
            this.value = next;
        }

        public void clear() {
            value = null;
        }


        public final boolean update(K nextKey, Function<V, V> update) {
            return update(nextKey, (k, v) -> update.apply(v));
        }

        /**
         * return true to keep or false to remove from the map
         */
        public boolean update(K nextKey, BiFunction<K, V, V> update) {
            this.key = nextKey;
            V prev = value;

            V next = update.apply(nextKey, prev);

            boolean create = false, delete = false;

            if (prev != null) {

                if (next == null) {
                    delete = true;
                } else {
                    if (Objects.equals(value, prev)) {
                        
                    } else {
                        delete = true;
                        create = true; 
                    }
                }
            } else {
                if (next == null) {
                    delete = true;
                } else {
                    create = true;
                }
            }



            if (create) {
                set(next);
            }

            return !delete;
        }

    }
}
