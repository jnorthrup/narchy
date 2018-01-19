package jcog.list;

import com.google.common.collect.Iterators;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Iterator;

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
            throw new RuntimeException("iterator() method can only be called once");
        return this;
    }

    public static <E> Iterator<E> get(E... e) {
        if (e == null)
            return Collections.emptyIterator();
        else
            return ArrayIterator.get(e, e.length);
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
