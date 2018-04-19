package spacegraph.space2d.container.grid;

import jcog.data.map.ConcurrentFastIteratingHashMap;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.AbstractMutableContainer;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class MutableMapContainer<K, V> extends AbstractMutableContainer {

    protected final ConcurrentFastIteratingHashMap<K, CacheCell<K, V>> cache =
            new ConcurrentFastIteratingHashMap<>(new CacheCell[0]);

    @Override
    public void forEach(Consumer<Surface> o) {
        cache.forEachValue(e -> {
            Surface s = e.surface;
            if (s != null)
                o.accept(s);
        });
    }

    @Override
    public boolean whileEach(Predicate<Surface> o) {
        return cache.whileEachValue(e -> {
            Surface s = e.surface;
            return s != null ? o.test(s) : true;
        });
    }

    @Override
    public boolean whileEachReverse(Predicate<Surface> o) {
        return cache.whileEachValueReverse(e -> {
            Surface s = e.surface;
            return s != null ? o.test(s) : true;
        });
    }

    @Override
    public int childrenCount() {
        return Math.max(1, cache.size()); //may not be accurate HACK force non-empty
    }

    @Override
    protected void clear() {
        cache.values().removeIf(e -> {
            remove(e);
            return true;
        });
        cache.invalidate();
    }

    public CacheCell<K, V> put(K key, V nextValue, BiFunction<K,V, Surface> renderer) {

        CacheCell<K,V> entry = cache.computeIfAbsent(key, CacheCell::new);

        Surface existingSurface = entry.surface;

        boolean create = false, delete = false;

        if (existingSurface != null) {
            if (nextValue == null) {
                delete = true;
            } else {
                if (Objects.equals(entry.value, nextValue)) {
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
            remove(entry);
        } else if (create) {
            Surface newSurface = renderer.apply(key, entry.value = nextValue);
            entry.surface = newSurface;
            if (parent!=null) {
                entry.surface.start(this);
            }
        }

        return entry;
    }


    public boolean remove(K key) {
        CacheCell<K, V> entry = cache.remove(key);
        if (entry!=null) {
            remove(entry);
            return true;
        }
        return false;
    }

    public void remove(CacheCell<K, V> entry) {
        Surface es = entry.surface;
        if (es!=null) {
            es.stop();
        }
        entry.clear();
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
            surface = null;
            value = null;
        }
    }
}
