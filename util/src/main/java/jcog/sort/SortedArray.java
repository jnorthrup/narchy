package jcog.sort;

import jcog.Util;
import jcog.util.SubArrayIterator;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.api.block.function.primitive.FloatFunction;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

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
 * @param <E>
 * @author Andreas Hollmann
 *
 * TODO extend FasterList as a base
 */
public abstract class SortedArray<E> extends AbstractCollection<E> {


    public static final int BINARY_SEARCH_THRESHOLD = 8;
    private static final float GROWTH_RATE = 1.5f;

    public E[] list = (E[]) ArrayUtils.EMPTY_OBJECT_ARRAY;

    protected int size;

    public SortedArray() {
    }

    private static int grow(int oldSize) {
        return 1 + (int) Math.ceil(oldSize * GROWTH_RATE);
        
    }

    private static void swap(Object[] l, int a, int b) {
        assert (a != b);
        Object x = l[b];
        l[b] = l[a];
        l[a] = x;
    }

    /**
     * direct array access; use with caution ;)
     */
    public Object[] array() {
        return list;
    }

    @Override
    public int size() {
        return size;
    }

    public E remove(int index) {

        int totalOffset = this.size - index - 1;
        if (totalOffset >= 0) {
            E[] list = this.list;
            E previous = list[index];
            if (totalOffset > 0) {
                System.arraycopy(list, index + 1, list, index, totalOffset);
            }
            list[--this.size] = null;
            return previous;
        }
        return null;
    }





























    public void removeFast(int index) {
        int totalOffset = this.size - index - 1;
        if (totalOffset >= 0) {
            E[] list = this.list;
            if (totalOffset > 0) {
                System.arraycopy(list, index + 1, list, index, totalOffset);
            }
            list[--this.size] = null;
        }
    }

    public boolean remove(E removed, FloatFunction<E> cmp) {
        int i = indexOf(removed, cmp);
        return i != -1 && remove(i) != null;
    }

    @Override
    public void clear() {
        
        Arrays.fill(list, null);
        
        this.size = 0;
    }

    @Override
    public void forEach(Consumer<? super E> action) {
        for (E x : list) {
            if (x != null) {
                action.accept(x);
            } else {
                break; 
            }
        }
    }

    public final int add(final E element, FloatFunction<E> cmp) {
        float elementRank = cmp.floatValueOf(element);
        return add(element, cmp, elementRank);
    }

    private int add(E element, FloatFunction<E> cmp, float elementRank) {
        
        return (elementRank == elementRank) ? add(element, elementRank, cmp) : -1;
    }

    public int add(E element, float elementRank, FloatFunction<E> cmp) {
        int s = size;
        if (s < BINARY_SEARCH_THRESHOLD)
            return addLinear(element, elementRank, cmp, s);
        else
            return addBinary(element, elementRank, cmp, s);
    }

    private int addBinary(E element, float elementRank, FloatFunction<E> cmp, int size) {
        
        final int index = this.findInsertionIndex(elementRank, 0, size - 1, new int[1], cmp);

        return insert(element, index, elementRank, cmp, size);
    }

    private int insert(E element, int index, float elementRank, FloatFunction<E> cmp, int size) {
        final E last = list[size - 1];

        if (index == size || Util.fastCompare(cmp.floatValueOf(last), elementRank) < 0) {
            return addEnd(element);
        } else {
            return (index == -1 ? addEnd(element) : addInternal(index, element));
        }
    }

