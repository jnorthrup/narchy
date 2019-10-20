package jcog.data.list;

import jcog.Util;
import jcog.sort.SmoothSort;
import jcog.util.ArrayUtil;
import jcog.util.FloatFloatToFloatFunction;
import org.eclipse.collections.api.block.function.Function;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.block.function.primitive.LongObjectToLongFunction;
import org.eclipse.collections.api.block.predicate.Predicate2;
import org.eclipse.collections.api.block.predicate.primitive.IntObjectPredicate;
import org.eclipse.collections.api.block.procedure.Procedure2;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.*;
import java.util.stream.IntStream;

/**
 * Less-safe faster FastList with direct array access
 * <p>
 * TODO override the array creation to create an array
 * of the actual type necessary, so that .array()
 * can provide the right array when casted
 */
public class FasterList<X> extends FastList<X> {

    private static final int INITIAL_SIZE_IF_GROWING_FROM_EMPTY = 4;

    public FasterList() {
        super();
    }

    public FasterList(int initialCapacity) {
        this.items = newArray(initialCapacity);
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
        for (X x : copy) {
            add(x);
        }
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
        this.items = x;
        this.size = size;
        if (size > x.length)
            ensureCapacity(size);
    }

    public FasterList(X[] x) {
        super(x);
    }

    private static int sizePlusFiftyPercent(int oldSize) {
        return oldSize + (oldSize / 2) + 1;
        //return result < oldSize ? (Integer.MAX_VALUE - 8) : result;
    }

    public boolean isEmpty() {
        return this.size == 0;
    }

    /**
     * use with caution
     */
    public void setArray(X[] v) {
        items = v;
        size = v.length;
    }

    @Override
    public FastList<X> sortThis() {
        if (size > 1) { //superclass doesnt test for this condition
            //super.sortThis();
            SmoothSort.smoothSort(items, 0, size, (x,y)->((Comparable)x).compareTo(y)); //figure that repeated sorting will lead to a smoothsort advantage
        }
        return this;
    }

