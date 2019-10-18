package com.github.fge.grappa.stack;

import com.google.common.annotations.VisibleForTesting;
import jcog.util.ArrayUtil;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A {@link ValueStack} implementation using arrays
 *
 * <p>This is the default implementation currently used.</p>
 *
 * @param <V> type parameter of the stack's element
 */
@ParametersAreNonnullByDefault
public final class ArrayValueStack<V>
    extends ValueStackBase<V> {
    @VisibleForTesting
    static final int INITIAL_SIZE = 16;

    @VisibleForTesting
    static final int SIZE_INCREASE = 16;

    private int arraySize = 0;
    private V[] array = (V[]) new Object[INITIAL_SIZE];

    public ArrayValueStack() {
    }

    @VisibleForTesting
    ArrayValueStack(V[] values) {
        System.arraycopy(values, 0, array, 0, values.length);
        arraySize = values.length;
    }

    @VisibleForTesting
    V[] getArray() {
        return Arrays.copyOf(array, array.length);
    }

    @Override
    protected void doPush(int down, V value) {
        ensureCapacity();
        System.arraycopy(array, down, array, down + 1, arraySize - down);
        array[down] = value;
        arraySize++;
    }

    @Override
    protected V doPop(int down) {
        V ret = array[down];
        arraySize--;
        System.arraycopy(array, down + 1, array, down, arraySize - down);
        array[arraySize] = null;
        shrinkIfNecessary();
        return ret;
    }

    @Override
    protected V doPeek(int down) {
        return array[down];
    }

    @Override
    protected void doPoke(int down, V value) {
        array[down] = value;
    }

    @Override
    protected void doDup() {
        ensureCapacity();
        System.arraycopy(array, 0, array, 1, arraySize);
        arraySize++;
    }

    @Override
    protected void doSwap(int n) {
        V tmp;

        int swapIndex = n / 2; // this also works for odd numbers

        for (int index = 0; index < swapIndex; index++) {
            tmp = array[index];
            array[index] = array[n - index - 1];
            array[n - index - 1] = tmp;
        }
    }

    @Override
    public int size() {
        return arraySize;
    }

    @Override
    public void clear() {
        arraySize = 0;
        array = (V[]) ArrayUtil.EMPTY_OBJECT_ARRAY;
    }

    @Override
    public Object takeSnapshot() {
        V[] copy = array.length > 0  ? Arrays.copyOf(array, array.length) : (V[]) ArrayUtil.EMPTY_OBJECT_ARRAY;
        return new ArrayWithSize<>(copy, arraySize);
    }

    @Override
    public void restoreSnapshot(Object snapshot) {
        ArrayWithSize<V> s = (ArrayWithSize<V>) snapshot;
        array = s.array;
        arraySize = s.arraySize;
    }

    @Override
    public Iterator<V> iterator() {
        return new ArrayIterator<>(array, arraySize);
    }

    private void ensureCapacity() {
        if (arraySize == array.length)
            array = Arrays.copyOf(array, arraySize + SIZE_INCREASE);
    }

    private void shrinkIfNecessary() {
    }
//    private void shrinkIfNecessary()
//    {
//        final int length = array.length;
//        final int lengthSizeDiff = length - arraySize;
//        if (lengthSizeDiff >= SIZE_INCREASE)
//            array = Arrays.copyOf(array, length - SIZE_INCREASE);
//    }

    private static final class ArrayWithSize<T>
    {
        private final T[] array;
        private final int arraySize;

        private ArrayWithSize(T[] array, int size)
        {
            this.array = array;
            arraySize = size;
        }
    }

    private static final class ArrayIterator<T>
        implements Iterator<T>
    {
        private final T[] array;
        private final int arraySize;

        private int index = 0;

        private ArrayIterator(T[] array, int size)
        {
            this.array = array;
            arraySize = size;
        }

        @Override
        public boolean hasNext()
        {
            return index < arraySize;
        }

        @Override
        public T next()
        {
            if (!hasNext())
                throw new NoSuchElementException();
            return array[index++];
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }
}
