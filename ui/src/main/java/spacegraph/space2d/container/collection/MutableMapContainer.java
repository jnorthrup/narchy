package spacegraph.space2d.container.collection;

import jcog.data.map.CellMap;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.AbstractMutableContainer;

import java.util.Collection;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class MutableMapContainer<K, V> extends AbstractMutableContainer {

    /**
     * "cells" of the container; maintain the mapping between the indexed keys and their "materialized" representations or projections
     */
    protected final CellMap<K, V> cells = new CellMap<>() {
        @Override
        protected CacheCell<K, V> newCell() {
            return new SurfaceCacheCell<>();
        }

        @Override
        protected final void unmaterialize(CacheCell<K, V> entry) {
            MutableMapContainer.this.unmaterialize(entry.value);
            super.unmaterialize(entry);
        }
        //        @Override
//        protected void added(CacheCell<K, V> entry) {
//            if (parent == null)
//                return;
//
//            Surface es = ((SurfaceCacheCell) entry).surface;
//            //if (es != null && es.parent == null)
//            es.start(MutableMapContainer.this);
//        }


//        @Override
//        protected void invalidated() {
//            super.invalidated();
//        }
    };


    protected void unmaterialize(V v) {

    }


    @Override
    public void forEach(Consumer<Surface> each) {
        cells.forEachCell(e -> {
            if (e == null)
                throw new NullPointerException();

            Surface s = ((SurfaceCacheCell) e).surface;
            if (s != null)
                each.accept(s);
        });
    }


//    public void forEachVisible(Consumer<Surface> each) {
//        forEach(x -> {
//            if (x.visible())
//                each.accept(x);
//        });
//    }
//
//    public void forEachKeySurface(BiConsumer<? super K, Surface> each) {
//        cellMap.forEachCell((cell) -> {
//            Surface ss = ((SurfaceCacheCell) cell).surface;
//            if (ss != null)
//                each.accept(cell.key, ss);
//        });
//    }

    public void forEachValue(Consumer<? super V> each) {
        cells.forEachValue(each);
    }


    @Override
    protected void doLayout(int dtMS) {

    }

    @Override
    public int childrenCount() {
        return Math.max(1, cells.size());
    }

    @Override
    protected void clear() {
        cells.clear();
    }

    protected void removeAll(Iterable<K> x) {
        cells.removeAll(x);
    }

    public Collection<K> keySet() {
        return cells.map.keySet();
    }

    @Nullable
    public V getValue(K x) {
        return cells.getValue(x);
    }

    public CellMap.CacheCell<K, V> compute(K key, Function<V, V> builder) {
        return cells.compute(key, builder);
    }

    public CellMap.CacheCell<K, V> computeIfAbsent(K key, Function<K, V> builder) {
        return cells.computeIfAbsent(key, builder);
    }

    protected CellMap.CacheCell<K, V> put(K key, V nextValue, BiFunction<K, V, Surface> renderer) {

        CellMap.CacheCell<K, V> entry = cells.map.computeIfAbsent(key, k -> cells.cellPool.get());

        ((SurfaceCacheCell) entry).update(key, nextValue, renderer);

        return cells.update(key, entry, entry.key != null);

    }


    public V remove(Object key) {
        CellMap.CacheCell<K, V> c = cells.remove(key);
        return c!=null ? c.value : null;
    }

    @Override
    public boolean removeChild(Surface s) {
        K k = cells.firstByValue(x -> s == x);
        return k!=null && remove(k)!=null;
    }

    protected boolean removeSilently(K key) {
        return cells.removeSilently(key);
    }


    public void getValues(Collection<V> l) {
        cells.getValues(l);
    }

    @Override
    public boolean whileEach(Predicate<Surface> o) {
        return cells.whileEach(e -> {
            Surface s = ((SurfaceCacheCell) e).surface;
            return s == null || o.test(s);
        });
    }

    @Override
    public boolean whileEachReverse(Predicate<Surface> o) {
        return cells.whileEachReverse(e -> {
            Surface s = ((SurfaceCacheCell) e).surface;
            return s == null || o.test(s);
        });
    }


    public static class SurfaceCacheCell<K, V> extends CellMap.CacheCell<K, V> {

        public transient volatile Surface surface = null;

        @Override
        public void clear() {
            super.clear();

            Surface s = surface;
            surface = null;

            if (s != null) {
                if (s.parent != null)
                    s.stop();
            }
        }

        @Override
        protected void set(V next) {
            super.set(next);
            if (next instanceof Surface) {
                setSurface((Surface) next);
            }
        }

        private void setSurface(Surface next) {
            assert (surface == null);
            if (next != surface) {
                if (this.surface != null) {
                    this.surface.stop();
                }
                this.surface = (Surface) next;
            }
        }

        /**
         * return true to keep or false to remove from the map
         */
        void update(K nextKey, V nextValue, BiFunction<K, V, Surface> renderer) {

            if (nextValue == null) {
                this.key = null;

            } else {

                if (!Objects.equals(this.value, nextValue) || surface == null) {
                    this.key = null;
                    Surface nextSurface = renderer.apply(nextKey, nextValue);
                    set(nextValue);
                    setSurface(nextSurface);
                }

                this.key = nextKey; //ready
            }

        }
    }
}
