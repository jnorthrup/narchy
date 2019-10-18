package jcog.sort;

import jcog.Util;
import jcog.data.atomic.MetalAtomicIntegerFieldUpdater;
import jcog.data.iterator.ArrayIterator;
import jcog.pri.ScalarValue;
import jcog.util.ArrayUtil;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.stream.IntStream;
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

    public static final int BINARY_SEARCH_THRESHOLD = 4;

    /** when scanning for identity equality */
    public static final int BINARY_SEARCH_THRESHOLD_SCAN = 16;

    protected static final MetalAtomicIntegerFieldUpdater<SortedArray> SIZE =
            new MetalAtomicIntegerFieldUpdater(SortedArray.class, "size");
    private static final float GROWTH_RATE = 1.25f;
    public /*volatile*/ X[] items = (X[]) ArrayUtil.EMPTY_OBJECT_ARRAY;
    protected int size;


    protected static int grow(int oldSize) {
        return 1 + (int) (oldSize * GROWTH_RATE);

    }


    private static <X> boolean eq(X element, X ii, boolean eqByIdentity) {
        return (element == ii) || (!eqByIdentity && element.equals(ii));
    }

    public void sort(ToIntFunction<X> x, int from, int to) {
        int size = this.size;
        if (size == 0)
            return;
        from = Math.max(0, from);
        to = Util.clamp(size-1, from, to);
        if (from >= to)
            return;


        IntifySmoothSort.smoothSort(items, from, to, x);
        //SmoothSort.smoothSort(items, from, to, (a,b)->Integer.compare(x.applyAsInt(b), x.applyAsInt(a)));

        //qsort(items, from, to, x);

//        ArrayUtil.quickSort(from, to,
//            (a,b)->Integer.compare(x.applyAsInt(items[a]), x.applyAsInt(items[b])),
//            (a,b)->swap(items, a, b) );


    }

    public final X getSafe(int i) {
        X[] ii = this.items;
        int s = Math.min(ii.length, size);
        return s == 0 || i >= s ? null : ii[i];
    }


    public final X get(int i) {
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

//    /** simply swaps the item at the index with the last item */
//    public final boolean removeFaster(X x, int index) {
//        int s = size;
//        if (s > index) {
//            X[] items = this.items;
//            if (items[index] == x) {
//                //items[index] = null;
//                items[index] = items[SIZE.decrementAndGet(this)];
//                return true;
//            }
//        }
//        return false;
//    }

    public final boolean removeFast(X x, int index) {
        int totalOffset = this.size - index - 1;
        if (totalOffset < 0)
            return false;

        X[] items = this.items;
        if (items[index] == x) {
            if (totalOffset > 0)
                System.arraycopy(items, index + 1, items, index, totalOffset);
            items[SIZE.decrementAndGet(this)] = null;
            return true;
        }
        return false;
    }

    public boolean remove(X removed, FloatFunction<X> cmp) {
        int i = indexOf(removed, cmp);
        return i != -1 && remove(i) != null;
    }

    public void delete() {
        SIZE.getAndSet(this, 0);
        items = (X[])ArrayUtil.EMPTY_OBJECT_ARRAY;
    }


    public void clear() {
        int s = SIZE.getAndSet(this, 0);
        if (s > 0)
            Arrays.fill(items, 0, s, null);
    }

    /**
     * doesnt nullify existing items
     */
    public void clearWeak() {
        SIZE.set(this, 0);
    }

    public final int add(X element, FloatFunction<X> cmp) {
        return addRanked(element, cmp.floatValueOf(element), cmp);
    }

    public int addRanked(X element, float elementRank, FloatFunction<X> cmp) {
        int i = (elementRank == elementRank) ? addSafe(element, elementRank, cmp) : -1;
        if (i < 0)
            rejectOnEntry(element);
        return i;
    }

    protected void rejectOnEntry(X e) {

    }

    /** assumes elementRank is finite */
    public final int addSafe(X element, float elementRank, FloatFunction<X> cmp) {
        //assert (elementRank == elementRank);

        int index = indexOf(element, elementRank, cmp, false, true);

        int size1 = size;
//        assert (index != -1);
        return index == size1 ?
                addEnd(element, elementRank) :
                addAtIndex(index, element, elementRank, size1);

//        if (!isSorted(cmp)) throw new WTF();
    }

    public final boolean isEmpty() {
        return size == 0;
    }

    protected boolean grows() {
        return true;
    }

    protected int addEnd(X x, float elementRank) {
        int s = this.size;
        if (capacity() <= s) {
            if (!grows())
                return -1;

            resize(grow(s));
        }
        items[SIZE.getAndIncrement(this)] = x;
        return s;
    }

    protected void resize(int newLen) {
        assert (newLen >= size);
        this.items = copyOfArray(items, newLen);
    }

    protected int addAtIndex(int index, X element, float elementRank, int oldSize) {

        X[] list = this.items;
        boolean adding;
        int c = capacity();
        if (c == oldSize) {
            if (!grows()) {
                rejectExisting(list[oldSize - 1]); //pop
                adding = false;
            } else {
                int newCapacity = grow(oldSize);
                assert (newCapacity > c);
                this.items = list = copyOfArray(list, newCapacity);
                adding = true;
                c = newCapacity;
            }
        } else {
            adding = true;
        }

        if (adding)
            SIZE.getAndIncrement(this);

        int ss = Math.min(oldSize - 1, c - 2);
        if (ss + 1 - index >= 0)
            System.arraycopy(list, index, list, index + 1, ss + 1 - index);

        list[index] = element;

        return index;

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

    public @Nullable X removeFirst() {
        return size == 0 ? null : remove(0);
    }

    public X removeLast() {
        int s = SIZE.decrementAndGet(this);
        X[] items = this.items;
        X x = items[s];
        items[s] = null;
        return x;
    }


    public int capacity() {
        return items.length;
    }

    /**
     * tests for descending sort
     */
    public boolean isSorted(FloatFunction<X> f) {
        X[] ii = this.items;
        //TODO use valueAt(
        int bound = size;
        return IntStream.range(1, bound).noneMatch(i -> f.floatValueOf(ii[i - 1]) >= f.floatValueOf(ii[i]));
    }

    public int indexOf(X element, FloatFunction<X> cmp) {
        return indexOf(element, cmp.floatValueOf(element) /*Float.NaN*/, cmp, false, false);
    }


    public final int indexOf(X element, float elementRank /* can be NaN for forFind */, FloatFunction<X> cmp, boolean eqByIdentity, boolean forInsertionOrFind) {

        int s = size;
        if (s == 0)
            return forInsertionOrFind ? 0 : -1;

        int left = 0, right = s;
        X[] items = this.items;
        int searchThresh = (!forInsertionOrFind && eqByIdentity) ?  BINARY_SEARCH_THRESHOLD_SCAN : BINARY_SEARCH_THRESHOLD;
        main:
        while (right - left >= searchThresh) {

            int mid = left + (right - left) / 2;

            switch (Float.compare(valueAt(mid, cmp), elementRank)) {
                case 0:
                    if (forInsertionOrFind)
                        return mid + 1; /* after existing element */
                    else
                        break main;
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
//        if (right - left <= BINARY_SEARCH_THRESHOLD) {
        for (int i = left; i < right; i++) {
            if (!forInsertionOrFind) {
                if (eq(element, items[i], eqByIdentity))
                    return i;

            } else {
                if (0 < Float.compare(valueAt(i, cmp), elementRank))
                    return i;
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

    public float valueAt(int item, FloatFunction<X> cmp) {
        return cmp.floatValueOf(items[item]);
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
    public final @Nullable X first() {
        return size == 0 ? null : items[0];
    }

    /**
     * Returns the last (highest) element currently in this list.
     */
    public final @Nullable X last() {
        int size = this.size;
        return size == 0 ? null : items[size - 1];
    }

    public void forEach(Consumer<? super X> action) {
        X[] items = this.items;
        int s = Math.min(items.length, size);
        for (int i = 0; i < s; i++)
            action.accept(items[i]);
    }

    public final boolean whileEach(Predicate<? super X> action) {
        return whileEach(-1, action);
    }

    public final boolean whileEach(int n, Predicate<? super X> action) {
        int s0 = this.size;
        int s = (n == -1) ? s0 : Math.min(s0, n);
        if (s > 0) {
            X[] ii = items;
            //(X) ITEM.getOpaque(ii,i)
            return Arrays.stream(ii, 0, s).filter(Objects::nonNull).allMatch(action);
        }
        return true;
    }

//    public final void removeRange(int start, int _end, Consumer<? super X> action) {
//        int end = (_end == -1) ? size : Math.min(size, _end);
//        if (start < end) {
//            removeRangeSafe(start, end, action);
//        }
//    }

    public void removeRangeSafe(int start, int end, Consumer<? super X> action) {
        X[] l = items;
        for (int i = start; i < end; i++)
            action.accept(l[i]);
        removeRange(start, end);
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
        return ArrayIterator.stream(ii, Math.min(capacity(), s));
//        return s > 0 ?
//                Arrays.stream(ii, 0, Math.min(ii.length, s))
//                :
//                Stream.empty();
    }

    public Iterator<X> iterator() {
        return ArrayIterator.iterateN(items, size);
    }

    public Iterator<X> iteratorNonNull() {
        return ArrayIterator.iterateNonNullN(items, size);
    }


    public void reprioritize(X existing, int posBefore, float delta, float priAfter, FloatFunction<X> cmp) {

        //only reindex if exceeds threshold to previous or next item

        X[] items = this.items;

        if (delta > 0) {
            if (posBefore > 0) {
                if (-valueAt(posBefore - 1, cmp) > priAfter - ScalarValue.EPSILON / 2)
                    return; //order doesnt change
            } else {
                //already highest
                return;
            }
        } else if (delta < 0) {
            if (posBefore < size() - 1) {
                if (-valueAt(posBefore+1, cmp) < priAfter + ScalarValue.EPSILON / 2)
                    return; //order doesnt change
            } else {
                //already lowest
                return;
            }
        }

        if (!removeFast(existing, posBefore))
            throw new ConcurrentModificationException(); //item order changed

        int inserted = addSafe(existing, -priAfter, cmp);
        assert (inserted != -1);

    }
    public void replace(Function<X,X> each) {
        X[] items = this.items;
        for (int i = 0; i < size; i++) {
            items[i] = each.apply(items[i]);
        }
    }
}


//    private static void swap(Object[] l, int a, int b) {
//        if (a != b) {
//            Object x = l[b];
//            l[b] = l[a];
//            l[a] = x;
//        }

//        Object x = ITEM.getAcquire(l, b);
//        ITEM.setAt(l, b, ITEM.getAndSetAcquire(l, a, x));
//    }


//    static final int QSORT_SCAN_THRESH = 6;
//
//    private static <X> void qsort(X[] c, int left, int right, ToIntFunction<X> pCmp) {
//        int[] stack = null;
//        int stack_pointer = -1;
//        while (right - left > QSORT_SCAN_THRESH) {
//            int median = (left + right) / 2;
//            int i = left + 1;
//
//            swap(c, i, median);
//
//            final X vci = c[i];
//            int pci = pCmp.applyAsInt(vci);
//            int pcl = pCmp.applyAsInt(c[left]);
//            int pcr = pCmp.applyAsInt(c[right]);
//
//            int pivot;
//            if (pcl < pcr) {
//                swap(c, right, left);
//                int x = pcr;
//                pcr = pcl;
//                pcl = x;
//                pivot = i == left ? pcr : (i == right ? pcl : pci);
//            } else {
//                pivot = i == left ? pcl : (i == right ? pcr : pci);
//            }
//
//            if (pivot < pcr) {
//                swap(c, right, i);
//                pivot = pcr;
//            }
//            if (pcl < pivot) {
//                swap(c, i, left);
//            }
//
//
//            /** safety limit in case the order of the items changes while sorting; external factors could cause looping indefinitely */
//            int j = right;
//            int limit = Util.sqr(right-left);
//            while (true) {
//                while (i < right && pCmp.applyAsInt(c[++i]) > pci && --limit > 0) { }
//                while (j > left && /* <- that added */ pCmp.applyAsInt(c[--j]) < pci && --limit > 0) { }
//                if (j <= i || limit <= 0)
//                    break;
//                swap(c, j, i);
//            }
//
//            c[left + 1] = c[j];
//            c[j] = vci;
//
//            int a, b;
//            if (right - i + 1 >= j - left) {
//                a = i;
//                b = right;
//                right = j - 1;
//            } else {
//                a = left;
//                b = j - 1;
//                left = i;
//            }
//
//            if (stack_pointer == -1)
//                stack = new int[Math.max(1,2+(int) Math.ceil(2*Math.log(1 + right - left)/Math.log(2)))]; //HACK
//            stack[++stack_pointer] = a;
//            stack[++stack_pointer] = b;
//        }
//
//        qsort_bubble(pCmp, c, left, right, stack, stack_pointer);
//    }
//
//    private static <X> void qsort_bubble(ToIntFunction<X> pCmp, X[] c, int left, int right, int[] stack, int stack_pointer) {
//        while (true) {
//
//            for (int j = left + 1; j <= right; j++) {
//                int i = j - 1;
//                X cj = c[j];
//                if (i >= left) {
//                    int pcj = pCmp.applyAsInt(cj);
//                    while (i >= left && pCmp.applyAsInt(c[i]) < pcj) {
//                        swap(c, i + 1, i--);
//                    }
//                }
//                c[i + 1] = cj;
//            }
//
//            if (stack_pointer < 0)
//                break;
//
//            right = stack[stack_pointer--];
//            left = stack[stack_pointer--];
//
//        }
//    }


//    /**
//     * TODO find exact requirements
//     */
//    static int sortSize(int size) {
//        if (size < 16)
//            return 4;
//        else if (size < 64)
//            return 6;
//        else if (size < 128)
//            return 8;
//        else if (size < 2048)
//            return 16;
//        else
//            return 32;
//    }

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
