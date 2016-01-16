package nars.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * adapter to a Map for coordinating changes in a Map with another Collection
 */
public abstract class CollectorMap<K, V>  {

    public final Map<K, V> map;

    protected CollectorMap(Map<K, V> map) {
        this.map = map;
    }

    abstract public K key(V v);

    @Override
    public String toString() {
        return map.toString();
    }

    /**
     * implementation for adding the value to another collecton (called internally)
     */
    protected abstract V addItem(V e);

    /**
     * implementation for removing the value to another collecton (called internally)
     */
    @Nullable
    protected abstract V removeItem(V e);

    public final void forEach(@NotNull BiConsumer<K, V> each) {
        map.forEach(each);
    }



    public V put(K key, V value) {


        /*synchronized (nameTable)*/
        V removed = putKey(key, value);
        if (removed != null) {

            V remd = removeItem(removed);

            if (remd == null) {
                throw new RuntimeException("unable to remove item corresponding to key " + key);
            }
        }

        V removed2 = addItem(value);

        if (removed != null && removed2 != null) {
            throw new RuntimeException("Only one item should have been removed on this insert; both removed: " + removed + ", " + removed2);
        }
        if ((removed2 != null) && (!key(removed2).equals(key))) {
            removeKey(key(removed2));
            removed = removed2;
        }

        return removed;
    }

    @Nullable
    public V remove(K key) {

        V e = removeKey(key);
        if (e != null) {
            V removed = removeItem(e);
            if (removed == null) {
                /*if (Global.DEBUG)
                    throw new RuntimeException(key + " removed from index but not from items list");*/
                return null;
            }
            if (removed != e)
                throw new RuntimeException(key + " removed " + e + " but item removed was " + removed);
            return removed;
        }

        return null;
    }


    public final V removeKey(K key) {
        return map.remove(key);
    }


    public final int size() {
        return map.size();
    }

    public final boolean containsValue(V it) {
        return map.containsValue(it);
    }

    public final void clear() {
        map.clear();
    }

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


    /**
     * put key in index, do not add value
     */
    public final V putKey(K key, V value) {
        return map.put(key, value);
    }


}
