package spacegraph.space2d.container.grid;

import jcog.data.map.ConcurrentFastIteratingHashMap;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.AbstractMutableContainer;

import java.util.Collection;
import java.util.Objects;
import java.util.function.*;

public class MutableMapContainer<K, V> extends AbstractMutableContainer {

    protected final ConcurrentFastIteratingHashMap<K, CacheCell<K, V>> cache =
            new ConcurrentFastIteratingHashMap<>(new CacheCell[0]);

    @Override
    public void forEach(Consumer<Surface> each) {
        cache.forEachValue(e -> {
            Surface s = e.surface;
            if (s != null)
                each.accept(s);
        });
    }

    public void forEachVisible(Consumer<Surface> each) {
        forEach(x -> {
            if (x.visible())
                each.accept(x);
        });
    }

    public void forEachKeySurface(BiConsumer<? super K, Surface> each) {
        cache.forEach((k,v)->{
           each.accept(k, v.surface);
        });
    }

    public void forEachValue(Consumer<? super V> each) {
        cache.forEachValue(e -> {
            V s = e.value;
            if (s != null)
                each.accept(s);
        });
    }
//    public void forEachVisiblesValue(Consumer<? super V> each) {
//        cache.forEachValue(e -> {
//            V s = e.value;
//            if (s != null)
//                each.accept(s);
//        });
//    }

    @Override
    public boolean whileEach(Predicate<Surface> o) {
        return cache.whileEachValue(e -> {
            Surface s = e.surface;
            return s == null || o.test(s);
        });
    }

    @Override
    public boolean whileEachReverse(Predicate<Surface> o) {
        return cache.whileEachValueReverse(e -> {
            Surface s = e.surface;
            return s == null || o.test(s);
        });
    }

    @Override
    public int childrenCount() {
        return Math.max(1, cache.size()); //may not be accurate HACK force non-empty
    }

    @Override
    protected void clear() {
        cache.values().removeIf(e -> {
            e.clear();
            return true;
        });
        cache.invalidate();
    }

    @Nullable public V getValue(K x) {
        CacheCell<K, V> y = cache.get(x);
        if (y !=null)
            return y.value;
        return null;
    }


    public CacheCell<K, V> compute(K key, Function<V,V> builder) {
        CacheCell<K,V> entry = cache.computeIfAbsent(key, CacheCell::new);
        return update(key, entry, entry.update(builder));
    }

    public CacheCell<K, V> put(K key, V nextValue, BiFunction<K,V, Surface> renderer) {

        CacheCell<K,V> entry = cache.computeIfAbsent(key, CacheCell::new);

        return update(key, entry, entry.update(nextValue, renderer));

    }

    @Nullable
    public MutableMapContainer.CacheCell<K, V> update(K key, CacheCell<K, V> entry, boolean keep) {
        if (!keep) {
            remove(key);
            return null;
        } else {

            if (parent != null) {
                Surface es = entry.surface;
                if (es != null && es.parent == null)
                    es.start(this);
            }

            return entry;
        }
    }


    public boolean remove(K key) {
        CacheCell<K, V> entry = cache.remove(key);
        if (entry!=null) {
            entry.clear();
            return true;
        }
        return false;
    }

    public void getValues(Collection<V> l) {
        forEachValue(l::add);
    }

    /**
     * (key, value, surface) triple
     */
    public static class CacheCell<K, V> {

        public final K key;
        public transient volatile V value = null;
        public transient volatile Surface surface = null;

        CacheCell(K key) {
            this.key = key;
        }

        public void clear() {
            value = null;
            Surface es = this.surface;
            surface = null;
            if (es!=null)
                es.stop();
        }

        /** return true to keep or false to remove from the map */
        public boolean update(V nextValue, BiFunction<K, V, Surface> renderer) {
            Surface existingSurface = surface;

            boolean create = false, delete = false;

            if (existingSurface != null) {
                if (nextValue == null) {
                    delete = true;
                } else {
                    if (Objects.equals(value, nextValue)) {
                        //equal value, dont re-create surface
                    } else {
                        create = true; //replace
                    }
                }
                if (delete || create) {
                    //TODO different eviction policies
                    existingSurface.stop();
                }
            } else { //if (existingSurface == null) {
                if (nextValue!=null)
                    create = true;
                else
                    delete = true;
            }

            if (delete) {
                return false;
            } else if (create) {
                Surface newSurface = renderer.apply(key, this.value = nextValue);
                this.surface = newSurface;
            }

            return true;
        }

        /** return true to keep or false to remove from the map */
        public boolean update(Function<V,V> update) {
            V prev = value;

            V next = update.apply(prev);

            boolean create = false, delete = false;

            if (prev != null) {

                if (next == null) {
                    delete = true;
                } else {
                    if (Objects.equals(value, prev)) {
                        //equal value, dont re-create surface
                    } else {
                        create = true; //replace
                    }
                }
                if (delete || create) {
                    //TODO different eviction policies
                    if (surface!=null)
                        surface.stop();
                }
            } else { //if (existingSurface == null) {
                if (next!=null)
                    create = true;
                else
                    delete = true;
            }

            if (delete) {
                return false;
            } else if (create) {
                this.value = next;
                this.surface = (Surface)value;
            }

            return true;
        }
    }


}
