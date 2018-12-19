package jcog.tree.rtree.rect;

import jcog.TODO;
import jcog.Util;
import jcog.math.v2;
import jcog.tree.rtree.HyperRegion;
import jcog.tree.rtree.Spatialization;
import jcog.tree.rtree.point.Float2D;

import static jcog.Util.lerp;
import static jcog.tree.rtree.Spatialization.EPSILON;


public class RectFloat implements HyperRegion<Float2D>, Comparable<RectFloat> {

    public static final RectFloat Unit = XYXY(0, 0, 1, 1);
    public static final RectFloat Zero = XYXY(0, 0, 0, 0);

    public final float x, y, w, h;


    protected RectFloat(RectFloat r) {
        this.x = r.x;
        this.y = r.y;
        this.w = r.w;
        this.h = r.h;

//        this.x = r.x;
//        this.y = r.y;
//        this.w = r.w;
//        this.h = r.h;

    }

    private RectFloat(float x1, float y1, float x2, float y2) {
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
    public static RectFloat XYXY(float x1, float y1, float x2, float y2) {
        return new RectFloat(x1, y1, x2, y2);
    }

    /**
     * specified as a center point (cx,cy) and width,height extent (w,h)
     */
    public static RectFloat XYWH(float cx, float cy, float w, float h) {
        return new RectFloat(cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2);
    }

    /** x,y corresponds to "lower left" corner rather than XYWH's center */
    public static RectFloat X0Y0WH(float x0, float y0, float w, float h) {
        return XYXY(x0, y0, x0+w, y0+h);
    }

    public static RectFloat XYWH(double cx, double cy, double w, double h) {
        return XYWH((float)cx, ((float)cy), (float)w, (float)h);
    }

    /**
     * interpolates the coordinates, and the scale is proportional to the mean dimensions of each
     */
    public static RectFloat mid(RectFloat source, RectFloat target, float relScale) {
        float cx = (source.cx() + target.cx()) / 2f;
        float cy = (source.cy() + target.cy()) / 2f;
        float wh = relScale * Math.max((source.w + target.w) / 2f, (source.h + target.h) / 2f);
        return RectFloat.XYWH(cx, cy, wh, wh);
    }

    public static RectFloat WH(float w, float h) {
        return X0Y0WH(0, 0, w, h);
    }

    public static RectFloat XYXY(v2 ul, v2 br) {
        return RectFloat.XYXY(ul.x, ul.y, br.x, br.y);
    }

    public RectFloat move(double dx, double dy) {
        return move((float) dx, (float) dy);
    }

    public RectFloat move(float dx, float dy) {
        return move(dx, dy, Spatialization.EPSILONf);
    }

    public RectFloat move(float dx, float dy, float epsilon) {
        return Math.abs(dx) < epsilon && Math.abs(dy) < epsilon ? this :
                XYXY(x + dx, y + dy, x + w + dx, y + h + dy);
    }
    public RectFloat pos(float x, float y, float epsilon) {
        return Util.equals(this.x, x, epsilon) && Util.equals(this.y, y, epsilon) ? this :
                XYXY(x , y , x + w, y + h);
    }

    public RectFloat size(float ww, float hh) {
        return size(ww, hh, Spatialization.EPSILONf);
    }

    public RectFloat size(float ww, float hh, float epsilon) {
        float w = this.w;
        float h = this.h;
        return Util.equals(w, ww, epsilon) && Util.equals(h, hh, epsilon) ? this : XYWH(cx(), cy(), ww, hh);
    }

    @Override
    public RectFloat mbr(final HyperRegion<Float2D> r) {
        if (r == this) return this;

        final RectFloat r2 = (RectFloat) r;
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
    public double coord(int dimension, boolean maxOrMin) {
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
        final RectFloat r2 = (RectFloat) r;

        return x <= r2.x &&
                x + w >= r2.x + r2.w &&
                y <= r2.y &&
                y + h >= r2.y + r2.h;
    }

    @Override
    public boolean intersects(final HyperRegion r) {
        if (this == r) return true;
        final RectFloat r2 = (RectFloat) r;

        return !((x > r2.x + r2.w) || (r2.x > x + w) ||
                (y > r2.y + r2.h) || (r2.y > y + h));
    }

    @Override
    public double cost() {
        final float dx = w;
        final float dy = h;
        return Math.abs(dx * dy);
    }


    @Override
    public int hashCode() {
        return Util.hashCombine(Util.hashCombine(Float.hashCode(x), Float.hashCode(y)), Util.hashCombine(Float.hashCode(w), Float.hashCode(h)));
    }

    @Override
    public boolean equals(Object o) {
        return equals(o, (float) EPSILON);
    }

    public boolean equals(Object o, float epsilon) {
        if (this == o) return true;
        if (!(o instanceof RectFloat)) return false;

        RectFloat rect2D = (RectFloat) o;
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
    public int compareTo(RectFloat o) {
        throw new TODO();
    }

    public final float mag() {
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

    public RectFloat transform(float s, float ox, float oy) {
        ////        if (Util.equals(scale, 1f, ScalarValue.EPSILON) && offset.equalsZero())
            return RectFloat.XYWH(cx()+ox, cy()+oy, w * s, h * s);
    }

    public RectFloat scale(float s) {
        if (Util.equals(s, 0, EPSILON))
            return this;
        else
            return RectFloat.XYWH(cx(), cy(), w * s, h * s);
    }

    public float radius() {
        float W = w / 2;
        float H = h / 2;
        return ((float) Math.sqrt(W * W + H * H));
    }

    public final float area() {
        return w * h;
    }

    public RectFloat posLerp(float x, float y, float p) {
        return RectFloat.XYWH(lerp(p, cx(), x),lerp(p, cy(), y) ,w , h);
    }

    public boolean nonZero(float epsilon) {
        return w > epsilon && h > epsilon;
    }



}