package jcog.data.iterator;

import com.google.common.collect.Iterators;
import jcog.TODO;
import jcog.Util;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/** TODO optionally skip nulls */
public class ArrayIterator<E> implements Iterator<E>, Iterable<E> {

    protected final E[] array;
    protected int index;

    public ArrayIterator(E[] array) {
        this.array = array;
    }

    @Override
    public boolean hasNext() {
        return index < array.length;
    }

    @Override
    public E next() {
        //if (index < array.length)
            return array[index++];
        //throw new NoSuchElementException();
    }

    @Override
    public void forEachRemaining(Consumer<? super E> action) {
        throw new TODO();
    }
    @Override
    public void forEach(Consumer<? super E> action) {
        throw new TODO();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<E> iterator() {
        if (index != 0)
            return clone(); //already started, so return a fresh copy
        return this;
    }

    public ArrayIterator<E> clone() {
        return new ArrayIterator(array);
    }

    public static <E> Iterator<E> iterator(E... e) {
        if (e == null)
            return Util.emptyIterator;
        else
            return ArrayIterator.iterateN(e, e.length);
    }

    public static <E> Iterable<E> iterable(E... e) {
        if (e == null)
            return Util.emptyIterable;
        else {
            switch (e.length) {
                case 1:
                    return List.of(e);
                default:
                    return (Iterable) ArrayIterator.iterateN(e, e.length);
            }
        }
    }

    public static <E> Iterator<E> iterateN(E[] e, int from, int to) {
        throw new TODO();
    }

    public static <E> Iterator<E> iterateN(E[] e, int size) {
        switch (size) {
            case 0:
                return Util.emptyIterator;
            case 1:
                return Iterators.singletonIterator(e[0]);
            default:
                return size == e.length ?
                        new ArrayIterator(e) :
                        new PartialArrayIterator(e, size);
        }
    }

    public static <E> Iterator<E> iterateNonNull(E[] e) {
        return iterateNonNullN(e, e.length);
    }

    public static <E> Iterator<E> iterateNonNullN(E[] e, int size) {
        switch (size) {
            case 0:
                return Util.emptyIterator;
            case 1:
                E ee = e[0];
                return ee!=null ? Iterators.singletonIterator(ee) : Util.emptyIterator;
            default:
                return new ArrayIteratorNonNull<>(e, size);
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
                return arrayStream(list, size);
        }
    }

    private static <X> Stream<X> arrayStream(X[] list, int size) {
        if (list.length>size) {
            return IntStream.range(0, size).mapToObj(i -> list[i]);
        } else {
            return Stream.of(list);
        }
    }

    public static <X> Stream<X> streamNonNull(X[] list, int size) {
        switch (size) {
            case 0: return Stream.empty();
            case 1: { X x0 = list[0]; return x0==null ? Stream.empty() : Stream.of(x0); }
            case 2: { X x0 = list[0], x1 = list[1]; if (x0!=null && x1!=null) return Stream.of(x0, x1); else if (x0 == null && x1 == null) return Stream.empty(); else if (x1 == null) return Stream.of(x0); else return Stream.of(x1); }
            default:
                return arrayStream(list, size).filter(Objects::nonNull);
        }
    }

    public static class PartialArrayIterator<E> extends ArrayIterator<E> {

        private final int size;

        public PartialArrayIterator(E[] array, int size) {
            super(array);
            this.size = size;
        }

        @Override
        public boolean hasNext() {
            return index < size;
        }

        public ArrayIterator<E> clone() {
            return new PartialArrayIterator<>(array, size);
        }
    }

//    public static class AtomicArrayIterator<X> extends PartialArrayIterator<X> {
//
//        public AtomicArrayIterator(X[] array, int size) {
//            super(array, size);
//        }
//
//        @Override
//        public X next() {
//            return (X)ITEM.getOpaque(array, index++);
//        }
//    }
}
