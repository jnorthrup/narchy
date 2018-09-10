package jcog.tree.rtree.rect;

import jcog.TODO;
import jcog.Util;
import jcog.tree.rtree.HyperRegion;
import jcog.tree.rtree.Spatialization;
import jcog.tree.rtree.point.Float2D;


public class RectFloat2D implements HyperRegion<Float2D>, Comparable<RectFloat2D> {

    public static final RectFloat2D Unit = XYXY(0, 0, 1, 1);
    public static final RectFloat2D Zero = XYXY(0, 0, 0, 0);

    public final float x, y, w, h;


    protected RectFloat2D(RectFloat2D r) {
        this.x = r.x;
        this.y = r.y;
        this.w = r.w;
        this.h = r.h;

//        this.x = r.x;
//        this.y = r.y;
//        this.w = r.w;
//        this.h = r.h;

    }

    private RectFloat2D(float x1, float y1, float x2, float y2) {
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

    /**
     * specified as a pair of X,Y coordinate pairs defining the diagonal extent
     */
    public static RectFloat2D XYXY(float x1, float y1, float x2, float y2) {
        return new RectFloat2D(x1, y1, x2, y2);
    }

    /**
     * specified as a center point (cx,cy) and width,height extent (w,h)
     */
    public static RectFloat2D XYWH(float cx, float cy, float w, float h) {
        return new RectFloat2D(cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2);
    }

    /** x,y corresponds to "lower left" corner rather than XYWH's center */
    public static RectFloat2D X0Y0WH(float x0, float y0, float w, float h) {
        return XYXY(x0, y0, x0+w, y0+h);
    }

    public static RectFloat2D XYWH(double cx, double cy, double w, double h) {
        return XYWH((float)cx, ((float)cy), (float)w, (float)h);
    }

    /**
     * interpolates the coordinates, and the scale is proportional to the mean dimensions of each
     */
    public static RectFloat2D mid(RectFloat2D source, RectFloat2D target, float relScale) {
        float cx = (source.cx() + target.cx()) / 2f;
        float cy = (source.cy() + target.cy()) / 2f;
        float wh = relScale * Math.max((source.w + target.w) / 2f, (source.h + target.h) / 2f);
        return RectFloat2D.XYWH(cx, cy, wh, wh);
    }



    public RectFloat2D move(double dx, double dy) {
        return move((float) dx, (float) dy);
    }

    public RectFloat2D move(float dx, float dy) {
        return move(dx, dy, Spatialization.EPSILONf);
    }

    public RectFloat2D move(float dx, float dy, float epsilon) {
        return Math.abs(dx) < epsilon && Math.abs(dy) < epsilon ? this :
                XYXY(x + dx, y + dy, x + w + dx, y + h + dy);
    }
    public RectFloat2D pos(float x, float y, float epsilon) {
        return Util.equals(this.x, x, epsilon) && Util.equals(this.y, y, epsilon) ? this :
                XYXY(x , y , x + w, y + h);
    }

    public RectFloat2D size(float ww, float hh) {
        return size(ww, hh, Spatialization.EPSILONf);
    }

    public RectFloat2D size(float ww, float hh, float epsilon) {
        float w = this.w;
        float h = this.h;
        return Util.equals(w, ww, epsilon) && Util.equals(h, hh, epsilon) ? this : XYWH(cx(), cy(), ww, hh);
    }

    @Override
    public RectFloat2D mbr(final HyperRegion<Float2D> r) {
        if (r == this) return this;

        final RectFloat2D r2 = (RectFloat2D) r;
        final float minX = Math.min(x, r2.x);
        final float minY = Math.min(y, r2.y);
        final float maxX = Math.max(x + w, r2.x + r2.w);
        final float maxY = Math.max(y + h, r2.y + r2.h);

        return XYXY(minX, minY, maxX, maxY);
    }

    @Override
    public final int dim() {
        return 2;
    }


    @Override
    public double center(int d) {
        if (d == 0) {
            return cx();
        } else {
            assert (d == 1);
            return cy();
        }
    }


    @Override
    public double coord(boolean maxOrMin, int dimension) {
        switch (dimension) {
            case 0:
                return maxOrMin ? (x + w) : x;
            case 1:
                return maxOrMin ? (y + h) : h;
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
                x + w >= r2.x + w &&
                y <= r2.y &&
                y + h >= r2.y + h;
    }

    @Override
    public boolean intersects(final HyperRegion r) {
        if (this == r) return true;
        final RectFloat2D r2 = (RectFloat2D) r;

        return !((x > r2.x + w) || (r2.x > x + w) ||
                (y > r2.y + h) || (r2.y > y + h));
    }

    @Override
    public double cost() {
        final float dx = w;
        final float dy = h;
        return Math.abs(dx * dy);
    }


    @Override
    public boolean equals(Object o) {
        return equals(o, (float) Spatialization.EPSILON);
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






    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append('(');
        sb.append(Float.toString(x));
        sb.append(',');
        sb.append(Float.toString(y));
        sb.append(')');
        sb.append(' ');
        sb.append('(');
        sb.append(Float.toString(x + w));
        sb.append(',');
        sb.append(Float.toString(y + h));
        sb.append(')');

        return sb.toString();
    }

    @Override
    public int compareTo(RectFloat2D o) {
        throw new TODO();




    }

    public float mag() {
        return Math.max(w, h);
    }

    public final boolean contains(float px, float py) {
        return (px >= x && px <= x + w && py >= y && py <= y + h);
    }

    public final float top() {
        return y;
    }

    public final float left() {
        return x;
    }

    public final float right() {
        return x + w;
    }

    public final float bottom() {
        return y + h;
    }

    public final float cx() {
        return x + w / 2;
    }

    public final float cy() {
        return y + h / 2;
    }

    public RectFloat2D scale(float s) {
        if (s == 1)
            return this;
        else
            return RectFloat2D.XYWH(cx(), cy(), w * s, h * s);
    }

    public float radius() {
        float W = w / 2;
        float H = h / 2;
        return ((float) Math.sqrt(W * W + H * H));
    }

    public final float area() {
        return w * h;
    }

}