package jcog.tree.rtree.rect;

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
 *      http:
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */


import jcog.tree.rtree.HyperRegion;
import jcog.tree.rtree.point.DoubleND;

import java.io.Serializable;
import java.util.function.Function;

import static java.lang.Double.NEGATIVE_INFINITY;
import static java.lang.Double.POSITIVE_INFINITY;

/**
 * Created by jcovert on 6/15/15.
 */

public class RectDoubleND implements HyperRegion<DoubleND>, Serializable {

    public static final HyperRegion ALL_1 = RectDoubleND.all(1);
    public static final HyperRegion ALL_2 = RectDoubleND.all(2);
    public static final HyperRegion ALL_3 = RectDoubleND.all(3);
    public static final HyperRegion ALL_4 = RectDoubleND.all(4);
    public static final DoubleND unbounded = new DoubleND() {
        @Override
        public String toString() {
            return "*";
        }
    };
    public final DoubleND min;
    public final DoubleND max;

    public RectDoubleND() {
        min = unbounded;
        max = unbounded;
    }

    public RectDoubleND(final DoubleND p) {
        min = p;
        max = p;
    }

    public RectDoubleND(double[] a, double[] b) {
        this(new DoubleND(a), new DoubleND(b));
    }

    @Override
    public double coord(boolean maxOrMin, int dimension) {
        return (maxOrMin ? max : min).coord[dimension];
    }

    public RectDoubleND(final DoubleND a, final DoubleND b) {
        int dim = a.dim();

        double[] min = new double[dim];
        double[] max = new double[dim];

        double[] ad = a.coord;
        double[] bd = b.coord;
        for (int i = 0; i < dim; i++) {
            double ai = ad[i];
            double bi = bd[i];
            min[i] = Math.min(ai, bi);
            max[i] = Math.max(ai, bi);
        }
        this.min = new DoubleND(min);
        this.max = new DoubleND(max);
    }

    public static HyperRegion all(int i) {
        return new RectDoubleND(DoubleND.fill(i, NEGATIVE_INFINITY), DoubleND.fill(i, POSITIVE_INFINITY));
    }





























    @Override
    public double cost() {
        double sigma = 1f;
        int dim = dim();
        for (int i = 0; i < dim; i++) {
            sigma *= rangeIfFinite(i, 1 /* an infinite dimension can not be compared, so just ignore it */);
        }
        return sigma;
    }

    @Override
    public HyperRegion mbr(final HyperRegion r) {
        final RectDoubleND x = (RectDoubleND) r;

        int dim = dim();
        double[] newMin = new double[dim];
        double[] newMax = new double[dim];
        for (int i = 0; i < dim; i++) {
            newMin[i] = Math.min(min.coord[i], x.min.coord[i]);
            newMax[i] = Math.max(max.coord[i], x.max.coord[i]);
        }
        return new RectDoubleND(newMin, newMax);
    }


    @Override
    public double center(int dim) {
        return centerF(dim);
    }

    public double centerF(int dim) {
        double min = this.min.coord[dim];
        double max = this.max.coord[dim];
        if ((min == NEGATIVE_INFINITY) && (max == Double.POSITIVE_INFINITY))
            return 0;
        if (min == NEGATIVE_INFINITY)
            return max;
        if (max == Double.POSITIVE_INFINITY)
            return min;

        return (max + min) / 2;
    }

    public DoubleND center() {
        int dim = dim();
        double[] c = new double[dim];
        for (int i = 0; i < dim; i++) {
            c[i] = centerF(i);
        }
        return new DoubleND(c);
    }


    @Override
    public int dim() {
        return min.dim();
    }

    @Override
    public double range(final int dim) {
        double min = this.min.coord[dim];
        double max = this.max.coord[dim];
        if (min == max)
            return 0;
        if ((min == NEGATIVE_INFINITY) || (max == Double.POSITIVE_INFINITY))
            return Double.POSITIVE_INFINITY;
        return (max - min);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null /*|| getClass() != o.getClass()*/) return false;

        RectDoubleND r = (RectDoubleND) o;
        return min.equals(r.min) && max.equals(r.max);
    }

    @Override
    public int hashCode() {
        int result = min.hashCode();
        result = 31 * result + max.hashCode();
        return result;
    }

    public String toString() {
        if (min.equals(max)) {
            return min.toString();
        } else {
            return new StringBuilder().append('(').append(min).append(',').append(max).append(')').toString();
        }
    }


    public final static class Builder<X extends RectDoubleND> implements Function<X, HyperRegion> {

        @Override
        public X apply(final X rect2D) {
            return rect2D;
        }

    }


}