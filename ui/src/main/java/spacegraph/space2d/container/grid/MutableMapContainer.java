package spacegraph.space2d.container.grid;

import jcog.data.map.CellMap;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.AbstractMutableContainer;

import java.util.Collection;
import java.util.Objects;
import java.util.function.*;

public class MutableMapContainer<K, V> extends AbstractMutableContainer {

    protected final CellMap<K,V> cellMap = new CellMap<>() {
        @Override
        protected CacheCell<K, V> newCell() {
            return new SurfaceCacheCell<>();
        }

        @Override
        protected void added(CacheCell<K, V> entry) {
            if (parent != null) {
                Surface es = ((SurfaceCacheCell)entry).surface;
                if (es != null && es.parent == null)
                    es.start(MutableMapContainer.this);
            }
        }

        @Override
        protected void invalidated() {
            super.invalidated();
            invalidate();
        }
    };

    @Override
    public void forEach(Consumer<Surface> each) {
        cellMap.forEachCell(e -> {
            Surface s = ((SurfaceCacheCell)e).surface;
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
        cellMap.forEachCell((cell)->{
            Surface ss = ((SurfaceCacheCell)cell).surface;
            if (ss!=null)
                each.accept(cell.key, ss);
        });
    }

    public void forEachValue(Consumer<? super V> each) {
        cellMap.forEachValue(each);
    }










    @Override
    public int childrenCount() {
        return Math.max(1, cellMap.size()); 
    }

    @Override
    protected void clear() {
        cellMap.clear();
    }

    protected void removeAll(Iterable<K> x) {
        cellMap.removeAll(x);
    }

    @Nullable public V getValue(K x) {
        return cellMap.getValue(x);
    }

    public CellMap.CacheCell<K, V> compute(K key, Function<V,V> builder) {
        return cellMap.compute(key, builder);
    }

    CellMap.CacheCell put(K key, V nextValue, BiFunction<K, V, Surface> renderer) {

        CellMap.CacheCell entry = cellMap.cache.computeIfAbsent(key, k-> cellMap.cellPool.get());
        return cellMap.update(key, entry, ((SurfaceCacheCell)entry).update(key, nextValue, renderer));

    }







    public boolean remove(K key) {
        return cellMap.remove(key);
    }

    public boolean remove(K key, boolean invalidate) {
        return cellMap.remove(key, invalidate);
    }

    void invalidate() {
        cellMap.cache.invalidate();
    }

    public void getValues(Collection<V> l) {
        cellMap.getValues(l);
    }

    @Override
    public boolean whileEach(Predicate<Surface> o) {
        return cellMap.whileEach(e -> {
            Surface s = ((SurfaceCacheCell)e).surface;
            return s == null || o.test(s);
        });
    }

    @Override
    public boolean whileEachReverse(Predicate<Surface> o) {
        return cellMap.whileEachReverse(e -> {
            Surface s = ((SurfaceCacheCell)e).surface;
            return s == null || o.test(s);
        });
    }


    public static class SurfaceCacheCell<K,V> extends CellMap.CacheCell<K,V> {

        transient Surface surface = null;

        SurfaceCacheCell() {
            super();
        }

        @Override
        protected void set(V next) {

            if (next instanceof Surface)
                surface = (Surface)next;

            super.set(next);
        }

        @Override
        public void clear() {
            super.clear();

            Surface es = this.surface;
            surface = null;
            if (es != null) {
                es.stop();
                es.hide();
            }

        }
        /**
         * return true to keep or false to remove from the map
         */
        boolean update(K nextKey, V nextValue, BiFunction<K, V, Surface> renderer) {

            this.key = nextKey;

            Surface existingSurface = surface;

            boolean create = false, delete = false;

            if (existingSurface != null) {
                if (nextValue == null) {
                    delete = true;
                } else {
                    if (Objects.equals(value, nextValue)) {
                        
                    } else {
                        create = true; 
                    }
                }
                if (delete || create) {
                    
                    existingSurface.stop();
                }
            } else { 
                if (nextValue != null)
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

    }
}
