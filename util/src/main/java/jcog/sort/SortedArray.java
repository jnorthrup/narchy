package jcog.sort;

import jcog.Util;
import jcog.data.atomic.MetalAtomicIntegerFieldUpdater;
import jcog.data.iterator.ArrayIterator;
import jcog.util.ArrayUtils;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;

/**
 * {@link SortedList_1x4} is a decorator which decorates {@link List}. Keep in
 * mind that {@link SortedList_1x4} violates the contract of {@link List}
 * interface, because inserted elements don't stay in the inserted order, but
 * will be sorted according to used comparator.
 * <p>
 * {@link SortedList_1x4} supports two types of sorting-strategy:
 * <ul>
 * <li> {@link SearchType#BinarySearch} - uses binary search and is suitable for
 * lists like {@link ArrayList} or {@link TreeList}, where elements can be
 * cheaply accessed by their index.</br> The complexity of {link SortType#Array}
 * is equal to N*logN</li>
 * <li>{@link SearchType#LinearSearch} - uses insertion sort to insert new
 * elements. Insertion sort starts to search for element from the beginning of
 * the list until the index for insertion is found and inserts then the new
 * element. This type of sorting is suitable for {@link LinkedList}. insertion
 * Sort has complexity between N (best-case) and N^2 (worst-case)</li>
 * </ul>
 * <p/>
 * {@link SortedList_1x4} implements all methods provided by
 * {@link NavigableSet}, but requirements of such methods are adopted to the
 * {@link List} interface which can contain duplicates and thus differs from
 * {@link NavigableSet}.
 * <p>
 * <p>
 * Here is a small examle how to store few integers in a {@link SortedList_1x4}
 * :</br></br> final List<Integer> sortedList =
 * Collections_1x2.sortedList();</br> sortedList.addAll(Arrays.asList(new
 * Integer[] { 5, 0, 6, 5, 3, 4 }));</br>
 * System.out.print(sortedList.toString());</br>
 * </p>
 * The output on console is: {@code [0, 3, 4, 5, 5, 6]}
 *
 * @param <X>
 * @author Andreas Hollmann
 * <p>
 * TODO extend FasterList as a base
 */
public class SortedArray<X> /*extends AbstractList<X>*/ implements Iterable<X> {

    public static final int BINARY_SEARCH_THRESHOLD = 2;
    private static final float GROWTH_RATE = 1.25f;

    public /*volatile*/ X[] items = (X[]) ArrayUtils.EMPTY_OBJECT_ARRAY;

    private static final AtomicIntegerFieldUpdater<SortedArray> SIZE =
            new MetalAtomicIntegerFieldUpdater(SortedArray.class, "size");

    protected volatile int size;

    public SortedArray() {
    }

    protected static int grow(int oldSize) {
        return 1 + (int) (oldSize * GROWTH_RATE);

    }

    private static void swap(Object[] l, int a, int b) {
        if (a != b) {
            Object x = l[b];
            l[b] = l[a];
            l[a] = x;
        }

//        Object x = ITEM.getAcquire(l, b);
//        ITEM.setAt(l, b, ITEM.getAndSetAcquire(l, a, x));
    }


//    public void sort(FloatFunction<X> x, int from, int to) {
//        int[] stack = new int[sortSize(to - from) /* estimate */];
//        qsort(stack, items, from /*dirtyStart - 1*/, to, x);
//    }
//
//    public static <X> void qsort(int[] stack, X[] c, int left, int right, FloatFunction<X> pCmp) {
//        int stack_pointer = -1;
//        int cLenMin1 = c.length - 1;
//        final int SCAN_THRESH = 7;
//        while (true) {
//            int i, j;
//            if (right - left <= SCAN_THRESH) {
//                for (j = left + 1; j <= right; j++) {
//                    X swap = c[j];
//                    i = j - 1;
//                    float swapV = pCmp.floatValueOf(swap);
//                    while (i >= left && pCmp.floatValueOf(c[i]) < swapV) {
//                        swap(c, i + 1, i--);
//                    }
//                    c[i + 1] = swap;
//                }
//                if (stack_pointer != -1) {
//                    right = stack[stack_pointer--];
//                    left = stack[stack_pointer--];
//                } else {
//                    break;
//                }
//            } else {
//
//                int median = (left + right) / 2;
//                i = left + 1;
//                j = right;
//
//                swap(c, i, median);
//
//                float cl = pCmp.floatValueOf(c[left]);
//                float cr = pCmp.floatValueOf(c[right]);
//                if (cl < cr) {
//                    swap(c, right, left);
//                    float x = cr;
//                    cr = cl;
//                    cl = x;
//                }
//                float ci = pCmp.floatValueOf(c[i]);
//                if (ci < cr) {
//                    swap(c, right, i);
//                    ci = cr;
//                }
//                if (cl < ci) {
//                    swap(c, i, left);
//                }
//
//                X temp = c[i];
//                float tempV = pCmp.floatValueOf(temp);
//
//                while (true) {
//                    while (i < cLenMin1 && pCmp.floatValueOf(c[++i]) > tempV) ;
//                    while (j > 0 && /* <- that added */ pCmp.floatValueOf(c[--j]) < tempV) ;
//                    if (j < i) {
//                        break;
//                    }
//                    swap(c, j, i);
//                }
//
//                c[left + 1] = c[j];
//                c[j] = temp;
//
//                int a, b;
//                if (right - i + 1 >= j - left) {
//                    a = i;
//                    b = right;
//                    right = j - 1;
//                } else {
//                    a = left;
//                    b = j - 1;
//                    left = i;
//                }
//
//                stack[++stack_pointer] = a;
//                stack[++stack_pointer] = b;
//            }
//        }
//    }

