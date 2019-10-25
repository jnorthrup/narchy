package jcog.tree.rtree.rect;

import jcog.TODO;
import jcog.Util;
import jcog.math.v2;
import jcog.tree.rtree.HyperRegion;

import static jcog.Texts.*;
import static jcog.Util.lerpSafe;
import static jcog.tree.rtree.Spatialization.EPSILON;
import static jcog.tree.rtree.Spatialization.EPSILONf;


public class RectFloat implements HyperRegion, Comparable<RectFloat> {

    public static final RectFloat Unit = XYXY((float) 0, (float) 0, 1.0F, 1.0F);
    public static final RectFloat Zero = XYXY((float) 0, (float) 0, (float) 0, (float) 0);

    public final float x;
    public final float y;
    public final float w;
    public final float h;


    protected RectFloat(RectFloat r) {
        this(r.x, r.y, r.w, r.h);
    }

    private RectFloat(float left, float bottom, float w, float h) {
        assert(w >= (float) 0 && h >= (float) 0);
        this.x = left; this.y = bottom; this.w = w; this.h = h;
    }


    /**
     * specified as a pair of X,Y coordinate pairs defining the diagonal extent
     */
    public static RectFloat XYXY(float x1 /* left */, float y1 /* bottom */, float x2, float y2) {
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

        return RectFloat._X0Y0WH(x1, y1, (x2 - x1), (y2 - y1));
    }

    /**
     * specified as a center point (cx,cy) and width,height extent (w,h)
     */
    public static RectFloat XYWH(float cx, float cy, float w, float h) {
        w = Math.abs(w); h = Math.abs(h);
        return X0Y0WH(cx - w / 2.0F, cy - h / 2.0F, w, h);
    }

    public static RectFloat XYWH(v2 center, float w, float h) {
        return XYWH(center.x, center.y, w, h);
    }

    /** x,y corresponds to "lower left" corner rather than XYWH's center */
    public static RectFloat X0Y0WH(float x0, float y0, float w, float h) {
        w = Math.abs(w); h = Math.abs(h);
        return _X0Y0WH(x0, y0, w, h);
    }

    
    private static RectFloat _X0Y0WH(float x0, float y0, float w, float h) {
        return new RectFloat(x0, y0, w, h);
    }


    public static RectFloat XYWH(double cx, double cy, double w, double h) {
        return XYWH((float)cx, ((float)cy), (float)w, (float)h);
    }
    /**
     * interpolates the coordinates, and the scale is proportional to the mean dimensions of each
     */
    public static RectFloat mid(RectFloat source, RectFloat target, float relScale) {
        float cx = (source.cx() + target.cx()) / 2.0F;
        float cy = (source.cy() + target.cy()) / 2.0F;
        float wh = relScale * Math.max((source.w + target.w) / 2f, (source.h + target.h) / 2.0F);
        return target.orThisIfEqual(source.orThisIfEqual(RectFloat.XYWH(cx, cy, wh, wh)));
    }

    public static RectFloat WH(float w, float h) {
        return X0Y0WH((float) 0, (float) 0, w, h);
    }

    public static RectFloat XYXY(v2 ul, v2 br) {
        return RectFloat.XYXY(ul.x, ul.y, br.x, br.y);
    }

    public RectFloat move(double dx, double dy) {
        return move((float) dx, (float) dy);
    }

    public RectFloat move(float dx, float dy) {
        return move(dx, dy, EPSILONf);
    }

    public RectFloat move(float dx, float dy, float epsilon) {
        return Math.abs(dx) < epsilon && Math.abs(dy) < epsilon ? this :
                X0Y0WH(x + dx, y + dy, w, h);
    }
    public RectFloat pos(float x, float y, float epsilon) {
        return Util.equals(this.x, x, epsilon) && Util.equals(this.y, y, epsilon) ? this :
                X0Y0WH(x , y, w, h);
    }


    public RectFloat size(float ww, float hh) {
        return orThisIfEqual(X0Y0WH(x, y, ww, hh));
    }