    private int addLinear(E element, float elementRank, FloatFunction<E> cmp, int size) {
        E[] l = this.list;
        if (size > 0 && l.length > 0) {
            for (int i = 0; i < size; i++) {
                final E current = l[i];
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

    private int addEnd(E e) {
        int s = this.size;
        Object[] l = this.list;
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
        E[] newList = newArray(newLen);
        System.arraycopy(list, 0, newList, 0, size);
        return this.list = newList;
    }

    private int addInternal(int index, E e) {
        int s = this.size;
        if (index > -1 && index < s) {
            this.addAtIndex(index, e);
            return index;
        } else if (index == s) {
            return this.addEnd(e);
        }
        throw new UnsupportedOperationException();




    }

    private void addAtIndex(int index, E element) {
        int oldSize = this.size;
        E[] list = this.list;
        if (list.length == oldSize) {
            if (grows()) {

                this.size++;
                E[] newItems = newArray(grow(oldSize)); 
                if (index > 0) {
                    System.arraycopy(list, 0, newItems, 0, index);
                }
                System.arraycopy(list, index, newItems, index + 1, oldSize - index);
                this.list = list = newItems;
            } else {
                rejectExisting(list[index]);
            }

        } else {
            this.size++;
            System.arraycopy(list, index, list, index + 1, oldSize - index);
        }
        list[index] = element;
    }


    /** called when the lowest value has been kicked out of the list by a higher ranking insertion */
    private void rejectExisting(E e) {

    }

    /**
     * generally, uses grow(oldSize) (not oldSize directly!) to get the final constructed array length
     */
    abstract protected E[] newArray(int oldSize);

    @Nullable
    public E removeFirst() {
        if (size == 0)
            return null;
        return remove(0);
    }

    public E removeLast() {
        
        return this.list[--size];
        
        
    }

    @Override
    public Iterator<E> iterator() {
        
        int s = size();
        return (s == 0) ? Collections.emptyIterator() : new SubArrayIterator<>(list, 0, s);
    }

    public int capacity() {
        return list.length;
    }

    /**
     * called when an item's sort order may have changed
     */
    public void adjust(int index, FloatFunction<E> cmp) {
        E[] l = this.list;
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

    public boolean isSorted(FloatFunction<E> f) {
        for (int i= 1; i < size; i++)
            if (f.floatValueOf(list[i-1]) >= f.floatValueOf(list[i]))
                return false;
        return true;
    }

















    @SuppressWarnings("unchecked")
    public int indexOf(final E element, FloatFunction<E> cmp) {

		/*if (element == null)
            return -1;*/

        int size = size();
        if (size == 0)
            return -1;

        if (size >= BINARY_SEARCH_THRESHOLD) {

            final int[] rightBorder = {0};
            final int left = this.findInsertionIndex(cmp.floatValueOf(element), 0, size, rightBorder, cmp);

            E[] l = this.list;
            for (int index = left; index < rightBorder[0]; index++) {
                if (element.equals(l[index])) {
                    return index;
                }
            }

            
            
        }

        return indexOfInternal(element);

    }














































































































































    private int indexOfInternal(E e) {
        Object[] l = this.list;
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
            final int[] rightBorder, FloatFunction<E> cmp) {

        assert (right >= left); 

        if ((right - left) <= BINARY_SEARCH_THRESHOLD) {
            rightBorder[0] = right;
            return findFirstIndex(elementRank, left, right, cmp);
        }

        final int midle = left + (right - left) / 2;

        E[] list = this.list;

        final E midleE = list[midle];


        final int comparedValue = Util.fastCompare(cmp.floatValueOf(midleE), elementRank);
        if (comparedValue == 0) {
            
            int index = midle;
            for (; index >= 0; ) {
                final E e = list[index];
                if (0 != Util.fastCompare(cmp.floatValueOf(e), elementRank)) {
                    break;
                }
                index--;
            }
            rightBorder[0] = index;
            return index;
        }

        boolean c = (0 < comparedValue);

        return this.findInsertionIndex(elementRank, c ? left : midle, c ? midle : right, rightBorder, cmp);
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
                               final int left, final int right, FloatFunction<E> cmp) {


        E[] l = this.list;
        for (int index = left; index < right; ) {
            E anObject = l[index];
            if (0 <= Util.fastCompare(cmp.floatValueOf(anObject), elementRank)) {
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
    public final E first() {
        return this.isEmpty() ? null : list[0];
    }

    /**
     * Returns the last (highest) element currently in this list.
     */
    @Nullable
    public final E last() {
        int size = this.size;
        if (size == 0) return null;
        E[] ll = list;
        return ll[Math.min(ll.length - 1, size - 1)];

    }

    


























































































































































































































































































































































































































































































































































































































































































































































































































}
