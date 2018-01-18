package jcog.tree.rtree.rect;

import jcog.TODO;
import jcog.Util;
import jcog.tree.rtree.HyperRegion;
import jcog.tree.rtree.RTree;
import jcog.tree.rtree.point.Float2D;


public class RectFloat2D implements HyperRegion<Float2D>, Comparable<RectFloat2D> {

    public static final RectFloat2D Unit = new RectFloat2D(0, 0, 1, 1);
    public static final RectFloat2D Zero = new RectFloat2D(0, 0, 0, 0);

    public final float x, y, w, h;

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

        x = x1;
        w = (x2 - x1);
        y = y1;
        h = (y2 - y1);
    }


    public RectFloat2D move(float dx, float dy, float epsilon) {
        return Math.abs(dx) < epsilon && Math.abs(dy) < epsilon ?
                this :
                new RectFloat2D(x + dx, y + dy, x + w + dx, y + h + dy);
    }

    @Override
    public RectFloat2D mbr(final HyperRegion<Float2D> r) {
        if (r == this) return this;

        final RectFloat2D r2 = (RectFloat2D) r;
        final float minX = Util.min(x, r2.x);
        final float minY = Util.min(y, r2.y);
        final float maxX = Util.max(x+w, r2.x+r2.w);
        final float maxY = Util.max(y+h, r2.y+r2.h);

        return new RectFloat2D(minX, minY, maxX, maxY);
    }

    @Override
    public final int dim() {
        return 2;
    }


    @Override
    public double center(int d) {
        if (d == 0) {
            return x + w / 2.0;
        } else {
            assert (d == 1);
            return y + h / 2.0;
        }
    }


    @Override
    public double coord(boolean maxOrMin, int dimension) {
        switch (dimension) {
            case 0:
                return maxOrMin ? (x+w) : x;
            case 1:
                return maxOrMin ? (y+h) : h;
            default:
                throw new UnsupportedOperationException();
        }
    }


    @Override
    public double range(final int dim) {
        if (dim == 0) {
            return w;
        } else if (dim == 1) {
            return h;
        } else {
            throw new IllegalArgumentException("Invalid dimension");
        }
    }

    @Override
    public boolean contains(final HyperRegion r) {
        if (this == r) return true;
        final RectFloat2D r2 = (RectFloat2D) r;

        return x <= r2.x &&
                x+w >= r2.x+w &&
                y <= r2.y &&
                y+h >= r2.y+h;
    }

    @Override
    public boolean intersects(final HyperRegion r) {
        if (this == r) return true;
        final RectFloat2D r2 = (RectFloat2D) r;

        return !((x > r2.x+w) || (r2.x > x+w) ||
                (y > r2.y+h) || (r2.y > y+h));
    }

    @Override
    public double cost() {
        final float dx = w;
        final float dy = h;
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
        return equals(rect2D.x, rect2D.y, rect2D.w, rect2D.h, epsilon);
    }

    public boolean equals(float xx, float yy, float ww, float hh, float epsilon) {
        return Util.equals(x, xx, epsilon) &&
                Util.equals(y, yy, epsilon) &&
                Util.equals(w, ww, epsilon) &&
                Util.equals(h, hh, epsilon);
    }

    @Override
    public int hashCode() {
        throw new TODO();
    }

    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append('(');
        sb.append(Float.toString(x));
        sb.append(',');
        sb.append(Float.toString(y));
        sb.append(')');
        sb.append(' ');
        sb.append('(');
        sb.append(Float.toString(x+w));
        sb.append(',');
        sb.append(Float.toString(y+h));
        sb.append(')');

        return sb.toString();
    }

    @Override
    public int compareTo(RectFloat2D o) {
        throw new TODO();
//        int a = min.compareTo(o.min);
//        if (a != 0) return a;
//        int b = max.compareTo(o.max);
//        return b;
    }

    public float mag() {
        return Math.max( w, h );
    }

    public boolean contains(float px, float py) {
        return (px >= x && px <= x+w && py >= y && py <= y+h);
    }

    public float top() {
        return y;
    }

    public float left() {
        return x;
    }
    public float right() {
        return x + w;
    }
    public float bottom() {
        return y + h;
    }
}