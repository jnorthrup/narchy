package jcog.sort;

import jcog.Util;
import jcog.data.iterator.ArrayIterator;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static jcog.Util.ITEM;

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
public abstract class SortedArray<X> extends AbstractList<X> {

    public static final int BINARY_SEARCH_THRESHOLD = 8;
    private static final float GROWTH_RATE = 1.5f;

    public volatile X[] items = (X[]) ArrayUtils.EMPTY_OBJECT_ARRAY;

    protected volatile int size;

    public SortedArray() {
    }

    private static int grow(int oldSize) {
        return 1 + (int) Math.ceil(oldSize * GROWTH_RATE);

    }

    private static void swap(Object[] l, int a, int b) {
//        assert (a != b);
//        Object x = l[b];
//        l[b] = l[a];
//        l[a] = x;

        Object x = ITEM.getAcquire(l, b);
        ITEM.setRelease(l, b, ITEM.getAndSetAcquire(l, a, x));
    }


    public void sort(FloatFunction<X> x, int from, int to) {
        int[] stack = new int[sortSize(to - from) /* estimate */];
        qsortAtomic(stack, items, from /*dirtyStart - 1*/, to, x);
    }

    public static <X> void qsort(int[] stack, X[] c, int left, int right, FloatFunction<X> pCmp) {
        int stack_pointer = -1;
        int cLenMin1 = c.length - 1;
        final int SCAN_THRESH = 7;
        while (true) {
            int i, j;
            if (right - left <= SCAN_THRESH) {
                for (j = left + 1; j <= right; j++) {
                    X swap = c[j];
                    i = j - 1;
                    float swapV = pCmp.floatValueOf(swap);
                    while (i >= left && pCmp.floatValueOf(c[i]) < swapV) {
                        swap(c, i + 1, i--);
                    }
                    c[i + 1] = swap;
                }
                if (stack_pointer != -1) {
                    right = stack[stack_pointer--];
                    left = stack[stack_pointer--];
                } else {
                    break;
                }
            } else {

                int median = (left + right) / 2;
                i = left + 1;
                j = right;

                swap(c, i, median);

                float cl = pCmp.floatValueOf(c[left]);
                float cr = pCmp.floatValueOf(c[right]);
                if (cl < cr) {
                    swap(c, right, left);
                    float x = cr;
                    cr = cl;
                    cl = x;
                }
                float ci = pCmp.floatValueOf(c[i]);
                if (ci < cr) {
                    swap(c, right, i);
                    ci = cr;
                }
                if (cl < ci) {
                    swap(c, i, left);
                }

                X temp = c[i];
                float tempV = pCmp.floatValueOf(temp);

                while (true) {
                    while (i < cLenMin1 && pCmp.floatValueOf(c[++i]) > tempV) ;
                    while (j > 0 && /* <- that added */ pCmp.floatValueOf(c[--j]) < tempV) ;
                    if (j < i) {
                        break;
                    }
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
        }
    }
    /** untested, not finished */
    public static void qsortAtomic(int[] stack, Object[] c, int left, int right, FloatFunction pCmp) {
        int stack_pointer = -1;
        int cLenMin1 = c.length - 1;
        final int SCAN_THRESH = 7;
        while (true) {
            if (right - left <= SCAN_THRESH) {
                for (int j = left + 1; j <= right; j++) {
                    Object swap = ITEM.get(c, j);
                    int i = j - 1;
                    float swapV = pCmp.floatValueOf(swap);
                    while (i >= left && pCmp.floatValueOf(ITEM.get(c,i)) < swapV) {
                        swap(c, i + 1, i--);
                    }
                    ITEM.set(c, i+1, swap);
                }
                if (stack_pointer != -1) {
                    right = stack[stack_pointer--];
                    left = stack[stack_pointer--];
                } else {
                    break;
                }
            } else {

                int median = (left + right) / 2;
                int i = left + 1;

                swap(c, i, median);

                float cl = pCmp.floatValueOf(ITEM.get(c,left));
                float cr = pCmp.floatValueOf(ITEM.get(c, right));
                if (cl < cr) {
                    swap(c, right, left);
                    float x = cr;
                    cr = cl;
                    cl = x;
                }
                float ci = pCmp.floatValueOf(ITEM.get(c,i));
                if (ci < cr) {
                    swap(c, right, i);
                    ci = cr;
                }
                if (cl < ci) {
                    swap(c, i, left);
                }

                Object temp = ITEM.get(c,i);
                float tempV = pCmp.floatValueOf(temp);
                int j = right;

                while (true) {
                    while (i < cLenMin1 && pCmp.floatValueOf(ITEM.get(c,++i)) > tempV) ;
                    while (j > 0 && /* <- that added */ pCmp.floatValueOf(ITEM.get(c,--j)) < tempV) ;
                    if (j < i) {
                        break;
                    }
                    swap(c, j, i);
                }


                ITEM.set(c,left+1, ITEM.getAndSet(c,j,temp));

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
        }
    }


    /** TODO find exact requirements */
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
        return (X) ITEM.getOpaque(items, i);
    }


    /**
     * direct array access; use with caution ;)
     */
    public Object[] array() {
        return items;
    }

    @Override
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
            list[--this.size] = null;
            return previous;

//            X[] items = this.items;
//            X previous = (X) ITEM.getAndSetAcquire(items, index, null);
//            if (totalOffset > 0) {
//                size--;
//                for (int i = index; i < size; i++) {
//                    ITEM.setRelease(items, i, ITEM.getAcquire(items, i+1));
//                }
//                for (int i = size; i < items.length; i++) {
//                    ITEM.setRelease(items, i, null);
//                }
//                //System.arraycopy(items, index + 1, items, index, totalOffset);
//                //ITEM.setRelease(items, --this.size, null);
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
            list[--this.size] = null;
        }
    }

