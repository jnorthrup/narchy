package jcog.data.list.table;

import jcog.WTF;
import jcog.data.atomic.MetalAtomicIntegerFieldUpdater;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Items are available by an integer index
 */
abstract public class ArrayListTable<K, V> implements Table<K, V> {

    static final MetalAtomicIntegerFieldUpdater CAPACITY = new MetalAtomicIntegerFieldUpdater(ArrayListTable.class, "capacity");

    public final Map<K, V> map;

    private volatile int capacity;

    public ArrayListTable(Map<K, V> map) {
        this.map = map;
    }

    abstract public V get(int i);

    @Override
    abstract public int size();

    @Override
    public void forEachKey(Consumer<? super K> each) {
        forEach(t -> each.accept(key(t)));
    }


    @Override
    abstract public Iterator<V> iterator();


    public void clear() {
        map.clear();
        listClear();
    }

    abstract protected void listClear();

    /**
     * Check if an item is in the bag
     *
     * @param k An item
     * @return Whether the Item is in the Bag
     */
    public final boolean contains(/**/ K k) {
        return this.containsKey(k);
    }


    @Override
    public final int capacity() {
        return CAPACITY.getOpaque(this);
    }


    @Override
    public void setCapacity(int newCapacity) {
        CAPACITY.set(this, newCapacity);
    }

    /**
     * returns whether the capacity has changed
     */
    protected boolean setCapacityIfChanged(int newCapacity) {
        return CAPACITY.getAndSet(this, newCapacity)!=newCapacity;
    }


    /**
     * default implementation; more optimal implementations will avoid instancing an iterator
     */
    public void forEach(int max, Consumer<? super V> action) {
        int n = Math.min(size(), max);

        for (int i = 0; i < n; i++) {
            action.accept(get(i));
        }
    }


    abstract public K key(V v);

    /**
     * implementation for removing the value to another collecton (called internally)
     */
    protected abstract boolean removeItem(V e);

    public final void forEach(BiConsumer<K, V> each) {
        map.forEach(each);
    }

    public V remove(/*@NotNull*/ K x) {
        V removed = map.remove(x);
        if (removed != null) {
            boolean removedFromList = removeItem(removed);
            if (!removedFromList)
                throw new WTF();
        }
        return removed;
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
}
