package jcog.list;

import jcog.Util;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.block.predicate.Predicate2;
import org.eclipse.collections.api.block.predicate.primitive.IntObjectPredicate;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.utility.ArrayIterate;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.*;

/**
 * Less-safe faster FastList with direct array access
 * <p>
 * TODO override the array creation to create an array
 * of the actual type necessary, so that .array()
 * can provide the right array when casted
 */
public class FasterList<X> extends FastList<X> {


    private static final int INITIAL_SIZE_IF_GROWING_FROM_EMPTY = 8;


    public FasterList() {
        super();
    }

    public FasterList(int initialCapacity) {
        this.items = initialCapacity == 0 ? (X[]) ArrayUtils.EMPTY_OBJECT_ARRAY : (X[]) newArray(initialCapacity);
    }


    public FasterList(Iterable<X> copy) {
        this(copy, 0);
    }

    public FasterList(Iterator<X> copy) {
        this(copy, INITIAL_SIZE_IF_GROWING_FROM_EMPTY);
    }

    public FasterList(Iterator<X> copy, int sizeEstimate) {
        super(sizeEstimate);
        while (copy.hasNext()) {
            add(copy.next());
        }
    }

    public FasterList(Iterable<X> copy, int sizeEstimate) {
        super(sizeEstimate);
        copy.forEach(this::add);
    }

    public FasterList(Collection<X> copy) {
        super(copy);
    }

    public FasterList(Collection<X> copy, IntFunction<X[]> arrayBuilder) {
        this(copy, arrayBuilder, 0);
    }

    public FasterList(Collection<X> copy, IntFunction<X[]> arrayBuilder, int extraCapacity) {
        int size = copy.size();
        this.items = copy.toArray(arrayBuilder.apply(size + extraCapacity));
        this.size = size;
    }


    /**
     * uses array directly
     */
    public FasterList(int size, X[] x) {
        super(size, x);
    }

    public FasterList(X[] x) {
        super(x);
    }

    static private int sizePlusFiftyPercent(int oldSize) {
        int result = oldSize + (oldSize / 2) + 1;
        return result < oldSize ? (Integer.MAX_VALUE - 8) : result;
    }

    @Override
    public FastList<X> toSortedList() {
        //TODO size=2 simple case
        return size > 1 ? super.toSortedList() : this;
    }

    @Override
    public <V extends Comparable<? super V>> MutableList<X> toSortedListBy(Function<? super X, ? extends V> function) {
        //TODO size=2 simple case
        return size > 1 ? super.toSortedListBy(function) : this;
    }

    @Override
    public FastList<X> toSortedList(Comparator<? super X> comparator) {
        //TODO size=2 simple case
        return size > 1 ? super.toSortedList(comparator) : this;
    }

    @Override
    public FasterList<X> sortThis(Comparator<? super X> comparator) {
        if (size > 1)
            super.sortThis(comparator);
        return this;
    }

    @Override
    public MutableList<X> sortThisByFloat(FloatFunction<? super X> function) {
        if (size > 1)
            super.sortThisByFloat(function);
        return this;
    }

    @Override
    public MutableList<X> sortThisByInt(org.eclipse.collections.api.block.function.primitive.IntFunction<? super X> function) {
        if (size > 1)
            super.sortThisByInt(function);
        return this;
    }

    @Override
    public int size() {

        return size;
    }

    public void clearHard() {
        this.size = 0;
        this.items = (X[]) ArrayUtils.EMPTY_OBJECT_ARRAY;
    }

    public X removeLast() {

        if (size == 0)
            throw new ArrayIndexOutOfBoundsException();
        X[] ii = this.items;
        X x = ii[--size];
        ii[size] = null;
        return x;
    }

    @Override
    public X get(int index) {
        return items[index];
    }

    @Nullable
    public X get(Random random) {
        int s = this.size;
        X[] ii = this.items;
        switch (s) {
            case 0:
                return null;
            case 1:
                return ii[0];
            default:
                return ii[random.nextInt(Math.min(s, ii.length))];
        }
    }

    @Nullable
    public final X getSafe(int index) {
        if (index < 0)
            return null;
        X[] items = this.items;
        if (index < Math.min(items.length, this.size)) {
            return items[index];
        } else {
            return null;
        }
    }