    public void sort(ToIntFunction<X> x, int from, int to) {
        from = Math.max(0, from);
        to = Math.max(from, Math.min(size - 1, to));
        if (from == to)
            return;
        int[] stack = new int[to - from + 1 /*sortSize(to - from)*/ /* estimate */];
        qsort(stack, items, from /*dirtyStart - 1*/, to, x);
    }

    private static void qsort(int[] stack, Object[] c, int left, int right, ToIntFunction pCmp) {
        int stack_pointer = -1;
        final int SCAN_THRESH = 4;
        while (right - left > SCAN_THRESH) {
            int median = (left + right) / 2;
            int i = left + 1;
            int j = right;

            swap(c, i, median);

            int cl = pCmp.applyAsInt(c[left]), cr = pCmp.applyAsInt(c[right]);
            int ci;
            if (cl < cr) {
                swap(c, right, left);
                int x = cr;
                cr = cl;
                cl = x;
                ci = i == left ? cr : (i == right ? cl : pCmp.applyAsInt(c[i]));
            } else {
                ci = i == left ? cl : (i == right ? cr : pCmp.applyAsInt(c[i]));
            }

            if (ci < cr) {
                swap(c, right, i);
                ci = cr;
            }
            if (cl < ci) {
                swap(c, i, left);
            }

            Object temp = c[i];
            int tempV = pCmp.applyAsInt(temp);

            while (true) {
                while (i < right && pCmp.applyAsInt(c[++i]) > tempV) ;
                while (j > left && /* <- that added */ pCmp.applyAsInt(c[--j]) < tempV) ;
                if (j <= i)
                    break;
                swap(c, j, i);
            }

            c[left + 1] = c[j];
            c[j] = temp;

            int a, b;
            if (right - i + 1 >= j - left) {
                a = i;
                b = right;
                right = j - 1;
            } else {
                a = left;
                b = j - 1;
                left = i;
            }

            stack[++stack_pointer] = a;
            stack[++stack_pointer] = b;
        }

        qsortBubble(pCmp, c, left, right, stack, stack_pointer);
    }

