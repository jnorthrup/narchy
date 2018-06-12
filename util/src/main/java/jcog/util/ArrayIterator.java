package jcog.util;

import com.google.common.collect.Iterators;
import com.google.common.collect.Streams;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

/** TODO optionally skip nulls */
public class ArrayIterator<E> implements Iterator<E>, Iterable<E> {

    private final E[] array;
    int index;

    public ArrayIterator(E[] array) {
        this.array = array;
    }


    @Override
    public boolean hasNext() {
        return index < array.length;
    }

    @Override
    public E next() {
        return index < array.length ? array[index++] : null;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public Iterator<E> iterator() {
        if (index != 0)
            return new ArrayIterator(array); //already started, so return a fresh copy
        return this;
    }

    public static <E> Iterator<E> get(E... e) {
        if (e == null)
            return Collections.emptyIterator();
        else
            return ArrayIterator.get(e, e.length);
    }

    public static <E> Iterable<E> iterable(E... e) {
        if (e == null)
            return List.of();
        else {
            switch (e.length) {
                case 1:
                    return List.of(e);
                default:
                    return (Iterable) ArrayIterator.get(e, e.length);
            }
        }
    }

    public static <E> Iterator<E> get(E[] e, int size) {
        switch (size) {
            case 0:
                return Collections.emptyIterator();
            case 1:
                return Iterators.singletonIterator(e[0]);
            default:
                return size == e.length ?
                        new ArrayIterator(e) :
                        new PartialArrayIterator(e, size);
        }
    }


    public static <X> Stream<X> stream(X[] list) {
        return stream(list, list.length);
    }

    public static <X> Stream<X> stream(X[] list, int size) {
        switch (size) {
            case 0: return Stream.empty();
            case 1: return Stream.of(list[0]);
            case 2: return Stream.of(list[0], list[1]);
            
            default:
                return Streams.stream(ArrayIterator.get(list, size));
        }
    }

    static final class PartialArrayIterator<E> extends ArrayIterator<E> {

        private final int size;

        public PartialArrayIterator(E[] array, int size) {
            super(array);
            this.size = size;
        }

        @Override
        public boolean hasNext() {
            return index < size;
        }
    }

}
