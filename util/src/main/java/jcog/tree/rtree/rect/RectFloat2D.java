package jcog.tree.rtree.rect;

import jcog.Util;
import jcog.tree.rtree.HyperRegion;
import jcog.tree.rtree.RTree;
import jcog.tree.rtree.point.Float2D;


public class RectFloat2D implements HyperRegion<Float2D>, Comparable<RectFloat2D> {

    public static final RectFloat2D Unit = new RectFloat2D(0, 0, 1, 1);
    public static final RectFloat2D Zero = new RectFloat2D(0, 0, 0, 0);

    public final Float2D min;
    public final Float2D max;

    public RectFloat2D(final Float2D p) {
        min = p;
        max = p;
    }

    public RectFloat2D(float x1, float y1, float x2, float y2) {
        if (x2 < x1) {
            float t = x2;
            x2 = x1;
            x1 = t;
        }
        if (y2 < y1) {
            float t = y2;
            y2 = y1;
            y1 = t;
        }

        min = new Float2D(x1, y1);
        max = new Float2D(x2, y2);
    }

    public RectFloat2D(final Float2D p1, final Float2D p2) {
        final float minX, maxX;

        if (p1.x < p2.x) {
            minX = p1.x;
            maxX = p2.x;
        } else {
            minX = p2.x;
            maxX = p2.x;
        }

        final float minY;
        final float maxY;
        if (p1.y < p2.y) {
            minY = p1.y;
            maxY = p2.y;
        } else {
            minY = p2.y;
            maxY = p2.y;
        }

        min = new Float2D(minX, minY);
        max = new Float2D(maxX, maxY);
    }


    public RectFloat2D move(float dx, float dy) {
        if (Math.abs(dx) < Float.MIN_NORMAL && Math.abs(dy) < Float.MIN_NORMAL)
            return this;
        else
            return new RectFloat2D(min.x+dx, min.y+dy, max.x+dx, max.y+dy);
    }

    @Override
    public RectFloat2D mbr(final HyperRegion<Float2D> r) {
        final RectFloat2D r2 = (RectFloat2D) r;
        final float minX = Util.min(min.x, r2.min.x);
        final float minY = Util.min(min.y, r2.min.y);
        final float maxX = Util.max(max.x, r2.max.x);
        final float maxY = Util.max(max.y, r2.max.y);

        return new RectFloat2D(minX, minY, maxX, maxY);

    }

    @Override
    public int dim() {
        return 2;
    }

    public Float2D center() {
        final float dx = (float) center(0);
        final float dy = (float) center(1);

        return new Float2D(dx, dy);
    }

    @Override
    public double center(int d) {
        if (d == 0) {
            return min.x + (max.x - min.x) / 2.0;
        } else {
            assert (d == 1);
            return min.y + (max.y - min.y) / 2.0;
        }
    }


    @Override
    public double coord(boolean maxOrMin, int dimension) {
        Float2D e = (maxOrMin ? max : min);
        assert(dimension==0 || dimension==1);
        return dimension==0 ? e.x : e.y;
    }


    @Override
    public double range(final int dim) {
        if (dim == 0) {
            return max.x - min.x;
        } else if (dim == 1) {
            return max.y - min.y;
        } else {
            throw new IllegalArgumentException("Invalid dimension");
        }
    }

    @Override
    public boolean contains(final HyperRegion r) {
        if (this == r) return true;
        final RectFloat2D r2 = (RectFloat2D) r;

        return min.x <= r2.min.x &&
                max.x >= r2.max.x &&
                min.y <= r2.min.y &&
                max.y >= r2.max.y;
    }

    @Override
    public boolean intersects(final HyperRegion r) {
        if (this == r) return true;
        final RectFloat2D r2 = (RectFloat2D) r;

        return !((min.x > r2.max.x) || (r2.min.x > max.x) ||
                (min.y > r2.max.y) || (r2.min.y > max.y));
    }

    @Override
    public double cost() {
        final float dx = max.x - min.x;
        final float dy = max.y - min.y;
        return Math.abs(dx * dy);
    }


    @Override
    public boolean equals(Object o) {
        return equals(o, (float) RTree.EPSILON);
    }

    public boolean equals(Object o, float epsilon) {
        if (this == o) return true;
        if (!(o instanceof RectFloat2D)) return false;

        RectFloat2D rect2D = (RectFloat2D) o;
        return equals(rect2D.min.x, rect2D.min.y, rect2D.max.x, rect2D.max.y, epsilon);
    }

    public boolean equals(float minX, float minY, float maxX, float maxY, float epsilon) {
        return Util.equals(min.x, minX, epsilon) &&
                Util.equals(max.x, maxX, epsilon) &&
                Util.equals(min.y, minY, epsilon) &&
                Util.equals(max.y, maxY, epsilon);
    }

    @Override
    public int hashCode() {
        int result = min.hashCode();
        result = 31 * result + max.hashCode();
        return result;
    }

    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append('(');
        sb.append(Float.toString(min.x));
        sb.append(',');
        sb.append(Float.toString(min.y));
        sb.append(')');
        sb.append(' ');
        sb.append('(');
        sb.append(Float.toString(max.x));
        sb.append(',');
        sb.append(Float.toString(max.y));
        sb.append(')');

        return sb.toString();
    }

    @Override
    public int compareTo(RectFloat2D o) {
        int a = min.compareTo(o.min);
        if (a != 0) return a;
        int b = max.compareTo(o.max);
        return b;
    }

    public float w() {
        return max.x - min.x;
    }

    public float h() {
        return max.y - min.y;
    }

    public float mag() {
        return Math.max( w(), h() );
    }

    public boolean contains(float x, float y) {
        return (x >= min.x && y >= min.y && x <= max.x && y <= max.y);
    }
}