    @Override
    public int indexOf(/*@NotNull*/ Object object) {


        int s = size;
        if (s > 0) {
            X[] items = this.items;
            if (object == null) {
                for (int i = 0; i < s; i++)
                    if (items[i] == null)
                        return i;
            } else {
                for (int i = 0; i < s; i++)
                    if (object.equals(items[i]))
                        return i;
            }
        }
        return -1;
    }

    /**
     * use with caution.
     * --this could become invalidated so use it as a snapshot
     * --dont modify it
     * --when iterating, expect to encounter a null
     * at any time, and if this happens, break your loop
     * early
     * *
     */
    public final X[] array() {
        return items;
    }

    public FasterList<X> compact() {
        X[] i = items;
        int s = size;
        if (i.length != s) {
            items = Arrays.copyOf(items, size);
        }
        return this;
    }

    /**
     * returns the array directly, or reconstructs it for the target type for the exact size required
     */
    public final <Y> Y[] array(IntFunction<Y[]> arrayBuilder) {
        Object[] i = items;
        int s = size;
        if (i.length != s || i.getClass() == Object[].class) {
            Y[] x = arrayBuilder.apply(s);
            if (s > 0)
                System.arraycopy(items, 0, x, 0, s);
            return x;
        }
        return (Y[]) i;
    }

    public float meanValue(FloatFunction<? super X> function) {
        return (float) (sumOfFloat(function) / size());
    }

    public int maxIndex(Comparator<? super X> comparator) {
        Object[] array = items;
        int size = this.size;
        if (size == 0) {
            return -1;
        }

        X max = (X) array[0];
        int maxIndex = 0;
        for (int i = 1; i < size; i++) {
            X item = (X) array[i];
            if (comparator.compare(item, max) > 0) {
                max = item;
                maxIndex = i;
            }
        }
        return maxIndex;
    }

    public float maxValue(FloatFunction<? super X> function) {
        float max = Float.NEGATIVE_INFINITY;
        for (int i = 0, thisSize = this.size(); i < thisSize; i++) {
            float y = function.floatValueOf(this.get(i));
            if (y > max)
                max = y;
        }
        return max;
    }

    public long minValue(ToLongFunction<? super X> function) {
        long min = Long.MAX_VALUE;
        for (int i = 0, thisSize = this.size(); i < thisSize; i++) {
            long y = function.applyAsLong(this.get(i));
            if (y < min)
                min = y;
        }
        return min;
    }

    public X maxBy(float mustExceed, FloatFunction<? super X> function) {

        if (ArrayIterate.isEmpty(items)) {
            throw new NoSuchElementException();
        }

        X min = null;
        float minValue = mustExceed;
        for (int i = 0; i < size; i++) {
            X next = items[i];
            float nextValue = function.floatValueOf(next);
            if (nextValue > minValue) {
                min = next;
                minValue = nextValue;
            }
        }
        return min;

    }


    @Override
    public final boolean removeIf(Predicate<? super X> filter) {
        int s = size();
        int ps = s;
        X[] a = this.items;
        for (int i = 0; i < s; ) {
            if (filter.test(a[i])) {
                s--;
                System.arraycopy(a, i + 1, a, i, s - i);
                Arrays.fill(a, s, ps, null);
            } else {
                i++;
            }
        }
        if (ps != s) {
            this.size = s;
            return true;
        }

        return false;
    }

    public final boolean removeIf(Predicate<? super X> filter, List<X> displaced) {
        int s = size();
        int ps = s;
        X[] a = this.items;
        for (int i = 0; i < s; ) {
            X ai = a[i];
            if (ai == null || (filter.test(ai) && displaced.add(ai))) {
                s--;
                System.arraycopy(a, i + 1, a, i, s - i);
                Arrays.fill(a, s, ps, null);
            } else {
                i++;
            }
        }
        if (ps != s) {
            this.size = s;
            return true;
        }

        return false;
    }

    /**
     * try to use toArrayRecycled where possible
     */
    public X[] toArrayWith(IntFunction<X[]> arrayBuilder) {
        return fillArray(arrayBuilder.apply(size));
    }

//    public final X[] fillArrayNullPadded(X[] array) {
//        int s = size;
//        int l = array.length;
//        if (array == null || array.length < (s + 1)) {
//            array = (X[]) Array.newInstance(array.getClass().getComponentType(), s + 1);
//        }
//        System.arraycopy(items, 0, array, 0, s);
//        if (s < l)
//            Arrays.fill(array, s, l, null);
//        return array;
//    }

