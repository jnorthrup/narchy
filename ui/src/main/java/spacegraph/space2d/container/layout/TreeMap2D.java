package spacegraph.space2d.container.layout;

import jcog.pri.ScalarValue;
import jcog.tree.rtree.rect.RectFloat;
import spacegraph.space2d.container.graph.Graph2D;
import spacegraph.util.MutableFloatRect;

/**
 * Implements the Squarified Treemap layout published by
 * Mark Bruls, Kees Huizing, and Jarke J. van Wijk
 * <p>
 * Squarified Treemaps
 * https://www.win.tue.nl/~vanwijk/stm.pdf
 *
 * adapted from:https://github.com/peterdmv/treemap/
 */
public class TreeMap2D<X> extends DynamicLayout2D<X, MutableFloatRect<X>> {



    @Override
    protected void layout(Graph2D<X> g) {
        RectFloat b = g.bounds;
        nodes.sortThisByFloat((a)-> {
            //a.w = a.h = 1;
            a.w = b.w;
            a.h = b.h;
            return a.node.pri;
        });
        int end = nodes.size() - 1;
        total = areaSum(0, end);
        layout(0, end, b);
    }


    int mid;
    float total;
    public void layout(int start, int end, RectFloat bounds) {
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
                RectFloat newBounds = layoutRow(start, mid, bounds);
                layout(mid + 1, end, newBounds);
            }
        }
    }

    float highestAspect(int start, int end, RectFloat bounds) {

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

    RectFloat layoutRow(int start, int end, RectFloat bounds) {
        boolean isHorizontal = bounds.w > bounds.h;

        float rowSize = areaSum(start, end);
        float rowRatio = rowSize / total;
        assert(rowRatio==rowRatio): start + ".." + end + ": " + rowSize + " " + total;
        float offset = 0;

        for (int i = start; i <= end; i++) {
            MutableFloatRect x = this.nodes.get(i);

            float p = x.node.pri;
            if (p != p || p < ScalarValue.EPSILON) {
                x.size(0.1f,0.1f);
                continue;
            }

            float ratio = p / rowSize;


            assert(ratio==ratio): p + " " + rowSize;

            RectFloat r;
            if (isHorizontal) {
                r = RectFloat.X0Y0WH(bounds.x,
                        bounds.y + bounds.h * offset,
                        bounds.w * rowRatio,
                        bounds.h * ratio);
            } else {
                r = RectFloat.X0Y0WH(
                        bounds.x + bounds.w * offset,
                        bounds.y,
                        bounds.w * ratio,
                        bounds.h * rowRatio);
            }
            x.set(r);
            offset += ratio;
        }
        if (isHorizontal) {
            return RectFloat.X0Y0WH(bounds.x + bounds.w * rowRatio, bounds.y, bounds.w - bounds.w * rowRatio, bounds.h);
        } else {
            return RectFloat.X0Y0WH(bounds.x, bounds.y + bounds.h * rowRatio, bounds.w, bounds.h - bounds.h * rowRatio);
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