    @Override
    public RectFloat mbr(HyperRegion _b) {
        if (_b == this) return this;

        RectFloat b = (RectFloat) _b;

        //TODO better merge of these conditions
//        if (contains(b))
//            return this;
//        else if (b.contains(this))
//            return this;
//        else if (b.equals(this))
//            return this;

        float ax = this.x, bx = b.x,
                minX = Math.min(ax, bx),
                maxX = Math.max(ax + w, bx + b.w);
        float ay = this.y, by = b.y,
                minY = Math.min(ay, by),
                maxY = Math.max(ay + h, by + b.h);

        return orThisIfEqual(XYXY(minX, minY, maxX, maxY));
    }

    @Override
    public final int dim() {
        return 2;
    }


    @Override
    public double center(int d) {
        if (d == 0) {
            return (double) cx();
        } else {
            assert (d == 1);
            return (double) cy();
        }
    }


    @Override
    public double coord(int dimension, boolean maxOrMin) {
        switch (dimension) {
            case 0:
                return (double) (maxOrMin ? (x + w) : x);
            case 1:
                return (double) (maxOrMin ? (y + h) : h);
            default:
                throw new UnsupportedOperationException();
        }
    }


    @Override
    public double range(int dim) {
        switch (dim) {
            case 0:
                return (double) w;
            case 1:
                return (double) h;
            default:
                throw new IllegalArgumentException("Invalid dimension");
        }
    }


    @Override
    public final boolean contains(HyperRegion r) {
        if (this == r) return true;
        RectFloat R = (RectFloat) r;
        return contains(R.x, R.y, R.w, R.h);
    }
    @Override
    public final boolean intersects(HyperRegion r) {
        if (this == r) return true;
        RectFloat R = (RectFloat) r;
        return intersects(R.x, R.y, R.w, R.h);
    }

    public final boolean contains(float rx, float ry, float rw, float rh) {
        return (x <= rx) && (x + w >= rx + rw) && (y <= ry) && (y + h >= (ry + rh));
    }
    public final boolean intersects(float rx, float ry, float rw, float rh) {
        return (Math.max(rx, x) <= Math.min(rx + rw, x + w)) && (Math.max(ry, y) <= Math.min(ry + rh, y + h));
    }
    public final boolean intersectsX1Y1X2Y2(float x1, float y1, float x2, float y2) {
        //Longerval.intersects() = return max(x1, x2) <= min(y1, y2);
        return (Math.max(x1, x) <= Math.min(x2, x + w)) && (Math.max(y1, y) <= Math.min(y2, y + h));
    }

    @Override
    public double cost() {
        return (double) Math.abs(w * h);
    }

    @Override
    public final int hashCode() {
        return Util.hashCombine(Float.hashCode(x), Float.hashCode(y), Util.hashCombine(Float.hashCode(w), Float.hashCode(h)));
    }

    @Override
    public final boolean equals(Object o) {
        return this == o || ((o instanceof RectFloat) && equals((RectFloat)o, EPSILONf));

    }

    public final boolean equals(RectFloat o) {
        return this == o || equals(o, EPSILONf);
    }

    protected final boolean equals(Object o, float epsilon) {

        return this == o || ((o instanceof RectFloat) && equals((RectFloat)o, epsilon));
    }

    public final boolean equals(RectFloat o, float epsilon) {
        return o!=null && equals(o.x, o.y, o.w, o.h, epsilon);
    }

    public boolean equals(float xx, float yy, float ww, float hh, float epsilon) {
        return Util.equals(x, xx, epsilon) &&
                Util.equals(y, yy, epsilon) &&
                Util.equals(w, ww, epsilon) &&
                Util.equals(h, hh, epsilon);
    }

    public String toString() {
        return /* estimate */ '(' + INSTANCE.n4(cx()) + ',' + INSTANCE.n4(cy()) + ')' +
                'x' +
                '(' + INSTANCE.n4(w) + ',' + INSTANCE.n4(h) + ')';
    }

    @Override
    public int compareTo(RectFloat o) {
        throw new TODO();
    }


