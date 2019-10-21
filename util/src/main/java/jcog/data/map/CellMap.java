package jcog.data.map;

import jcog.data.pool.MetalPool;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Optional;
import java.util.function.*;

/**
 * concurrent map wrapping key,value pairs in cell instances
 * that are recycled in an internal pool.
 *
 * this can be useful for managing maps with setAt-like semantics
 *  --addAt
 *  --setAt
 *  --remove
 *
 * uses ConcurrentFastIteratingHashMap which maintains an additional
 * list of items that is eventually updated with any map operations.
 *
 * bulk operations should defer invalidation instead of invalidating
 * on each item.
 */
public class CellMap<K, V> {

    public final ConcurrentFastIteratingHashMap<K, CacheCell<K, V>> map =
            new ConcurrentFastIteratingHashMap<>(new CacheCell[0]);

    public final MetalPool<CacheCell<K, V>> cellPool = new MetalPool<>() {
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
        map.forEachValue(each);
    }

    public final void forEachValue(Consumer<? super V> each) {
        map.forEachValueWith((e, EACH) -> {
            V s = e.value;
            if (s != null)
                EACH.accept(s);
        }, each);
    }

    public void forEachKeyValue(BiConsumer<K,? super V> each) {
        map.forEachValueWith((e, EACH) -> {
            V s = e.value;
            if (s != null)
                EACH.accept(e.key, s);
        }, each);
    }

    public @Nullable CellMap.CacheCell<K, V> update(K key, CellMap.CacheCell<K, V> entry, boolean keep) {
        if (keep) {
            //added or continues
            return entry;
        } else {
            remove(key);
            return null;
        }
    }



    public boolean whileEach(Predicate<CacheCell<K,V>> o) {
        return map.whileEachValue(o);
    }

    public boolean whileEachReverse(Predicate<CacheCell<K,V>> o) {
        return map.whileEachValueReverse(o);
    }

    public void removeAll(Iterable<K> x) {
        boolean[] changed = {false};
        for (K xx : x) {
            changed[0] |= removeSilently(xx);
        }
        if (changed[0])
            invalidated();
    }

    public @Nullable V getValue(Object x) {
        CacheCell<K, V> y = map.get(x);
        return y != null ? y.value : null;
    }

    public CacheCell<K, V> compute(K key, BiFunction<K, V, V> builder) {
        CacheCell<K, V> entry = map.computeIfAbsent(key, new Function<K, CacheCell<K, V>>() {
            @Override
            public CacheCell<K, V> apply(K k) {
                return cellPool.get();
            }
        });
        entry.update(key, builder);
        return update(key, entry, entry.key!=null);
    }

    public CacheCell<K, V> compute(K key, UnaryOperator<V> builder) {
        return compute(key, new BiFunction<K, V, V>() {
            @Override
            public V apply(K z, V w) {
                return builder.apply(w);
            }
        });
    }

    public CacheCell<K, V> computeIfAbsent(K key, Function<K, V> builder) {
        return compute(key, new BiFunction<K, V, V>() {
            @Override
            public V apply(K z, V w) {
                if (w == null) return builder.apply(z);
                else return w;
            }
        });
    }



    public CacheCell<K, V> remove(Object key) {
        CacheCell<K, V> entry = map.remove(key);
        if (entry != null) {
            removed(entry);
            invalidated();
            return entry;
        }
        return null;
    }

    /** removes without immediately signaling invalidation, for use in batch updates */
    public boolean removeSilently(K key) {
        CacheCell<K, V> entry = map.remove(key);
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
        map.invalidate();
    }

    public void getValues(Collection<V> l) {
        forEachValue(l::add);
    }

    public int size() {
        return this.map.size();
    }

    public Collection<CacheCell<K,V>> cells() {
        return map.values();
    }

    public void clear() {
        map.removeIf(new Predicate<CacheCell<K, V>>() {
            @Override
            public boolean test(CacheCell<K, V> e) {
                CellMap.this.removed(e);
                return true;
            }
        });
    }

    public @Nullable V get(Object from) {
        CacheCell<K, V> v = map.get(from);
        return v != null ? v.value : null;
    }

    /** find first corresponding key to the provided value */
    public @Nullable K first(Predicate v) {
        for (CacheCell<K, V> kvCacheCell : map.valueArray()) {
            if (v.test(kvCacheCell.value)) {
                return Optional.of(kvCacheCell).map(new Function<CacheCell<K, V>, K>() {
                    @Override
                    public K apply(CacheCell<K, V> c) {
                        return c.key;
                    }
                }).orElse(null);
            }
        }
        return Optional.<CacheCell<K, V>>empty().map(new Function<CacheCell<K, V>, K>() {
            @Override
            public K apply(CacheCell<K, V> c) {
                return c.key;
            }
        }).orElse(null);
    }

    public @Nullable K firstByIdentity(V x) {
        for (CacheCell<K, V> kvCacheCell : map.valueArray()) {
            if (kvCacheCell.value == x) {
                return Optional.of(kvCacheCell).map(new Function<CacheCell<K, V>, K>() {
                    @Override
                    public K apply(CacheCell<K, V> c) {
                        return c.key;
                    }
                }).orElse(null);
            }
        }
        return Optional.<CacheCell<K, V>>empty().map(new Function<CacheCell<K, V>, K>() {
            @Override
            public K apply(CacheCell<K, V> c) {
                return c.key;
            }
        }).orElse(null);
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
            key = null;
            value = null;
        }


        public final void update(K nextKey, UnaryOperator<V> update) {
            update(nextKey, new BiFunction<K, V, V>() {
                @Override
                public V apply(K k, V v) {
                    return update.apply(v);
                }
            });
        }


        public void update(K nextKey, BiFunction<K, V, V> update) {

            V prev = value;

            V next = update.apply(nextKey, prev);
            if (next == prev) {
                key = next == null ? null : nextKey;
            } else {

                boolean create = false, delete = false;

                if (prev != null) {
                    if (next == null) {
                        delete = true;
                    } else {
                        create = delete = true;
                    }
                } else {
                    create = true;
                }

                if (delete) {
                    key = null;
                }

                if (create) {
                    key = nextKey;
                    set(next);
                }

            }
        }

    }
}
