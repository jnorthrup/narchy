package jcog.data.list.table;

import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * nearly a Map
 */
public interface Table<K, V> extends Iterable<V> {

    void clear();

    @Nullable
    V get( Object key);

    @Nullable
    Object remove( K key);

    int size();

//    /** clear first then set cap to zero. otherwise setCapacity might trigger compression. HACK */
//    default void delete() {
//        clear();
//        setCapacity(0);
//    }

    /**
     * iterates in sorted order
     */
    void forEachKey( Consumer<? super K> each);

    int capacity();

    default boolean isFull() {
        return size() >= capacity();
    }


}
