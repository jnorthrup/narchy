package jcog.list.table;

import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/** nearly a Map */
public interface Table<K,V> extends Iterable<V> {

    void clear();

    @Nullable
    V get(/*@NotNull*/ Object key);

    @Nullable
    Object remove(/*@NotNull*/ K key);

    int size();





    default void delete() {

    }

    /** iterates in sorted order */
    void forEachKey(/*@NotNull*/ Consumer<? super K> each);

    int capacity();

    void setCapacity(int i);



    default boolean isFull() {
        return size() >= capacity();
    }











}
