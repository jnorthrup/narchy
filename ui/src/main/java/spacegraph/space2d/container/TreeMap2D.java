package spacegraph.space2d.container;

import jcog.pri.ScalarValue;
import jcog.tree.rtree.rect.RectFloat2D;
import spacegraph.space2d.widget.Graph2D;
import spacegraph.util.MovingRectFloat2D;

/**
 * Implements the Squarified Treemap layout published by
 * Mark Bruls, Kees Huizing, and Jarke J. van Wijk
 * <p>
 * Squarified Treemaps
 * https://www.win.tue.nl/~vanwijk/stm.pdf
 * https://github.com/peterdmv/treemap/
 */

public class TreeMap2D<X> extends DynamicLayout2D<X, MovingRectFloat2D> {

    @Override
    protected MovingRectFloat2D newContainer() {
        return new MovingRectFloat2D();
    }

    @Override
    protected void layout(Graph2D<X> g) {
        nodes.sortThisByFloat((a)-> {
            //a.w = a.h = 1;
            a.w = g.bounds.w;
            a.h = g.bounds.h;
            return a.node.pri;
        });
        int end = nodes.size() - 1;
        total = areaSum(0, end);
        layout(0, end, g.bounds);
    }

//    public MovingRectFloat2D[] sortDescending(MovingRectFloat2D[] items) {
//        if (items == null || items.length == 0) {
//            return null;
//        }
//        MovingRectFloat2D[] inputArr = new MovingRectFloat2D[items.length];
//        System.arraycopy(items, 0, inputArr, 0, items.length);
//        int length = inputArr.length;
//
//        quickSortDesc(inputArr, 0, length - 1);
//
//        return inputArr;
//    }

    int mid;
    float total;
    public void layout(int start, int end, RectFloat2D bounds) {
        if (start > end) {
            return;
        }
        if (start == end) {
            nodes.get(start).set(bounds);
        }

        this.mid = start;
        while (mid < end) {
            if (highestAspect(start, mid, bounds) > highestAspect(start, mid + 1, bounds)) {
                mid++;
            } else {
                RectFloat2D newBounds = layoutRow(start, mid, bounds);
                layout(mid + 1, end, newBounds);
            }
        }
    }

    float highestAspect(int start, int end, RectFloat2D bounds) {

        layoutRow(start, end, bounds);

        float max = Float.NEGATIVE_INFINITY;
        for (int i = start; i <= end; i++) {
            float r = this.nodes.get(i).aspectExtreme();

            if (r > max) {
                max = r;
            }
        }
        return max;
    }

    RectFloat2D layoutRow(int start, int end, RectFloat2D bounds) {
        boolean isHorizontal = bounds.w > bounds.h;

        float rowSize = areaSum(start, end);
        float rowRatio = rowSize / total;
        assert(rowRatio==rowRatio): start + ".." + end + ": " + rowSize + " " + total;
        float offset = 0;

        for (int i = start; i <= end; i++) {
            MovingRectFloat2D x = this.nodes.get(i);

            float p = x.node.pri;
            if (p != p || p < ScalarValue.EPSILON) {
                x.size(0.1f,0.1f);
                continue;
            }

            float ratio = p / rowSize;


            assert(ratio==ratio): p + " " + rowSize;

            RectFloat2D r;
            if (isHorizontal) {
                r = RectFloat2D.X0Y0WH(bounds.x,
                        bounds.y + bounds.h * offset,
                        bounds.w * rowRatio,
                        bounds.h * ratio);
            } else {
                r = RectFloat2D.X0Y0WH(
                        bounds.x + bounds.w * offset,
                        bounds.y,
                        bounds.w * ratio,
                        bounds.h * rowRatio);
            }
            x.set(r);
            offset += ratio;
        }
        if (isHorizontal) {
            return RectFloat2D.X0Y0WH(bounds.x + bounds.w * rowRatio, bounds.y, bounds.w - bounds.w * rowRatio, bounds.h);
        } else {
            return RectFloat2D.X0Y0WH(bounds.x, bounds.y + bounds.h * rowRatio, bounds.w, bounds.h - bounds.h * rowRatio);
        }
    }


    float areaSum(int start, int end) {
        float sum = 0;
        for (int i = start; i <= end; i++)
            sum += nodes.get(i).node.pri; //getSize();
        return sum;
    }

//    private void quickSortDesc(MovingRectFloat2D[] inputArr, int lowerIndex, int higherIndex) {
//
//        int i = lowerIndex;
//        int j = higherIndex;
//        // calculate pivot number
//        float pivot = inputArr[lowerIndex+(higherIndex-lowerIndex)/2].getSize();
//        // Divide into two arrays
//        while (i <= j) {
//            /**
//             * In each iteration, we will identify a number from left side which
//             * is greater then the pivot value, and also we will identify a number
//             * from right side which is less then the pivot value. Once the search
//             * is done, then we exchange both numbers.
//             */
//            while (inputArr[i].getSize() > pivot) {
//                i++;
//            }
//            while (inputArr[j].getSize() < pivot) {
//                j--;
//            }
//            if (i <= j) {
//                MovingRectFloat2D temp = inputArr[i];
//                inputArr[i] = inputArr[j];
//                inputArr[j] = temp;
//                //move index to next position on both sides
//                i++;
//                j--;
//            }
//        }
//        // call quickSort() method recursively
//        if (lowerIndex < j)
//            quickSortDesc(inputArr, lowerIndex, j);
//        if (i < higherIndex)
//            quickSortDesc(inputArr, i, higherIndex);
//    }
}
