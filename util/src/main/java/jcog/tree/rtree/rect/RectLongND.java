package jcog.tree.rtree.rect;


import jcog.tree.rtree.HyperRegion;
import jcog.tree.rtree.point.LongND;

import java.io.Serializable;
import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.IntStream;


/**
 * Created by jcovert on 6/15/15.
 */

public class RectLongND implements HyperRegion, Serializable, Comparable<RectLongND> {

    public static final HyperRegion ALL_1 = RectLongND.all(1);
    public static final HyperRegion ALL_2 = RectLongND.all(2);
    public static final HyperRegion ALL_3 = RectLongND.all(3);
    public static final HyperRegion ALL_4 = RectLongND.all(4);
    public static final LongND unbounded = new LongND() {
        @Override
        public String toString() {
            return "*";
        }
    };
    public final LongND min;
    public final LongND max;

    public RectLongND() {
        min = unbounded;
        max = unbounded;
    }

    public RectLongND(LongND p) {
        min = p;
        max = p;
    }

    public RectLongND(long[] a, long[] b) {
        this(new LongND(a), new LongND(b));
    }


    public RectLongND(LongND a, LongND b) {
        var dim = a.dim();

        var min = new long[dim];
        var max = new long[dim];

        var ad = a.coord;
        var bd = b.coord;
        for (var i = 0; i < dim; i++) {
            var ai = ad[i];
            var bi = bd[i];
            min[i] = Math.min(ai, bi);
            max[i] = Math.max(ai, bi);
        }
        this.min = new LongND(min);
        this.max = new LongND(max);
    }

    public static HyperRegion all(int i) {
        return new RectLongND(LongND.fill(i, Long.MIN_VALUE), LongND.fill(i, Long.MAX_VALUE));
    }

    @Override
    public boolean contains(HyperRegion _inner) {
        var inner = (RectLongND) _inner;

        var dim = dim();
        return IntStream.range(0, dim).noneMatch(i -> min.coord[i] > inner.min.coord[i] || max.coord[i] < inner.max.coord[i]);
    }

    @Override
    public boolean intersects(HyperRegion r) {
        var x = (RectLongND) r;

        var dim = dim();
        /*return !((min.x > r2.max.x) || (r2.min.x > max.x) ||
                    (min.y > r2.max.y) || (r2.min.y > max.y));*/
        return IntStream.range(0, dim).noneMatch(i -> min.coord[i] > x.max.coord[i] || x.min.coord[i] > max.coord[i]);
    }

    @Override
    public double cost() {
        var sigma = 1f;
        var dim = dim();
        for (var i = 0; i < dim; i++) {
            sigma *= rangeIfFinite(i, 1 /* an infinite dimension can not be compared, so just ignore it */);
        }
        return sigma;
    }

    @Override
    public HyperRegion mbr(HyperRegion r) {
        var x = (RectLongND) r;

        var dim = dim();
        var newMin = new long[dim];
        var newMax = new long[dim];
        for (var i = 0; i < dim; i++) {
            newMin[i] = Math.min(min.coord[i], x.min.coord[i]);
            newMax[i] = Math.max(max.coord[i], x.max.coord[i]);
        }
        return new RectLongND(newMin, newMax);
    }


    @Override
    public double center(int dim) {
        return centerF(dim);
    }

    public double centerF(int dim) {
        var min = this.min.coord[dim];
        var max = this.max.coord[dim];
        if ((min == Long.MIN_VALUE) && (max == Long.MAX_VALUE))
            return 0;
        if (min == Long.MIN_VALUE)
            return max;
        if (max == Long.MAX_VALUE)
            return min;

        return (max + min) / 2.0f;
    }

    public LongND center() {
        var dim = dim();
        var c = new long[10];
        var count = 0;
        for (var i = 0; i < dim; i++) {
            var l = (min.coord(i) + max.coord(i)) / 2;
            if (c.length == count) c = Arrays.copyOf(c, count * 2);
            c[count++] = l;
        }
        c = Arrays.copyOfRange(c, 0, count);
        return new LongND(c);
    }


    @Override
    public int dim() {
        return min.dim();
    }

    @Override
    public double coord(int dimension, boolean maxOrMin) {
        return maxOrMin ? max.coord[dimension] : min.coord[dimension];
    }

    public double distance(HyperRegion X, int dim, boolean maxOrMin, boolean XmaxOrMin) {
        return max.coord[dim] - min.coord[dim];
    }

    @Override
    public double range(int dim) {
        float min = this.min.coord[dim];
        float max = this.max.coord[dim];
        if (min == max)
            return 0;
        if ((min == Long.MIN_VALUE) || (max == Long.MAX_VALUE))
            return Long.MAX_VALUE;
        return (max - min);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null /*|| getClass() != o.getClass()*/) return false;

        var r = (RectLongND) o;
        return min.equals(r.min) && max.equals(r.max);
    }

    @Override
    public int compareTo(RectLongND o) {
        var a = min.compareTo(o.min);
        if (a != 0) return a;
        return max.compareTo(o.max);
    }

    @Override
    public int hashCode() {
        var result = min.hashCode();
        result = 31 * result + max.hashCode();
        return result;
    }

    public String toString() {
        if (min.equals(max)) {
            return min.toString();
        } else {
            return "(" +
                    min +
                    ',' +
                    max +
                    ')';
        }
    }


    public static final class Builder<X extends RectLongND> implements Function<X, HyperRegion> {

        @Override
        public X apply(X rect2D) {
            return rect2D;
        }

    }


}