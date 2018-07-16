package jcog.data.map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;


/**
 * adapter to a Map for coordinating changes in a Map with another Collection
 */
public abstract class CollectorMap<K, V> {


    public final Map<K, V> map;

    protected CollectorMap(Map<K, V> map) {
        this.map = map;
    }

    @Nullable
    abstract public K key(V v);


    /**
     * implementation for removing the value to another collecton (called internally)
     */
    protected abstract void removeItem(V e);

    public final void forEach(BiConsumer<K, V> each) {
        map.forEach(each);
    }


    @Nullable
    public V remove(/*@NotNull*/ K x) {
        V removed = map.remove(x);
        if (removed != null)
            removeItem(removed);
        return removed;
    }


    public void clear() {
        map.clear();
    }

    @Nullable
    public final V get(Object key) {
        return map.get(key);
    }


    public boolean containsKey(K name) {
        return map.containsKey(name);
    }

    @NotNull
    public Set<K> keySet() {
        return map.keySet();
    }

    @NotNull
    public Collection<V> values() {
        return map.values();
    }


}