    private static void qsortBubble(ToIntFunction pCmp, Object[] c, int left, int right, int[] stack, int stack_pointer) {
        while (true) {
            for (int j = left + 1; j <= right; j++) {
                int i = j - 1;
                Object swap = c[j];
                int swapV = pCmp.applyAsInt(swap);
                while (i >= left && pCmp.applyAsInt(c[i]) < swapV) {
                    swap(c, i + 1, i--);
                }
                c[i + 1] = swap;
            }
            if (stack_pointer != -1) {
                right = stack[stack_pointer--];
                left = stack[stack_pointer--];
            } else {
                return;
            }
        }
    }

//    /** untested, not finished */
//    public static void qsortAtomic(int[] stack, Object[] c, int left, int right, FloatFunction pCmp) {
//        int stack_pointer = -1;
//        int cLenMin1 = c.length - 1;
//        final int SCAN_THRESH = 7;
//        while (true) {
//            if (right - left <= SCAN_THRESH) {
//                for (int j = left + 1; j <= right; j++) {
//                    Object swap = ITEM.get(c, j);
//                    int i = j - 1;
//                    float swapV = pCmp.floatValueOf(swap);
//                    while (i >= left && pCmp.floatValueOf(ITEM.get(c,i)) < swapV) {
//                        swap(c, i + 1, i--);
//                    }
//                    ITEM.setAt(c, i+1, swap);
//                }
//                if (stack_pointer != -1) {
//                    right = stack[stack_pointer--];
//                    left = stack[stack_pointer--];
//                } else {
//                    break;
//                }
//            } else {
//
//                int median = (left + right) / 2;
//                int i = left + 1;
//
//                swap(c, i, median);
//
//                float cl = pCmp.floatValueOf(ITEM.get(c,left));
//                float cr = pCmp.floatValueOf(ITEM.get(c, right));
//                if (cl < cr) {
//                    swap(c, right, left);
//                    float x = cr;
//                    cr = cl;
//                    cl = x;
//                }
//                float ci = pCmp.floatValueOf(ITEM.get(c,i));
//                if (ci < cr) {
//                    swap(c, right, i);
//                    ci = cr;
//                }
//                if (cl < ci) {
//                    swap(c, i, left);
//                }
//
//                Object temp = ITEM.get(c,i);
//                float tempV = pCmp.floatValueOf(temp);
//                int j = right;
//
//                while (true) {
//                    while (i < cLenMin1 && pCmp.floatValueOf(ITEM.get(c,++i)) > tempV) ;
//                    while (j > 0 && /* <- that added */ pCmp.floatValueOf(ITEM.get(c,--j)) < tempV) ;
//                    if (j < i) {
//                        break;
//                    }
//                    swap(c, j, i);
//                }
//
//
//                ITEM.setAt(c,left+1, ITEM.getAndSet(c,j,temp));
//
//                int a, b;
//                if (right - i + 1 >= j - left) {
//                    a = i;
//                    b = right;
//                    right = j - 1;
//                } else {
//                    a = left;
//                    b = j - 1;
//                    left = i;
//                }
//
//                stack[++stack_pointer] = a;
//                stack[++stack_pointer] = b;
//            }
//        }
//    }


    /**
     * TODO find exact requirements
     */
    static int sortSize(int size) {
        if (size < 16)
            return 4;
        else if (size < 64)
            return 6;
        else if (size < 128)
            return 8;
        else if (size < 2048)
            return 16;
        else
            return 32;
    }

    public X get(int i) {
//        int s = size;
//        if (s == 0)
//            throw new NoSuchElementException();
//        if (i >= s)
//            throw new ArrayIndexOutOfBoundsException();
        //return items[i];
        return items[i];//(X) ITEM.getOpaque(items, i);
    }


    /**
     * direct array access; use with caution ;)
     */
    public X[] array() {
        return items;
    }

    public int size() {
        return size;
    }

    public X remove(int index) {

        int totalOffset = this.size - index - 1;
        if (totalOffset >= 0) {
            X[] list = this.items;
            X previous = list[index];
            if (totalOffset > 0) {
                System.arraycopy(list, index + 1, list, index, totalOffset);
            }
            list[SIZE.decrementAndGet(this)] = null;
            return previous;

//            X[] items = this.items;
//            X previous = (X) ITEM.getAndSetAcquire(items, index, null);
//            if (totalOffset > 0) {
//                size--;
//                for (int i = index; i < size; i++) {
//                    ITEM.setAt(items, i, ITEM.getAcquire(items, i+1));
//                }
//                for (int i = size; i < items.length; i++) {
//                    ITEM.setAt(items, i, null);
//                }
//                //System.arraycopy(items, index + 1, items, index, totalOffset);
//                //ITEM.setAt(items, SIZE.decrementAndGet(this), null);
//            }
//            return previous;
        }
        return null;
    }


    public void removeFast(int index) {
        int totalOffset = this.size - index - 1;
        if (totalOffset >= 0) {
            X[] list = this.items;
            if (totalOffset > 0) {
                System.arraycopy(list, index + 1, list, index, totalOffset);
            }
            list[SIZE.decrementAndGet(this)] = null;
        }
    }

    public boolean remove(X removed, FloatFunction<X> cmp) {
        int i = indexOf(removed, cmp);
        return i != -1 && remove(i) != null;
    }

    public void clear() {
        Arrays.fill(items, 0, SIZE.getAndSet(this, 0), null);
    }


    public final int add(final X element, FloatFunction<X> cmp) {
        float elementRank = cmp.floatValueOf(element);
        int i = (elementRank == elementRank) ? add(element, elementRank, cmp) : -1;
        if (i < 0)
            rejectOnEntry(element);
        return i;
    }

    protected void rejectOnEntry(X e) {

    }

    public int add(X element, float elementRank, FloatFunction<X> cmp) {
        int s = size;
        assert (elementRank == elementRank);

        final int index = find(element, elementRank, cmp, false, true);

        int result = insert(element, index, s);

//        if (!isSorted(cmp))
//            throw new WTF();

        return result;
    }

