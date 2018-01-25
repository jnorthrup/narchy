package jcog.list;

import jcog.Util;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.eclipse.collections.api.block.predicate.primitive.IntObjectPredicate;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.utility.ArrayIterate;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
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

    public FasterList(int capacity) {
        super(capacity);
    }

    public FasterList(Iterable<X> copy) {
        copy.forEach(this::add);
    }

    public FasterList(Iterator<X> copy) {
        super();
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

//    public FasterList(Collection<X> copy, X... extra) {
//        this.items = (X[])new Object[copy.size() + extra.length];
//        addAll(copy);
//        for (X x : extra)
//            add(x);
//    }

    /**
     * uses array directly
     */
    public FasterList(int size, X[] x) {
        super(size, x);
    }


    public FasterList(X[] x) {
        super(x);
    }


    @Override
    public int size() {
        //assert (size >= 0);
        return size;
    }

    public void clearHard() {
        this.size = 0;
        this.items = (X[]) ArrayUtils.EMPTY_OBJECT_ARRAY;
    }

    public X removeLast() {
        //assert(size > 0);
        if (size == 0)
            throw new ArrayIndexOutOfBoundsException();
        X[] ii = this.items;
        X x = ii[--size];
        ii[size] = null; //GC help
        return x;
    }

//    @Nullable
//    public X removeLastElseNull() {
//        int s = size;
//        return s == 0 ? null : removeLast();
//    }

    @Override
    public X get(int index) {
        //if (index < this.size) {
        return items[index];
        //}
    }
    public X get(Random random) {
        //if (index < this.size) {
        return items[random.nextInt(size)];
        //}
    }

    @Nullable
    public final X getSafe(int index) {
        if (index >= 0 && index < this.size) {
            return items[index];
        } else {
            return null;
        }
    }


//    public final boolean addIfCapacity(X newItem) {
//        X[] ii = this.items;
//        int s;
//        if (ii.length <= (s = this.size++)) {
//            return false;
//        }
//        ii[s] = newItem;
//        return true;
//    }

    @Override
    public int indexOf(/*@NotNull*/ Object object) {
        //return InternalArrayIterate.indexOf(this.items, this.size, object);
        int s = size;
        X[] items = this.items;
        for (int i = 0; i < s; i++) {
            if (object.equals(items[i]))
                return i;
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
        Object[] i = items;
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
//    public X minBy(float mustExceed, FloatFunction<? super X> function) {
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
//            if (nextValue < minValue) {
//                min = next;
//                minValue = nextValue;
//            }
//        }
//        return min;
//
//    }

    //    /** use this to get the fast null-terminated version;
//     *  slightly faster; use with caution
//     * */
//    public <E> E[] toNullTerminatedArray(E[] array) {
//        array = toArrayUnpadded(array);
//        final int size = this.size;
//        if (array.length > size) {
//            array[size] = null;
//        }
//        return array;
//    }

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
    public X[] toArray(IntFunction<X[]> arrayBuilder) {
        return fillArray(arrayBuilder.apply(size));
    }


//    /** does not pad the remaining values in the array with nulls */
//    X[] toArrayUnpadded(X[] array) {
//        if (array.length < this.size)        {
//            //resize larger
//            array = (X[]) Array.newInstance(array.getClass().getComponentType(), this.size);
//        }
//        return fillArray(array);
//    }

    public final X[] fillArrayNullPadded(X[] array) {
        int s = size;
        int l = array.length;
        if (array == null || array.length < (s + 1)) {
            array = (X[]) Array.newInstance(array.getClass().getComponentType(), s + 1);
        }
        System.arraycopy(items, 0, array, 0, s);
        if (s < l)
            Arrays.fill(array, s, l, null); //pad remainder
        return array;
    }

    public final X[] fillArray(X[] array) {
        int s = size;
        int l = array.length;
        System.arraycopy(items, 0, array, 0, s);
        if (s < l)
            Arrays.fill(array, s, l, null); //pad remainder
        return array;
    }


//    public final X[] toNullTerminatedUnpaddedArray(X[] array) {
//        final int s = this.size; //actual size
//        if (array.length < (s+1)) {
//            array = (X[]) Array.newInstance(array.getClass().getComponentType(), s+1);
//        }
//        System.arraycopy(this.items, 0, array, 0, s);
//        array[s] = null;
//        return array;
//    }

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
        setSize(0);
    }


//    public final void clear0() {
//        this.items = (X[]) ZERO_SIZED_ARRAY;
//        this.size = 0;
//    }

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
                        new Object[INITIAL_SIZE_IF_GROWING_FROM_EMPTY]
                        :
                        this.copyItemsWithNewCapacity(sizePlusFiftyPercent(this.size))
        );
    }

    static private int sizePlusFiftyPercent(int oldSize) {
        int result = oldSize + (oldSize / 2) + 1;
        return result < oldSize ? (Integer.MAX_VALUE - 8) : result;
    }

    private Object[] copyItemsWithNewCapacity(int newCapacity) {
        Object[] newItems = new Object[newCapacity];
        System.arraycopy(this.items, 0, newItems, 0, Math.min(this.size, newCapacity));
        return newItems;
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
        for (Object j : items) {
            if (j == null)
                break; //end of list
            each.accept(n++, j);
        }
        return size();
    }

    public FasterList addingAll(X... x) {
        for (X y : x)
            add(y);
        return this;
    }

    public final void setFast(int index, X t) {
        items[index] = t;
    }

    public void removeFast(int index) {
        X[] ii = items;
        System.arraycopy(ii, index + 1, ii, index, size - index - 1);
        ii[--size] = null;
    }

    public void removeBelow(int index) {
        if (size <= index)
            return; // no change
        this.items = Arrays.copyOfRange(items, 0, this.size = index);
    }

    public int capacity() {
        return items.length;
    }

    public <E> E[] arrayClone(Class<? extends E> type) {
        E[] array = (E[]) Array.newInstance(type, size);
        return toArray(array);
    }

    /**
     * dangerous unless you know the array has enough capacity
     */
    public void addWithoutResizeCheck(X x) {
        this.items[this.size++] = x;
    }

//    @Override
//    @Deprecated /* try to use toArrayRecycled where possible */
//    public <E> E[] toArray(E[] array) {
//        return super.toArray(array);
//    }

    public X[] toArrayRecycled(IntFunction<X[]> ii) {
        X[] a = items;
        int s = size;
        if (s == a.length && a.getClass() != Object[].class)
            return a;
        else
            return toArray(ii);
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

    /**
     * forcibly sets the size
     */
    public void setSize(int s) {
        this.size = s;
    }


    public X addThen(X x) {
        add(x);
        return x;
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

            //TODO fast case 2?

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
                // keep it
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

}