    public boolean remove(X removed, FloatFunction<X> cmp) {
        int i = indexOf(removed, cmp);
        return i != -1 && remove(i) != null;
    }

    @Override
    public void clear() {
        this.size = 0;
        Arrays.fill(items, null);
    }


    public final int add(final X element, FloatFunction<X> cmp) {
        float elementRank = cmp.floatValueOf(element);
        return add(element, cmp, elementRank);
    }

    private int add(X element, FloatFunction<X> cmp, float elementRank) {

        return (elementRank == elementRank) ? add(element, elementRank, cmp) : -1;
    }

    public int add(X element, float elementRank, FloatFunction<X> cmp) {
        int s = size;
        if (s < BINARY_SEARCH_THRESHOLD)
            return addLinear(element, elementRank, cmp, s);
        else
            return addBinary(element, elementRank, cmp, s);
    }

    private int addBinary(X element, float elementRank, FloatFunction<X> cmp, int size) {

        final int index = this.findInsertionIndex(elementRank, 0, size - 1, new int[1], cmp);

        return insert(element, index, elementRank, cmp, size);
    }

    private int insert(X element, int index, float elementRank, FloatFunction<X> cmp, int size) {
        final X last = items[size - 1];

        if (index == size || Util.fastCompare(cmp.floatValueOf(last), elementRank) < 0) {
            return addEnd(element);
        } else {
            return (index == -1 ? addEnd(element) : addInternal(index, element));
        }
    }

    private int addLinear(X element, float elementRank, FloatFunction<X> cmp, int size) {
        X[] l = this.items;
        size = Math.min(size, l.length);
        if (size > 0) {
            for (int i = 0; i < size; i++) {
                final X current = l[i];
                if (elementRank < cmp.floatValueOf(current)) {
                    return addInternal(i, element);
                }
            }
        }
        return addEnd(element);
    }