    public final X[] fillArray(X[] array) {
        int s = size;
        int l = array.length;
        System.arraycopy(items, 0, array, 0, s);
        if (s < l)
            Arrays.fill(array, s, l, null);
        return array;
    }

    @Override
    public void forEach(Consumer c) {
        int s = size;
        X[] ii = items;
        for (int i = 0; i < s; i++) {
            X j = ii[i];
            if (j != null)
                c.accept(j);
        }
    }

    public final void clearFast() {
        size = 0;
    }

    /**
     * remove, but with Map.remove semantics
     */
    public X removed(/*@NotNull*/ X object) {
        int index = this.indexOf(object);
        if (index >= 0) {
            X r = get(index);
            this.remove(index);
            return r;
        }
        return null;
    }

    @Override
    public boolean add(X newItem) {
        if (this.items.length == this.size) {
            this.ensureCapacityForAdd();
        }
        addWithoutResizeCheck(newItem);
        return true;
    }

    public int addAndGetSize(X newItem) {
        int s;
        if (this.items.length == (s = this.size)) {
            this.ensureCapacityForAdd();
        }
        addWithoutResizeCheck(newItem);
        return s + 1;
    }

    private void ensureCapacityForAdd() {
        this.items = (X[]) (
                (this.items.length == 0) ?
                        newArray(INITIAL_SIZE_IF_GROWING_FROM_EMPTY)
                        :
                        this.copyItemsWithNewCapacity(sizePlusFiftyPercent(this.size))
        );
    }

    private Object[] copyItemsWithNewCapacity(int newCapacity) {
        Object[] newItems = newArray(newCapacity);
        System.arraycopy(this.items, 0, newItems, 0, Math.min(this.size, newCapacity));
        return newItems;
    }

    protected Object[] newArray(int newCapacity) {
        return new Object[newCapacity];
    }

    public final boolean addIfNotNull(@Nullable Supplier<X> x) {
        return addIfNotNull(x.get());
    }

    public final boolean addIfNotNull(@Nullable X x) {
        if (x != null)
            return add(x);
        return false;
    }

    /**
     * slow: use a set
     */
    public final boolean addIfNotPresent(@Nullable X x) {
        if (!contains(x)) {
            add(x);
            return true;
        }
        return false;
    }

    public int forEach(int offset, IntObjectPredicate each) {
        int n = offset;
        for (Object j: items) {
            if (j == null)
                break;
            each.accept(n++, j);
        }
        return size();
    }

    public FasterList addingAll(X... x) {
        for (X y: x)
            add(y);
        return this;
    }

    public final void setFast(int index, X t) {
        items[index] = t;
    }

    public void removeFast(int index) {
        X[] ii = items;
        int totalOffset = this.size - index - 1;
        if (totalOffset > 0)
            System.arraycopy(ii, index + 1, ii, index, totalOffset);
        ii[--size] = null;
    }


    @Override
    public boolean remove(Object object) {
        int index = this.indexOf(object);
        if (index >= 0) {
            this.removeFast(index);
            return true;
        }
        return false;
    }

    @Override
    public Iterator<X> iterator() {
        switch (size) {
            case 0:
                return Collections.emptyIterator();

            default:
                return new FasterListIterator<>(this);
        }
    }

    public boolean containsInstance(X x) {
        for (int i = 0, thisSize = this.size(); i < thisSize; i++) {
            X y = this.items[i];
            if (y == x)
                return true;
        }
        return false;
    }


    public boolean removeAbove(int index) {
        int s = this.size;
        if (index >= s)
            return false;
        Arrays.fill(items, index, s, null);
        this.size = index;
        return true;
    }

    public int capacity() {
        return items.length;
    }

    @Override
    public FasterList<X> clone() {
        return new FasterList<>(size, items.clone());
    }

    /**
     * dangerous unless you know the array has enough capacity
     */
    public void addWithoutResizeCheck(X x) {
        this.items[this.size++] = x;
    }

