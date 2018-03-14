package jcog.tree.rtree;

        /*
         * #%L
         * Conversant RTree
         * ~~
         * Conversantmedia.com © 2016, Conversant, Inc. Conversant® is a trademark of Conversant, Inc.
         * ~~
         * Licensed under the Apache License, Version 2.0 (the "License");
         * you may not use this file except in compliance with the License.
         * You may obtain a copy of the License at
         *
         *      http://www.apache.org/licenses/LICENSE-2.0
         *
         * Unless required by applicable law or agreed to in writing, software
         * distributed under the License is distributed on an "AS IS" BASIS,
         * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
         * See the License for the specific language governing permissions and
         * limitations under the License.
         * #L%
         */


import java.util.function.Function;

/**
 * An N dimensional rectangle or "hypercube" that is a representation of a data entry.
 * <p>
 * Created by jcairns on 4/30/15.
 */
public interface HyperRegion<X> {

    /**
     * Calculate the resulting mbr when combining param HyperRect with this HyperRect
     * use custom implementations of mbr(HyperRect[]) when possible, it is potentially more efficient
     *
     * @param r - mbr to add
     * @return new HyperRect representing mbr of both HyperRects combined
     */
    HyperRegion<X> mbr(HyperRegion<X> r);


    static <X> HyperRegion mbr(Function<X, HyperRegion> builder, X[] rect, short size) {
        assert (size > 0);
        HyperRegion bounds = builder.apply(rect[0]);
        for (int k = 1; k < size; k++) {
            X rr = rect[k];
            HyperRegion r = builder.apply(rr);
            bounds = bounds.mbr(r);
        }
        return bounds;
    }

    /**
     * warning, the array may contain nulls. in which case, break because these will be at the end
     */
    default HyperRegion<X> mbr(HyperRegion<X>[] rect) {
        HyperRegion<X> bounds = rect[0];
        for (int k = 1; k < rect.length; k++) {
            HyperRegion<X> r = rect[k];
            bounds = bounds.mbr(r);
        }
        return bounds;
    }


    /**
     * Get number of dimensions used in creating the HyperRect
     *
     * @return number of dimensions
     */
    int dim();

//    /**
//     * Get the minimum HyperPoint of this HyperRect
//     *
//     * @return min HyperPoint
//     */
//    X min();
//
//    /**
//     * Get the minimum HyperPoint of this HyperRect
//     *
//     * @return min HyperPoint
//     */
//    X max();

    /**
     * returns coordinate scalar at the given extremum and dimension
     *
     * @param maxOrMin  true = max, false = min
     * @param dimension which dimension index
     */
    double coord(boolean maxOrMin, int dimension);

    default float coordF(boolean maxOrMin, int dimension) {
        return (float)coord(maxOrMin, dimension);
    }


    default double center(int d) {
        return (coord(true, d) + coord(false, d)) / 2.0;
    }

    /**
     * Calculate the distance between the min and max HyperPoints in given dimension
     *
     * @param dim - dimension to calculate
     * @return double - the numeric range of the dimention (min - max)
     */
    default double range(final int dim) {
        return Math.abs(coord(true, dim) - coord(false, dim));
    }

    default double rangeIfFinite(int dim, double elseValue) {
        double r = range(dim);
        if (!Double.isFinite(r)) {
            return elseValue;
        } else {
            assert (r >= 0);
            return r;
        }
    }


    /**
     * Determines if this HyperRect fully contains parameter HyperRect
     *
     * @param r - HyperRect to test
     * @return true if contains, false otherwise; a region contains itself
     */
    default boolean contains(HyperRegion<X> x) {
        if (this == x) return true;
        int d = dim();
        for (int i = 0; i < d; i++)
            if (coord(false, i) > x.coord(false, i) ||
                    coord(true, i) < x.coord(true, i))
                return false;
        return true;
    }


    /**
     * Determines if this HyperRect intersects parameter HyperRect
     *
     * @param r - HyperRect to test
     * @return true if intersects, false otherwise
     */
    default boolean intersects(HyperRegion<X> x) {
        if (this == x) return true;
        int d = dim();
//            if (min.coord[i] > x.max.coord[i] || x.min.coord[i] > max.coord[i])
        for (int i = 0; i < d; i++)
            if (coord(false, i) > x.coord(true, i) ||
                    coord(true, i) < x.coord(false, i))
                return false;
        return true;
    }


    /**
     * Calculate the "cost" of this HyperRect -
     * generally this is computed as the area/volume/hypervolume of this region
     *
     * @return - cost
     */
    default double cost() {
        int n = dim();
        double a = 1.0;
        for (int d = 0; d < n; d++) {
            a *= rangeIfFinite(d, 0);
        }
        return a;
    }

    /**
     * Calculate the perimeter of this HyperRect - across all dimesnions
     *
     * @return - perimeter
     */
    default double perimeter() {
        double p = 0.0;
        final int n = this.dim();
        for (int d = 0; d < n; d++) {
            p += /*2.0 * */this.rangeIfFinite(d, 0);
        }
        return p;
    }

//    static <T> HyperRegion[] toArray(T[] data, int size, Function<T, HyperRegion> builder) {
//        return Util.map(builder::apply, new HyperRegion[size], data);
//    }

    /**
     * gets the distance along a certain dimension from this region's to another's extrema
     */
    default double distance(HyperRegion X, int dim, boolean maxOrMin, boolean XmaxOrMin) {
        if(this == X)
            return 0;
        return Math.abs(
                coord(maxOrMin, dim) - X.coord(XmaxOrMin, dim)
        );
    }


//    @JsonIgnore  default double getRangeMin() {
//        int dim = dim();
//        double min = Double.POSITIVE_INFINITY;
//        for (int i = 0; i < dim; i++) {
//            double r = getRange(i);
//            if (r < min) {
//                min = r;
//            }
//        }
//        return min;
//    }

//    /** whether any of the dimensions are finite */
//    @JsonIgnore  default boolean bounded() {
//        int dim = dim();
//        for (int i = 0; i < dim; i++) {
//            double r = getRange(i);
//            if (Double.isFinite(r))
//                return true;
//        }
//        return false;
//    }

}