    private int insert(X element, int index, int size) {
        assert (index != -1);
        if (index == size) {
            return addEnd(element);
        } else {
            if (addAtIndex(index, element, size))
                return index;
            else
                return -1;
        }
    }


    public final boolean isEmpty() {
        return size == 0;
    }

    protected boolean grows() {
        return true;
    }

    private int addEnd(X e) {
        int s = this.size;
        Object[] l = this.items;
        if (l.length == s) {
            if (grows()) {
                l = resize(grow(s));
            } else {
                return -1;
            }
        }
        l[SIZE.incrementAndGet(this) - 1] = e;
        return s;
    }

    protected X[] resize(int newLen) {
        assert (newLen >= size);
        return this.items = copyOfArray(items, newLen);
//        X[] newList = newArray(newLen);
//        System.arraycopy(items, 0, newList, 0, size);
//        return this.items = newList;
    }


    private boolean addAtIndex(int index, X element, int oldSize) {

        X[] list = this.items;
        boolean adding;
        if (list.length == oldSize) {
            if (!grows()) {
                rejectExisting(list[oldSize - 1]); //pop
                adding = false;
            } else {
                int newCapacity = grow(oldSize);
                assert (newCapacity > list.length);
                this.items = list = copyOfArray(list, newCapacity);
                adding = true;
            }
        } else {
            adding = true;
        }

        if (adding)
            SIZE.getAndIncrement(this);

        int ss = Math.min(oldSize - 1, list.length - 2);
        if (ss + 1 - index >= 0)
            System.arraycopy(list, index, list, index + 1, ss + 1 - index);

        list[index] = element;

        return true;

    }


    /**
     * called when the lowest value has been kicked out of the list by a higher ranking insertion
     */
    protected void rejectExisting(X e) {

    }

    /**
     * generally, uses grow(oldSize) (not oldSize directly!) to get the final constructed array length
     */
    protected X[] copyOfArray(X[] xx, int s) {
        return Arrays.copyOf(xx, s);
    }

    @Nullable
    public X removeFirst() {
        if (size == 0)
            return null;
        return remove(0);
    }

    public X removeLast() {

        return this.items[SIZE.decrementAndGet(this)];


    }


    public int capacity() {
        return items.length;
    }

    /**
     * an adjustment: called when an item's sort order may have changed
     */
    public void partialSort(int index, FloatFunction<X> cmp) {
        X[] l = this.items;
        final float cur = cmp.floatValueOf(l[index]);

        boolean reinsert = false;

        if (index > 0) {
            float f = cmp.floatValueOf(l[index - 1]);
            if (f > cur)
                reinsert = true;
        }

        int s = this.size;
        if (!reinsert) {
            if (index < s - 1) {
                float f = cmp.floatValueOf(l[index + 1]);
                if (f < cur)
                    reinsert = true;
            }
        }

        if (reinsert) {
            int next = find(null, cur, cmp, true, true);
            if (next == index) {
                //in the correct pos
            } else if (next == index - 1) {
                if (index >= 1)
                    swap(l, index, index - 1);
            } else if (next == index + 1) {

                if (index < size() - 1)
                    swap(l, index, index + 1);

            } else {

                insert(remove(index), next, s);

            }


        }
    }

    /**
     * tests for descending sort
     */
    public boolean isSorted(FloatFunction<X> f) {
        X[] ii = this.items;
        for (int i = 1; i < size; i++) {
            if (f.floatValueOf(ii[i - 1]) >= f.floatValueOf(ii[i]))
                return false;
        }
        return true;
    }


    public int indexOf(final X element, FloatFunction<X> cmp) {
        return indexOf(element, cmp, false);
    }

    public final int indexOf(final X element, FloatFunction<X> cmp, boolean eqByIdentity) {

        return find(element, Float.NaN, cmp, eqByIdentity, false);
    }

    public int indexOf(float rank, FloatFunction<X> cmp) {
        return find(null, rank, cmp, false, true);
    }