    /** max dimensional extent */
    public final float mag() {
        return Math.max(w, h);
    }

    public final boolean contains(v2 v) {
        return contains(v.x, v.y);
    }

    public final boolean contains(float px, float py) {
        return (px >= x && px <= x + w && py >= y && py <= y + h);
    }

    public final float bottom() { return y; }
    public final float left() {
        return x;
    }

    public final float right() {
        return x + w;
    }

    public final float top() {
        return y + h;
    }

    public final float cx() {
        return x + w / 2.0F;
    }
    public final float cy() {
        return y + h / 2.0F;
    }

    public RectFloat transform(float s, float ox, float oy) {
        ////        if (Util.equals(scale, 1f, ScalarValue.EPSILON) && offset.equalsZero())
            return orThisIfEqual(RectFloat.X0Y0WH(left()+ox, bottom()+oy, w * s, h * s));
    }

    public RectFloat scale(float s) {
        return !Util.equals((double) s, 1.0, EPSILON) ?
                RectFloat.XYWH(cx(), cy(), w * s, h * s) : this;
    }

    public RectFloat scale(float sw, float sh) {
        return !Util.equals((double) sw, 1.0, EPSILON) || !Util.equals((double) sh, 1.0, EPSILON) ?
                RectFloat.XYWH(cx(), cy(), w * sw, h * sh) : this;
    }

    public float radius() {
        return ((float) Math.sqrt((double) radiusSquare()));
    }

    public float radiusSquare() {
        float W = w / 2.0F, H = h / 2.0F;
        return (W * W + H * H);
    }

    public final float area() {
        return w * h;
    }

    /** note: this is sloppy lerp (non-cartesian) on dimensions independently */
    public RectFloat posLerp(float x, float y, float p) {
        return RectFloat.XYWH(lerpSafe(p, cx(), x),lerpSafe(p, cy(), y) ,w , h);
    }

//    public boolean nonZero(float epsilon) {
//        return w > epsilon && h > epsilon;
//    }

    public RectFloat rel(float cx, float cy, float pctW, float pctH) {
        float nw = this.w * pctW;
        float nh = this.h * pctH;
        return orThisIfEqual(X0Y0WH( x + cx , y + cy , nw, nh));
    }

    public v2 midPoint(RectFloat o) {
        return new v2((cx()+o.cx())/ 2.0F, (cy()+o.cy())/ 2.0F);
    }

    public v2 center() {
        return new v2(cx(), cy());
    }

    /** relative unit-scale position of the global point */
    public v2 unitize(v2 v) {
        return new v2((v.x - left())/w, (v.y - bottom()/h));
    }
    /** opposite of unitize */
    public v2 normalize(v2 v) {
        return new v2((v.x ) * w + left(), (v.y * h) + bottom());
    }

    /** computes the average of both position and scale parameters */
    public RectFloat mean(RectFloat o) {
        if (this == o) return this;
        return orThisIfEqual(X0Y0WH((x + o.x)/ 2.0F, (y+o.y)/ 2.0F, (w + o.w)/ 2.0F, (h + o.h)/ 2.0F));
    }

    /** clamp inside */
    public RectFloat clamp(RectFloat outer) {
        if (outer.contains(this))
            return this;
        else {
            float w = Math.min(this.w, outer.w), h = Math.min(this.h, outer.h);
//            if (outer.w < w || outer.h < h)
//                throw new WTF(this +  " is too large to fit inside " + outer);

            //if ((cx != cx) || (cy != cy)) randomize(bounds);
            float x = cx(); if (!Float.isFinite(x)) x = (float) 0;
            float y = cy(); if (!Float.isFinite(y)) y = (float) 0;

            return orThisIfEqual(XYWH(
                    Util.clamp(x, outer.left() + w / 2.0F, outer.right() - w / 2.0F),
                    Util.clamp(y, outer.bottom() + h / 2.0F, outer.top() - h / 2.0F),
                    w,h));
        }

    }

    public final RectFloat orThisIfEqual(RectFloat r) {
        if (r.equals(this))
            return this;
        else
            return r;
    }
}