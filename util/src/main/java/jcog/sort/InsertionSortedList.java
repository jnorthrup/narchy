package jcog.sort;


import jcog.data.list.FasterList;

import java.util.Arrays;

/**
 * insertion sorted list. It is constructed with a comparator that
 * can compare two objects and sorted objects accordingly. When you add an object
 * to the list, it is inserted in the correct place. Object that are equal
 * according to the comparator, will be in the list in the order that they were
 * added to this list. Add only objects that the comparator can compare.</p>
 */
public class InsertionSortedList<E extends Comparable> extends FasterList<E> /* implements SortedSet<E> */ {

    /**
     * indicates if the resulting ordering is different from the input order
     */
    public boolean orderChangedOrDeduplicated = false;


    public InsertionSortedList(int capacity) {
        super(capacity);
    }


    /**
     * uses array directly
     */
    public InsertionSortedList(E[] toSort) {
        super(0, toSort.clone());
        Arrays.fill(items, null);
        for (E e : toSort)
            add(e);
    }

    public InsertionSortedList(E[] toSort, E[] scratch) {
        super(0, scratch);
        for (E e : toSort)
            add(e);
    }

    /**
     * <p>
     * Adds an object to the list. The object will be inserted in the correct
     * place so that the objects in the list are sorted. When the list already
     * contains objects that are equal according to the comparator, the new
     * object will be inserted immediately after these other objects.</p>
     *
     * @param x the object to be added
     */
    @Override
    public final boolean add(final E x) {


        int s = size;
        if (s > 0) {
            int low = 0, high = s - 1;

            while (low <= high) {
                int mid = (low + high) / 2;

                int cmp = x.compareTo(get(mid));

                if (cmp > 0) {
                    low = mid + 1;
                } else if (cmp < 0) {
                    high = mid - 1;
                } else {
                    orderChangedOrDeduplicated = true;
                    return false;
                }
            }

            if (low != s) {
                orderChangedOrDeduplicated = true;
                add(low, x);
                return true;
            }

        }

        addFast(x);
        return true;
    }

}