    public X[] toArrayRecycled(IntFunction<X[]> ii) {
        X[] a = items;
        int s = size;
        if (s == a.length && a.getClass() != Object[].class)
            return a;
        else
            return toArrayWith(ii);
    }

    /**
     * after procedure executes on a cell, it nullifies the cell. equivalent to:
     * forEach(p) ... clear()
     * but faster
     *
     * @param procedure
     */
    public void clear(Consumer<? super X> procedure) {
        int s = this.size;
        for (int i = 0; i < s; i++) {
            procedure.accept(this.items[i]);
            this.items[i] = null;
        }
        this.size = 0;
    }

    @Override
    public void clear() {
        clearIfChanged();
    }

    public void clearReallocate(int maxSizeToReuse, int sizeIfNew) {
        assert(sizeIfNew < maxSizeToReuse);

        int s = this.size;
        if (s == 0)
            return;
        else if (s > maxSizeToReuse) {
            items = Arrays.copyOf(items, sizeIfNew);
        } else {
            //re-use, so nullify
            Arrays.fill(this.items, 0, s, null);
        }
        this.size = 0;
    }

    public boolean clearIfChanged() {
        int s = size;
        if (s > 0) {
            Arrays.fill(this.items, 0, size, null);
            this.size = 0;
            return true;
        }
        return false;
    }

    public void reverse() {
        int s = this.size;
        if (s > 1) {
            Util.reverse(items, 0, s - 1);
        }
    }

    public void removeNulls() {
        switch (size) {
            case 0:
                return;
            case 1:
                if (get(0) == null) {
                    size = 0;
                    return;
                }
                break;


            default:
                removeIf(Objects::isNull);

        }
    }

    @Override
    public boolean removeIf(org.eclipse.collections.api.block.predicate.Predicate<? super X> predicate) {
        int nowFilled = 0;
        int s0 = this.size;
        if (s0 == 0) return false;
        X[] xx = this.items;
        for (int i = 0; i < s0; i++) {
            X x = xx[i];
            if (!predicate.accept(x)) {

                if (nowFilled != i) {
                    xx[nowFilled] = x;
                }
                nowFilled++;
            }
        }
        if (nowFilled < s0) {
            Arrays.fill(items, nowFilled, s0, null);
            this.size = nowFilled;
            return true;
        } else
            return false;
    }

    public boolean removeInstance(X x) {
        return removeIf(y -> x == y);
    }

    public List<Iterable<X>> chunkView(int chunkSize) {
            if (chunkSize <= 0)
            {
                throw new IllegalArgumentException("Size for groups must be positive but was: " + chunkSize);
            }


                MutableList<Iterable<X>> result = new FasterList((int) (Math.ceil((float)size()) / chunkSize));
                int i = 0;

                int size = this.size();
                while (i < size) {
                    result.add(subList(i, Math.min(i + chunkSize, this.size)));
                    i += chunkSize;
                }
                return result;

        }


    /**
     * modified from MutableIterator
     */
    static class FasterListIterator<T> implements Iterator<T> {
        protected final FasterList<T> list;
        protected int currentIndex;
        protected int lastIndex = -1;

        public FasterListIterator(FasterList<T> list) {
            this.list = list;
        }

        @Override
        public boolean hasNext() {
            return this.currentIndex != this.list.size;
        }

        @Override
        public T next() {

            T next = this.list.get(this.currentIndex);
            this.lastIndex = this.currentIndex++;
            return next;


        }

        @Override
        public void remove() {
            if (this.lastIndex == -1) {
                throw new IllegalStateException();
            }
            this.list.removeFast(this.lastIndex);
            if (this.lastIndex < this.currentIndex) {
                this.currentIndex--;
            }
            this.lastIndex = -1;
        }
    }

    public <P> boolean anySatisfyWith(Predicate2<? super X, ? super P> predicate2, P parameter)    {
        int s = size;
        X[] items = this.items;
        for (int i = 0; i < s; i++) {
            if (predicate2.accept(items[i], parameter))
                return true;
        }
        return false;
    }
    public <P> boolean allSatisfyWith(Predicate2<? super X, ? super P> predicate2, P parameter)    {
        int s = size;
        X[] items = this.items;
        for (int i = 0; i < s; i++) {
            if (!predicate2.accept(items[i], parameter))
                return false;
        }
        return true;
    }

}
