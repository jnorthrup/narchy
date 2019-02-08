package jcog.data.list;

import jcog.Util;
import jcog.util.ArrayUtils;
import jcog.util.FloatFloatToFloatFunction;
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
     * use with caution
     */
    public void setArray(X[] v) {
        items = v;
        size = v.length;
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
        return result;
        //return result < oldSize ? (Integer.MAX_VALUE - 8) : result;
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

//    public void clearHard() {
//        this.size = 0;
//        this.items = (X[]) ArrayUtils.EMPTY_OBJECT_ARRAY;
//    }

    public X removeLast() {
//        if (size == 0)
//            throw new ArrayIndexOutOfBoundsException(); //it will be obvious if this happens
        X[] ii = this.items;
        X x = ii[--size];
        ii[size] = null;
        return x;
    }

    /**
     * removes last item or returns null if empty, similar to Queue.poll()
     */
    public final X poll() {
        if (size == 0)
            return null;
        return removeLast();
    }

    @Override
    public final X get(int index) {
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

        if (object == null)
            return indexOfInstance(null);

        int s = size;
        if (s > 0) {
            X[] items = this.items;
            for (int i = 0; i < s; i++)
                if (object.equals(items[i]))
                    return i;
        }
        return -1;
    }

    public int indexOfInstance(/*@NotNull*/ Object x) {
        int s = size;
        if (s > 0) {
            X[] items = this.items;
            for (int i = 0; i < s; i++)
                if (items[i] == x)
                    return i;
        }
        return -1;
    }

    @Override
    public MutableList<X> shuffleThis(Random rnd) {
        return size > 1 ? super.shuffleThis(rnd) : this;
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
    public X[] array() {
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

    /**
     * reduce
     */
    public float reapply(FloatFunction<? super X> function, FloatFloatToFloatFunction combine) {
        int n = size;
        switch (n) {
            case 0:
                return Float.NaN;
            case 1:
                return function.floatValueOf(items[0]);
            default:
                float x = function.floatValueOf(items[0]);
                for (int i = 1; i < n; i++) {
                    x = combine.apply(x, function.floatValueOf(items[i]));
                }
                return x;
        }
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

    public long maxValue(ToLongFunction<? super X> function) {
        long max = Long.MIN_VALUE;
        for (int i = 0, thisSize = this.size(); i < thisSize; i++) {
            long y = function.applyAsLong(this.get(i));
            if (y > max)
                max = y;
        }
        return max;
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

    public final X[] fillArray(X[] array, boolean nullRemainder) {

        int l = array.length;
        int s = Math.min(l, size);
        System.arraycopy(items, 0, array, 0, s);
        if (nullRemainder && s < l)
            Arrays.fill(array, s, l, null);
        return array;
    }

    @Override
    public void forEach(Consumer c) {
        int s = size;
        if (s > 0) {
            X[] ii = items;
            for (int i = 0; i < s; i++) {
                X j = ii[i];
                if (j != null)
                    c.accept(j);
            }
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
            this.removeFast(index);
            return r;
        }
        return null;
    }

    @Override
    public boolean add(X newItem) {
        ensureCapacityForAdditional(1);
        addWithoutResizeCheck(newItem);
        return true;
    }

    public int addAndGetSize(X newItem) {
        ensureCapacityForAdditional(1);
        addWithoutResizeCheck(newItem);
        return size;
    }



    public void ensureCapacityForAdditional(int num) {
        X[] ii = this.items;
        int s = this.size + num, l = ii.length;
        if (l < s) {
            this.items = (X[]) ((l == 0) ?
                            newArray(Math.max(num, INITIAL_SIZE_IF_GROWING_FROM_EMPTY))
                            :
                            Arrays.copyOf(items, sizePlusFiftyPercent(s)));
        }
    }

    protected Object[] newArray(int newCapacity) {
        return new Object[newCapacity];
    }

    public final boolean addIfNotNull(@Nullable Supplier<X> x) {
        return addIfNotNull(x.get());
    }

    public final boolean addIfNotNull(@Nullable X x) {
        return x != null && add(x);
    }

    /**
     * slow: use a setAt
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
        for (Object j : items) {
            if (j == null)
                break;
            each.accept(n++, j);
        }
        return size();
    }

    public FasterList<X> addingAll(X... x) {
        int l = x.length;
        if (l > 0) {
            ensureCapacityForAdditional(l);
            for (X y : x)
                add(y);
        }
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
        //if (size < 0) throw new ArrayIndexOutOfBoundsException(); //TEMPORARY
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


    public boolean removeBelow(int index) {
        if (index == 0)
            return false;

        //TODO optimize
        for (int i = 0; i < index; i++)
            remove(0);
        return true;
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
        return new FasterList<>(size, items.length > 0 ? items.clone() : items);
    }

    /**
     * dangerous unless you know the array has enough capacity
     */
    public void addWithoutResizeCheck(X x) {
        this.items[this.size++] = x;
    }

    public boolean addWithoutResize(X x) {
        X[] i = this.items;
        if (this.size < i.length) {
            i[this.size++] = x;
            return true;
        }
        return false;
    }

    public boolean addWithoutResize(Supplier<X> x) {
        X[] i = this.items;
        if (this.size < i.length) {
            i[this.size++] = x.get();
            return true;
        }
        return false;
    }

    public X[] toArrayRecycled(IntFunction<X[]> ii) {
        X[] a = items;
        int s = size;
        if (s == a.length && a.getClass() != Object[].class)
            return a;
        else
            return fillArray(ii.apply(size), false);
    }

    protected X[] toArrayCopy(@Nullable X[] target, IntFunction<X[]> ii) {
        int s = size;
        if (s != target.length) {
            target = ii.apply(s);
        }
        return fillArray(target, false);
    }


    /**
     * after procedure executes on a cell, it nullifies the cell. equivalent to:
     * forEach(p) ... clear()
     * but faster
     *
     * @param each
     */
    public void clear(Consumer<? super X> each) {
        int s = this.size;
        if (s > 0) {
            for (int i = 0; i < s; i++) {
                each.accept(this.items[i]);
                this.items[i] = null;
            }
            this.size = 0;
        }
    }

    public <Y> void clearWith(BiConsumer<X, Y> each, Y y) {
        int s = this.size;
        if (s > 0) {
            for (int i = 0; i < s; i++) {
                each.accept(this.items[i], y);
                this.items[i] = null;
            }
            this.size = 0;
        }
    }

    @Override
    public void clear() {
        clearIfChanged();
    }

//    public void clearReallocate(int maxSizeToReuse, int sizeIfNew) {
//        assert (sizeIfNew < maxSizeToReuse);
//
//        int s = this.size;
//        if (s == 0)
//            return;
//        else if (s > maxSizeToReuse) {
//            items = Arrays.copyOf(items, sizeIfNew);
//        } else {
//            //re-use, so nullify
//            Arrays.fill(this.items, 0, s, null);
//        }
//        this.size = 0;
//    }

    public boolean clearIfChanged() {
        int s = size;
        if (s > 0) {
            Arrays.fill(this.items, 0, s, null);
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

    public boolean removeFirstInstance(X x) {
        int i = indexOfInstance(x);
        if (i != -1) {
            removeFast(i);
            return true;
        }
        return false;
    }

    public List<Iterable<X>> chunkView(int chunkSize) {
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("Size for groups must be positive but was: " + chunkSize);
        }


        MutableList<Iterable<X>> result = new FasterList((int) (Math.ceil((float) size()) / chunkSize));
        int i = 0;

        int size = this.size();
        while (i < size) {
            result.add(subList(i, Math.min(i + chunkSize, this.size)));
            i += chunkSize;
        }
        return result;

    }

    public void swap(int a, int b) {
        if (a != b) {
            X x = get(a);
            X y = get(b);
            set(a, y);
            set(b, x);
        }
    }

    /**
     * use with caution
     */
    public void setSize(int s) {
        this.size = s;
    }

    public boolean removeFirst(X x) {
        int s = this.size;
        if (s > 0) {
            X[] ii = items;
            for (int i = 0; i < s; i++) {
                if (ii[i].equals(x)) {
                    removeFast(i);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * assumes all elements are non-null, eliding null checks
     */
    public boolean nonNullEquals(FasterList<X> x) {
        if (this == x) return true;
        int s = this.size;
        if (s != x.size) {
            return false;
        }
        X[] a = this.items;
        X[] b = x.items;
        for (int i = 0; i < s; i++) {
            if (!a[i].equals(b[i]))
                return false;
        }
        return true;
    }


    public void replaceLast(X y) {
        items[size - 1] = y;
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
            return this.currentIndex < this.list.size;
        }

        @Override
        public T next() {
            return this.list.get(this.lastIndex = this.currentIndex++);
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
    public boolean anySatisfy(int from, int to, Predicate<? super X> predicate2) {
        int s = size;
        if (s > 0 && s > from) {
            X[] items = this.items;
            for (int i = from; i < to; i++) {
                if (predicate2.test(items[i]))
                    return true;
            }
        }
        return false;
    }

    public <P> boolean anySatisfyWith(Predicate2<? super X, ? super P> predicate2, P parameter) {
        int s = size;
        if (s > 0) {
            X[] items = this.items;
            for (int i = 0; i < s; i++) {
                if (predicate2.accept(items[i], parameter))
                    return true;
            }
        }
        return false;
    }

    public <P> boolean allSatisfyWith(Predicate2<? super X, ? super P> predicate2, P parameter) {
        int s = size;
        if (s > 0) {
            X[] items = this.items;
            for (int i = 0; i < s; i++) {
                if (!predicate2.accept(items[i], parameter))
                    return false;
            }
        }
        return true;
    }

}