    @Override
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
        l[this.size++] = e;
        return s;
    }

    protected Object[] resize(int newLen) {
        X[] newList = newArray(newLen);
        System.arraycopy(items, 0, newList, 0, size);
        return this.items = newList;
    }

    private int addInternal(int index, X e) {
        if (index == -1)
            return -1;

        int s = this.size;
        if (index > -1 && index < s) {
            this.addAtIndex(index, e);
            return index;
        } else if (index == s) {
            return this.addEnd(e);
        }
        throw new UnsupportedOperationException();


    }

    private void addAtIndex(int index, X element) {
        int oldSize = this.size;
        X[] list = this.items;
        if (list.length == oldSize) {
            if (grows()) {

                this.size++;
                X[] newItems = newArray(grow(oldSize));
                if (index > 0) {
                    System.arraycopy(list, 0, newItems, 0, index);
                }
                System.arraycopy(list, index, newItems, index + 1, oldSize - index);
                this.items = list = newItems;
            } else {
                rejectExisting(list[index]);
            }

        } else {
            this.size++;
            System.arraycopy(list, index, list, index + 1, oldSize - index);
        }
        list[index] = element;
    }


    /**
     * called when the lowest value has been kicked out of the list by a higher ranking insertion
     */
    private void rejectExisting(X e) {

    }

    /**
     * generally, uses grow(oldSize) (not oldSize directly!) to get the final constructed array length
     */
    abstract protected X[] newArray(int oldSize);

    @Nullable
    public X removeFirst() {
        if (size == 0)
            return null;
        return remove(0);
    }

    public X removeLast() {

        return this.items[--size];


    }


    public int capacity() {
        return items.length;
    }

    /**
     * called when an item's sort order may have changed
     */
    public void adjust(int index, FloatFunction<X> cmp) {
        X[] l = this.items;
        float cur = cmp.floatValueOf(l[index]);

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
            int next = this.findInsertionIndex(cur, 0, s - 1, new int[1], cmp);
            if (next == index - 1) {

                swap(l, index, index - 1);
            } else if (next == index + 1) {

                swap(l, index, index + 1);
            } else {

                insert(remove(index), next, cur, cmp, s);
            }
        }
    }

    /**
     * tests for descending sort
     */
    public boolean isSorted(FloatFunction<X> f) {
        for (int i = 1; i < size; i++)
            if (f.floatValueOf(items[i - 1]) > f.floatValueOf(items[i]))
                return false;
        return true;
    }


    @SuppressWarnings("unchecked")
    public int indexOf(final X element, FloatFunction<X> cmp) {

		/*if (element == null)
            return -1;*/

        int size = size();
        if (size == 0)
            return -1;

        if (size >= BINARY_SEARCH_THRESHOLD) {

            final int[] rightBorder = {0};
            final int left = this.findInsertionIndex(cmp.floatValueOf(element), 0, size, rightBorder, cmp);

            X[] l = this.items;
            for (int index = left; index < rightBorder[0]; index++) {
                if (element.equals(l[index])) {
                    return index;
                }
            }


        }

        return indexOfInternal(element);

    }


    private int indexOfInternal(X e) {
        Object[] l = this.items;
        int s = this.size;
        for (int i = 0; i < s; i++) {
            if (e.equals(l[i]))
                return i;
        }
        return -1;
    }

    /**
     * find the position where the object should be inserted in to the list, or
     * the area of the list which should be searched for the object
     *
     * @param list        the list or sublist in where the index should be found
     * @param element     element for which the index should be found
     * @param left        the left index (inclusively)
     * @param right       the right index (exclusively)
     * @param rightBorder This parameter will be modified inside the method, thus you
     *                    can analyse it after the method execution. Is used to know the
     *                    right border.
     * @return first index of the element where the element should be inserted
     */
    private int findInsertionIndex(
            float elementRank, final int left, final int right,
            final int[] rightBorder, FloatFunction<X> cmp) {

        assert (right >= left);

        if ((right - left) < BINARY_SEARCH_THRESHOLD) {
            rightBorder[0] = right;
            return findFirstIndex(elementRank, left, right, cmp);
        }

        final int midle = left + (right - left) / 2;

        X[] list = this.items;

        final X midleE = list[midle];


        float ev = cmp.floatValueOf(midleE);
        final int comparedValue = Util.fastCompare(ev, elementRank);
        if (comparedValue == 0) {
//            //scan until the next element weaker than midlE/ev
            int index = midle;
//            int max = capacity();
//            for (; index < max; index++) {
//                final X e = list[index];
//                if (e == null) {
//                    rightBorder[0] = index;
//                    return index;
//                }
//                int i = Util.fastCompare(cmp.floatValueOf(e), elementRank);
//                if (0 != i) {
//                    assert(i > 0); //must be weaker if table is consistent
//                    break;
//                }
//            }
//            if (index < max) {
            rightBorder[0] = index;
            return index;
//            } else
//                return -1; //table full
        }

        boolean c = (0 < comparedValue);


        int nextLeft = c ? left : midle;
        int nextRight = c ? midle : right;

        return findInsertionIndex(elementRank, nextLeft, nextRight, rightBorder, cmp);

    }


    /**
     * searches for the first index found for given element
     *
     * @param list  the list or sublist which should be invastigated
     * @param left  left index (inclusively)
     * @param right right index (exclusively)
     * @return
     */
    private int findFirstIndex(float elementRank,
                               final int left, final int right, FloatFunction<X> cmp) {


        X[] l = this.items;
        for (int index = left; index < right; ) {
            X anObject = l[index];
            if (0 < Util.fastCompare(cmp.floatValueOf(anObject), elementRank)) {
                return index;
            }
            index++;
        }
        return right;


    }

    /**
     * Returns the first (lowest) element currently in this list.
     */
    @Nullable
    public final X first() {
        return this.isEmpty() ? null : (X) ITEM.getOpaque(items, 0);
    }

    /**
     * Returns the last (highest) element currently in this list.
     */
    @Nullable
    public final X last() {
        int size = this.size;
        if (size == 0) return null;
        X[] ll = items;
        return (X) ITEM.getOpaque(ll, Math.min(ll.length - 1, size - 1));

    }

    @Override
    public final void forEach(Consumer<? super X> action) {
        forEach(-1, action);
    }

    public final void forEach(int n, Consumer<? super X> action) {
        int s = (n == -1) ? size : Math.min(size, n);
        if (s > 0) {
            X[] l = items;
            for (int i = 0; i < s; i++)
                action.accept((X) ITEM.getOpaque(l,i));
        }
    }

    public final void removeRange(int start, int _end, Consumer<? super X> action) {
        int end = (_end == -1) ? size : Math.min(size, _end);
        if (start<end) {
            X[] l = items;
            for (int i = start; i < end; i++)
                action.accept(l[i]);
            removeRange(start, end);
        }
    }


    protected void removeRange(int fromIndex, int toIndex) {
        if (fromIndex <= toIndex)
            shiftTailOverGap(this.items, fromIndex, toIndex);
        else
            throw new IndexOutOfBoundsException();
    }

    private void shiftTailOverGap(Object[] es, int lo, int hi) {
        int ne = this.size;
        System.arraycopy(es, hi, es, lo, ne - hi);
        int ns = (this.size -= hi - lo);
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
        return IntStream.range(0, size()).mapToObj(i -> (X)ITEM.getOpaque(items, i));
    }

    @Override
    public Iterator<X> iterator() {
        //return ArrayIterator.get(items, size());
        return new ArrayIterator.AtomicArrayIterator(items, size());
    }

}
