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
package jcog.util;

import jcog.list.FasterList;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

/**
 * from: https://gist.github.com/sonelson/8116915
 *
 * @version 1.0
 * @author Sol
 * @since December, 2013
 */
public class Array2DIterable<X> implements Iterable<X> {

    private X[][] array2D;
    private int pos = 0;
    public FasterList<X> order;
    //private ShortArrayList order;

    public Array2DIterable(X x[][]) {
        array2D = x;
        //collection = new ArrayList<>(); // diamond op in jdk7
        int cols = array2D[0].length;
        int rows = array2D.length;
        int area = rows * cols;
        order = new FasterList(area);

        for (int j = 0; j < rows; j++) {
            for (int i = 0; i < cols; i++) {
                order.add(x[j][i]);
            }
        }

        //fillSpiralArray(array2D, 0, 0, true, true, area);
    }


    @NotNull
    @Override
    public Iterator<X> iterator() {
        return order.iterator();
    }


//    /**
//     * This method comprises the core piece to lay out the elements of the 2
//     * dimensional array in a data structure that can be traversed in an
//     * clockwise inward spiral.
//     *
//     * The core logic uses recursion to iterate through the 2D array row and
//     * column elements, incrementing the column/row indexes keeping one of the
//     * array dimensions row/column index constant. The increment cycle is
//     * followed by the decrement iteration, and the processing conditions are
//     * similar with the exception of the indexes decremented in this cycle. The
//     * increment/decrement iteration cycles continue till all the elements of
//     * the 2 dimensional array are processed. The logic has hooks in place to
//     * discard duplicates.
//     *
//     * For example, to process an array [3][4] with values:
//     *                      1  2   3  4
//     *                     10  11  l2 5
//     *                      9  8   7  6
//     *
//     *      Step#1: Start with keeping row index constant i.e. a[0] []
//     *              Iterate the column elements incrementing the col-index by 1
//     *                  i.e. a [0][0] ... a[0][3]; values => {1,2,3,4}
//     *      Step#2: Continue with keeping col index constant i.e. a[] [3]
//     *              Iterate the row elements incrementing the row-index by 1
//     *                  i.e. a[1][3] ... a[2][3]; values => {5, 6}
//     *      Step#3: Continue with keeping row index constant i.e. a[2] []
//     *              Iterate the column elements decrementing the col-index by 1
//     *                      i.e. a [2][3] ... a[2][0]; values => {7,8,9}
//     *      Step#4: Continue with keeping col index constant i.e. a[] [0]
//     *              Iterate the row elements decrementing the row-index by 1
//     *                      i.e. a[1][0] ... a[1][0]; values => {10}
//     *
//     * Increment/Decrement iteration cycles (steps: 1 through 4) repeat from the
//     * last processed point till all elements are consumed.
//     *
//     *      Step#5: Continue with keeping row index constant i.e. a[1] []
//     *              Iterate the column elements incrementing the col-index by 1
//     *                  i.e. a [1][0] ... a[1][2]; values => {11, 12}     *
//     *
//     * @param arr2D - int[][] 2 dimensional char array input
//     * @param
//     */
//    private void fillSpiralArray(X[][] arr2D, int rowPos, int colPos,
//            boolean colIter, boolean plusIter, int countDown) {
//
//        int rowHeight = arr2D.length;
//        int columnHeight = arr2D[0].length;
//
//        if (columnHeight == 1) {
//
//            for (int row = 0; row < rowHeight; row++) {
//
//                collection.add(arr2D[row][0]);
//            }
//
//            return;
//        }
//
//        // TODO - find a better approach to break the recursive loop
//        if (countDown < 1) {
//            System.out.println("All array elements processed :" + countDown);
//            return;
//        }
//
//        if (plusIter) {
//
//            // TO DO - use logger debug
//            System.out.println("===== Processing increment loop =====");
//
//            if (colIter) {
//
//                System.out.println("===== process Column index increment =====");
//
//                for (int col = rowPos; col < columnHeight; col++) {
//                    //System.out.println("row [" + rowPos + "] and col [" + col + "] =>" + arr2D[rowPos][col]);
//
//                    if (collection.contains(Integer.valueOf(arr2D[rowPos][col]))) {
//                        //System.out.println("Done row/column processing :");
//                        //return;
//                        continue;
//                    } else {
//                        collection.add(Integer.valueOf(arr2D[rowPos][col]));
//                        colPos = col;
//                    }
//                }
//                rowPos = rowPos + 1;
//                plusIter = false;
//
//                countDown = countDown - 1;
//
//            } else {
//
//                System.out.println("===== process Row index increment =====");
//
//                for (int row = rowPos; row < rowHeight; row++) {
//
//                    X aa = arr2D[row][colPos];
//                    if (collection.contains(aa)) {
//                        //return;
//                        continue;
//                    } else {
//                        collection.add(aa);
//                        rowPos = row;
//                    }
//
//                }
//
//                colPos = colPos - 1;
//                countDown = countDown - 1;
//            }
//
//        } else {
//
//            // TO DO - use logger debug
//            System.out.println("===== Processing decrement loop =====");
//
//            if (colIter) {
//
//                System.out.println("===== process Column index decrement =====");
//
//                for (int col = colPos; col >= 0; col--) {
//
//                    if (collection.contains(Integer.valueOf(arr2D[rowPos][col]))) {
//                        //return;
//                        continue;
//                    } else {
//                        collection.add(Integer.valueOf(arr2D[rowPos][col]));
//                        colPos = col;
//                    }
//
//                }
//
//                rowPos = rowPos - 1;
//                plusIter = true;
//                countDown = countDown - 1;
//
//            } else {
//
//                System.out.println("===== process Row index decrement =====");
//
//                for (int row = rowPos; row > 0; row--) {
//
//                    if (collection.contains(Integer.valueOf(arr2D[row][colPos]))) {
//                        //return;
//                        continue;
//                    } else {
//                        collection.add(Integer.valueOf(arr2D[row][colPos]));
//                        rowPos = row;
//                    }
//
//                }
//                colPos = colPos + 1;
//                countDown = countDown - 1;
//            }
//        }
//
//        // recursive call
//        fillSpiralArray(arr2D, rowPos, colPos, !colIter, !plusIter, countDown);
//
//    }
}