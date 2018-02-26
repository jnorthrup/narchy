package jcog.util;

import java.util.ListIterator;
import java.util.NoSuchElementException;

public class SubArrayIterator<E> implements ListIterator<E> {
    private final E[] array;
    private final int size;
    private int next;
    private int index;

    public SubArrayIterator(E[] array, int index, int size) {
        this.array = array;
        next = index;
        this.index = -1;
        this.size = Math.min(array.length, size);
    }

    @Override
    public boolean hasNext() {
        return next < size;
    }

    @Override
    public E next() {
        if (!hasNext())
            throw new NoSuchElementException();
        index = next++;
        return array[index];
    }

    @Override
    public boolean hasPrevious() {
        return next != 0;
    }

    @Override
    public E previous() {
        if (!hasPrevious())
            throw new NoSuchElementException();
        index = --next;
        return array[index];
    }

    @Override
    public int nextIndex() {
        return next;
    }

    @Override
    public int previousIndex() {
        return next - 1;
    }

    @Override
    public void remove() {
        // This operation is not so easy to do but we will fake it.
        // The issue is that the backing list could be completely
        // different than the one this iterator is a snapshot of.
        // We'll just remove(element) which in most cases will be
        // correct.  If the list had earlier .equals() equivalent
        // elements then we'll remove one of those instead.  Either
        // way, none of those changes are reflected in this iterator.
        //DirectCopyOnWriteArrayList.this.remove(array[lastReturned]);
        throw new UnsupportedOperationException();
    }

    @Override
    public void set(E e) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void add(E e) {
        throw new UnsupportedOperationException();
    }
}