    private int find(final X element, float elementRank /* can be NaN for forFind */, FloatFunction<X> cmp, boolean eqByIdentity, boolean forInsertionOrFind) {
        int s = size;
        if (s == 0)
            return forInsertionOrFind ? 0 : -1;

        int left = 0, right = s;
        X[] items = this.items;
        while (right - left >= BINARY_SEARCH_THRESHOLD) {

                final int mid = left + (right - left) / 2;

                final X m = items[mid];
                if (!forInsertionOrFind) {
                    if (m == element || (!eqByIdentity && m.equals(element))) {
                        return mid;
                    }
                }

                final int comparedValue = Util.fastCompare(cmp.floatValueOf(m), elementRank);

                switch (comparedValue) {
                    case 0:
                        if (forInsertionOrFind)
                            return mid + 1; /* after existing element */
                        break;
                    case 1:
                        right = mid;
                        break;
                    case -1:
                        left = mid;
                        break;
                    default:
                        throw new UnsupportedOperationException();
                }

        }
        if (right - left <= BINARY_SEARCH_THRESHOLD) {
            int i;
            for (i = left; i < right; i++) {
                X x = items[i];
                if (!forInsertionOrFind) {
                    boolean eq = eq(element, x, eqByIdentity);
                    if (eq) {
                        return i;
                    }
                } else {
                    if (0 < Util.fastCompare(cmp.floatValueOf(x), elementRank)) {
                        return i;
                    }
                }
            }
        }

        if (forInsertionOrFind)
            return right; //after the range
        else {
            return element != null && exhaustiveFind() ?
                    indexOfExhaustive(element, eqByIdentity) :
                    -1;
        }

    }

    private static <X> boolean eq(X element, X ii, boolean eqByIdentity) {
        return (element == ii) || (!eqByIdentity && element.equals(ii));
    }

    /**
     * needs to be true if the rank of items is known to be stable.  then binary indexOf lookup does not need to perform an exhaustive search if not found by rank
     */
    protected boolean exhaustiveFind() {
        return true;
    }


    private int indexOfExhaustive(X e, boolean eqByIdentity) {
        Object[] l = this.items;
        int s = this.size;
        for (int i = 0; i < s; i++) {
            Object ll = l[i];
            if (eqByIdentity ? (e == ll) : e.equals(ll))
                return i;
        }
        return -1;
    }


    /**
     * Returns the first (lowest) element currently in this list.
     */
    @Nullable
    public final X first() {
        return size == 0 ? null : items[0];
//        X[] ii = items;
//        return ii.length == 0 ? null :
//                //(X) ITEM.getOpaque(items, 0);
//                ii[0];
    }

    /**
     * Returns the last (highest) element currently in this list.
     */
    @Nullable
    public final X last() {
        int size = this.size;
        if (size == 0) return null;
        return items[size - 1];
//        X[] ll = items;
//        int i = Math.min(ll.length - 1, size - 1);
        //return (X) ITEM.getOpaque(ll, i);
        //return ll[i];

    }

    public final void forEach(Consumer<? super X> action) {
        int s = size;
        for (int i = 0; i < s; i++)
            action.accept(items[i]);
    }

    public final void forEach(int n, Consumer<? super X> action) {
        whileEach(n, x -> {
            action.accept(x);
            return true;
        });
    }

    public final boolean whileEach(Predicate<? super X> action) {
        return whileEach(-1, action);
    }

    public final boolean whileEach(int n, Predicate<? super X> action) {
        int s0 = this.size;
        int s = (n == -1) ? s0 : Math.min(s0, n);
        if (s > 0) {
            X[] ii = items;
            for (int i = 0; i < s; i++)
                if (!action.test(
                        //(X) ITEM.getOpaque(ii,i)
                        ii[i]
                ))
                    return false;
        }
        return true;
    }

    public final void removeRange(int start, int _end, Consumer<? super X> action) {
        int end = (_end == -1) ? size : Math.min(size, _end);
        if (start < end) {
            X[] l = items;
            for (int i = start; i < end; i++)
                action.accept(l[i]);
            removeRange(start, end);
        }
    }


    private void removeRange(int fromIndex, int toIndex) {
        if (fromIndex <= toIndex)
            shiftTailOverGap(this.items, fromIndex, toIndex);
        else
            throw new IndexOutOfBoundsException();
    }

    private void shiftTailOverGap(Object[] es, int lo, int hi) {
        int ne = this.size;
        System.arraycopy(es, hi, es, lo, ne - hi);
        int ns = SIZE.addAndGet(this, -(hi - lo));
        Arrays.fill(es, ns, ne, null);
    }


//    public boolean removeAbove(int index) {
//        int s = this.size;
//        if (index >= s)
//            return false;
//        Arrays.fill(items, index, s, null);
//        this.size = index;
//        return true;
//    }

    public Stream<X> stream() {
        //return ArrayIterator.stream(items, size());

        X[] ii = items;
        int s = size();
        /*(X) ITEM.getOpaque(items, i)*/
        return ArrayIterator.stream(ii, Math.min(ii.length, s));
//        return s > 0 ?
//                Arrays.stream(ii, 0, Math.min(ii.length, s))
//                :
//                Stream.empty();
    }

    public Iterator<X> iterator() {
        return ArrayIterator.get(items, size());
    }

}