    @Override
    public FastList<X> reverseThis() {
        if (size > 1) //superclass doesnt test for this condition
            super.reverseThis();
        return this;
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
    public void ensureCapacity(int minCapacity) {
        int oldCapacity = this.items.length;
        if (minCapacity > oldCapacity) {
            if (oldCapacity == 0) {
                items = newArray(minCapacity);
            } else {
                int newCapacity = Math.max(sizePlusFiftyPercent(oldCapacity), minCapacity);
                this.transferItemsToNewArrayWithCapacity(newCapacity);
            }
        }
    }

    public void clearCapacity(int newCapacity) {
        items = newArray(newCapacity);
        size = 0;
    }

    private void transferItemsToNewArrayWithCapacity(int newCapacity) {
        //this.items = (X[]) this.copyItemsWithNewCapacity(newCapacity);
        Object[] newItems = newArray(newCapacity);
        System.arraycopy(this.items, 0, newItems, 0, Math.min(this.size, newCapacity));
        this.items = (X[]) newItems;
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

    /**
     * pop()
     */
    public X removeLast() {
        X[] ii = this.items;
        X x = ii[--size];
        ii[size] = null;
        return x;
    }

    /**
     * pop()
     */
    public void removeLastFast() {
        X[] ii = this.items;
        size--;
        ii[size] = null;
    }

    /**
     * removes last item or returns null if empty, similar to Queue.poll()
     */
    public final X poll() {
        return size == 0 ? null : removeLast();
    }

    @Override
    public final X get(int index) {
        return items[index];
    }


    public final X getFirstFast() {
        return items[0];
    }

    public @Nullable X get(Random random) {
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

    public final @Nullable X getSafe(int index) {
        if (index < 0)
            return null;
        X[] items = this.items;
        if (items!=null && index < Math.min(items.length, this.size)) {
            return items[index];
        } else {
            return null;
        }
    }

    public final int indexOf(Predicate<X> p) {
        return indexOf(0, p);
    }

    public int indexOf(int atOrAfter, Predicate<X> p) {
        int s = size;
        if (s > 0) {
            X[] items = this.items;
            for (int i = Math.max(0, atOrAfter); i < s; i++) {
                if (p.test(items[i])) {
                    return i;
                }
            }
            return -1;
        }
        return -1;
    }

    public final int indexOf(IntPredicate p) {
        return indexOf(0, p);
    }

    public int indexOf(int atOrAfter, IntPredicate p) {
        int s = size;
        if (s > 0) {
            for (int i = Math.max(0, atOrAfter); i < s; i++) {
                if (p.test(i)) {
                    return i;
                }
            }
            return -1;
        }
        return -1;
    }

    @Override
    public int indexOf(/*@NotNull*/ Object object) {

        if (object == null)
            return indexOfInstance(null);

        int s = size;
        if (s > 0) {
            X[] items = this.items;
            for (int i = 0; i < s; i++) {
                if (object.equals(items[i])) {
                    return i;
                }
            }
            return -1;
        }
        return -1;
    }

    public int indexOfInstance(/*@NotNull*/ Object x) {
        int s = size;
        if (s > 0) {
            X[] items = this.items;
            for (int i = 0; i < s; i++) {
                if (items[i] == x) {
                    return i;
                }
            }
            return -1;
        }
        return -1;
    }

    @Override
    public MutableList<X> shuffleThis(Random rnd) {
        return size > 1 ? super.shuffleThis(rnd) : this;
    }
    public MutableList<X> shuffleThis(Supplier<Random> rnd) {
        return size > 1 ? super.shuffleThis(rnd.get()) : this;
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

    @Override
    public void trimToSize() {
        int s = this.size;
        if (items.length!= s) {
            this.items = s == 0 ? (X[]) ArrayUtil.EMPTY_OBJECT_ARRAY : Arrays.copyOf(this.items, s);
        }
    }

    @Override
    public X[] toArray() {
        int s = this.size;
        //return s > 0 ? Arrays.copyOf(items, s) : (X[]) ArrayUtil.EMPTY_OBJECT_ARRAY;
        return Arrays.copyOf(items, s);
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


    protected final long longify(LongObjectToLongFunction<X> f, long l) {
        int thisSize = this.size;
        if (thisSize > 0) {
            X[] ii = this.items;
            for (int i = 0; i < thisSize; i++) {
                l = f.longValueOf(l, ii[i]);
            }
        }
        return l;
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
            float y = function.floatValueOf(this.items[i]);
            if (y > max)
                max = y;
        }
        return max;
    }

    public long minValue(ToLongFunction<? super X> function) {
        long min = Long.MAX_VALUE;
        for (int i = 0, thisSize = this.size(); i < thisSize; i++) {
            long y = function.applyAsLong(this.items[i]);
            if (y < min)
                min = y;
        }
        return min;
    }

    public long maxValue(ToLongFunction<? super X> function) {
        return longify((max, x) -> Math.max(max, function.applyAsLong(x)), Long.MIN_VALUE);
    }

//    public X maxBy(float mustExceed, FloatFunction<? super X> function) {
//
//        if (ArrayIterate.isEmpty(items)) {
//            throw new NoSuchElementException();
//        }
//
//        X min = null;
//        float minValue = mustExceed;
//        for (int i = 0; i < size; i++) {
//            X next = items[i];
//            float nextValue = function.floatValueOf(next);
//            if (nextValue > minValue) {
//                min = next;
//                minValue = nextValue;
//            }
//        }
//        return min;
//
//    }


    @Override
    public boolean removeIf(Predicate<? super X> filter) {
        int s = size;
        int ps = s;
        X[] a = this.items;
        for (int i = 0; i < s; ) {
            if (filter.test(a[i])) {
                s--;
                System.arraycopy(a, i + 1, a, i, s - i);
            } else {
                i++;
            }
        }
        if (ps != s) {
            Arrays.fill(a, s, size, null);
            this.size = s;
            return true;
        }

        return false;
    }

//    public final boolean removeIf(Predicate<? super X> filter, List<X> displaced) {
//        int s = size();
//        int ps = s;
//        X[] a = this.items;
//        for (int i = 0; i < s; ) {
//            X ai = a[i];
//            if (ai == null || (filter.test(ai) && displaced.add(ai))) {
//                s--;
//                System.arraycopy(a, i + 1, a, i, s - i);
//                Arrays.fill(a, s, ps, null);
//            } else {
//                i++;
//            }
//        }
//        if (ps != s) {
//            this.size = s;
//            return true;
//        }
//
//        return false;
//    }

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

    final X[] fillArray(X[] array, boolean nullRemainder) {

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
    public boolean add(X x) {
        ensureCapacityForAdditional(1);
        addFast(x);
        return true;
    }

    public void add(int index, X element) {
        int sizeBefore = this.size;
        if (index > -1 && index < sizeBefore) {
            this.addAtIndex(index, element);
        } else if (index == sizeBefore) {
            this.add(element);
        } else {
            throw new ArrayIndexOutOfBoundsException(index);
        }
    }

    private void addAtIndex(int index, X element) {
        int oldSize = this.size++;
        if (this.items.length == oldSize) {
            X[] newItems = newArray(sizePlusFiftyPercent(oldSize));
            if (index > 0) {
                System.arraycopy(this.items, 0, newItems, 0, index);
            }

            System.arraycopy(this.items, index, newItems, index + 1, oldSize - index);
            this.items = newItems;
        } else {
            System.arraycopy(this.items, index, this.items, index + 1, oldSize - index);
        }

        this.items[index] = element;
    }

    public final int addAndGetSize(X x) {
        ensureCapacityForAdditional(1);
        addFast(x);
        return size;
    }

    public final byte addAndGetSizeAsByte(X x) {
        return (byte) addAndGetSize(x);
    }


    public final void ensureCapacityForAdditional(int num) {
        X[] ii = this.items;
        int s = this.size + num, l = ii.length;
        if (l < s) {
            this.items =
                    newArray((l == 0) ? Math.max(num, INITIAL_SIZE_IF_GROWING_FROM_EMPTY) : sizePlusFiftyPercent(s));
        }
    }

    private X[] newArray(int newCapacity) {
        //return newCapacity <= 0 ? (X[]) ArrayUtils.EMPTY_OBJECT_ARRAY : new Object[newCapacity];
        return Arrays.copyOf(items, newCapacity);
    }

    public final boolean addIfNotNull(Supplier<X> x) {
        return addIfNotNull(x.get());
    }

    private boolean addIfNotNull(@Nullable X x) {
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

    @SafeVarargs
    public final FasterList<X> addingAll(X... x) {
        int l = x.length;
        if (l > 0) {
            ensureCapacityForAdditional(l);
            addAll(x);
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
        return size == 0 ? Collections.emptyIterator() : new FasterListIterator<>(this);
    }

    public boolean containsInstance(X x) {
        X[] items = this.items;
        for (int i = 0, thisSize = this.size; i < thisSize; i++) {
            if (items[i] == x)
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
        if (index >= s) {
            return false;
        } else {
            Arrays.fill(items, index, s, null);
            this.size = index;
            return true;
        }
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
    public final void addFast(X x) {
        this.items[this.size++] = x;
    }

    public final void addAll(X a, X b) {
        ensureCapacityForAdditional(2);
        addFast(a);
        addFast(b);
    }

    @SafeVarargs
    public final void addAll(X... x) {
        addAll(x.length, x);
    }

    public final void addAll(int n, X[] x) {
        ensureCapacityForAdditional(n);
        addFast(x, n);
    }

    public final void addAllFaster(FasterList<X> source) {
        int s = source.size();
        if (s > 0) {
            this.ensureCapacityForAdditional(s);
            System.arraycopy(source.array(), 0, this.items, this.size, s);
            this.size += s;
        }
    }

    public final void addFast(X[] x, int n) {
        //if (n > 0) {
        X[] items = this.items;
        int size = this.size;
        if (n >= 0) System.arraycopy(x, 0, items, size, n);
        this.size += n;
        //}
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
     * before procedure executes on a cell, it nullifies the cell. equivalent to:
     * forEach(p) ... clear()
     * but faster
     *
     * @param each
     */
    public void clear(Consumer<? super X> each) {
        int s = this.size;
        if (s > 0) {
            X[] items = this.items;
            for (int i = 0; i < s; i++) {
                X ii = items[i];
                if (ii != null) {
                    items[i] = null;
                    each.accept(ii);
                }
            }
            this.size = 0;
        }
    }

    public <Y> void clearWith(BiConsumer<X, Y> each, Y y) {
        int s = this.size;
        if (s > 0) {
            X[] items = this.items;
            for (int i = 0; i < s; i++) {
                X ii = items[i];
                items[i] = null;
                each.accept(ii, y);
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

    public boolean removeNulls() {
        switch (size) {
            case 0:
                return false;

            case 1:
                if (items[0] == null) {
                    size = 0;
                    return true;
                } else
                    return false;

            default:
                return removeIf((Predicate)Objects::isNull);

        }
    }

    @Override
    @Deprecated public final boolean removeIf(org.eclipse.collections.api.block.predicate.Predicate<? super X> predicate) {
        return removeIf((Predicate<X>)predicate::test);
//
//        int nowFilled = 0;
//        int s0 = this.size;
//        if (s0 == 0)
//            return false;
//        X[] xx = this.items;
//        for (int i = 0; i < s0; i++) {
//            X x = xx[i];
//            if (!predicate.accept(x)) {
//                if (nowFilled != i) {
//                    xx[nowFilled] = x;
//                }
//                nowFilled++;
//            }
//        }
//
//        if (nowFilled < s0) {
//            Arrays.fill(items, this.size = nowFilled, s0, null);
//            return true;
//        }
//        return false;
    }

    public boolean removeInstance(X x) {
        return removeIf((Predicate)y -> x == y);
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
            ArrayUtil.swapObj(items, a, b);
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
    protected boolean nonNullEquals(FasterList<X> x) {
        if (this == x) return true;
        int s = this.size;
        if (s != x.size) {
            return false;
        }
        X[] a = this.items;
        X[] b = x.items;
        for (int i = 0; i < s; i++) {
            if (!a[i].equals(b[i])) {
                return false;
            }
        }
        return true;
    }


    protected void replaceLast(X y) {
        items[size - 1] = y;
    }

    @Override
    public final <P> void forEachWith(Procedure2<? super X, ? super P> procedure, P parameter) {
        int s = this.size;
        if (s > 0) {
            X[] items = this.items;
            for (int i = 0; i < s; i++)
                procedure.value(items[i], parameter);
        }
    }

    /**
     * returns this to an unallocated state
     */
    public void delete() {
        size = 0;
        items = newArray(0);
    }

    @Override
    public boolean allSatisfy(org.eclipse.collections.api.block.predicate.Predicate<? super X> predicate) {
        //return InternalArrayIterate.allSatisfy(this.items, this.size, predicate);
        int s = size;
        if (s > 0) {
            X[] items = this.items;
            for (int i = 0; i < s; i++) {
                if (!predicate.test(items[i])) {
                    return false;
                }
            }
            return true;
        }
        return true;
    }

    @Override
    public boolean anySatisfy(org.eclipse.collections.api.block.predicate.Predicate<? super X> predicate) {
        int s = size;
        if (s > 0) {
            X[] items = this.items;
            if (items!=null) { //if deleted
                s = Math.min(s, items.length);
                for (int i = 0; i < s; i++) {
                    if (predicate.accept(items[i])) {
                        return true;
                    }
                }
                return false;
            }
        }
        return false;
    }

    public boolean anySatisfy(int from, int to, Predicate<? super X> predicate2) {
        int s = size;
        if (s > 0 && s > from) {
            X[] items = this.items;
            for (int i = from; i < to; i++) {
                if (predicate2.test(items[i])) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    public <P> boolean anySatisfyWith(Predicate2<? super X, ? super P> predicate2, P parameter) {
        int s = size;
        if (s > 0) {
            X[] items = this.items;
            for (int i = 0; i < s; i++) {
                if (predicate2.accept(items[i], parameter)) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    public <P> boolean allSatisfyWith(Predicate2<? super X, ? super P> predicate2, P parameter) {
        int s = size;
        if (s > 0) {
            X[] items = this.items;
            for (int i = 0; i < s; i++) {
                if (!predicate2.accept(items[i], parameter)) {
                    return false;
                }
            }
            return true;
        }
        return true;
    }





    /**
     * modified from MutableIterator
     */
    public static final class FasterListIterator<T> implements Iterator<T> {
        protected final FasterList<T> list;
        int currentIndex;

        public FasterListIterator(FasterList<T> list) {
            this.list = list;
        }

        @Override
        public boolean hasNext() {
            return this.currentIndex < this.list.size;
        }

        @Override
        public T next() {
            return this.list.get(this.currentIndex++);
        }

        @Override
        public void remove() {
            this.list.removeFast(currentIndex-1);
            this.currentIndex--;
        }
    }

}
