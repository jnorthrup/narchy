/*
 *  Array2DIntgIterator implements Iterator interface and provides the functionality 
 *  to iterate through the elements of a 2 dimensional int array in an   
 *  clockwise inward spiral starting from the top left cell.
 * 
 *  A [3,4] array of characters with these values:
 *             1   2   3   4
 *             10  11  l2  5
 *             9   8   7   6
 *  would iterate as:
 *             1 2 3 4 5 6 7 8 9 10 11 12
 * 
 */
package jcog.data.iterator;

import jcog.data.list.FasterList;

import java.util.Arrays;
import java.util.Iterator;

/**
 * from: https:
 *
 * @version 1.0
 * @author Sol
 * @since December, 2013
 */
public class Array2DIterable<X> implements Iterable<X> {

    public FasterList<X> order;
    

    public Array2DIterable(X[][] x) {

        var cols = x[0].length;
        var rows = x.length;
        var area = rows * cols;
        order = new FasterList(area);

        for (var xes : x) {
            order.addAll(Arrays.asList(xes).subList(0, cols));
        }

        
    }

    @Override
    public Iterator<X> iterator() {
        return order.iterator();
    }


    public final X get(int i) {
        return order.get(i);
    }
}