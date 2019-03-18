package spacegraph.util;

import jcog.TODO;
import jcog.Util;
import jcog.math.v2;
import jcog.tree.rtree.rect.RectFloat;
import spacegraph.space2d.container.graph.Graph2D;

/**
 * similar to RectFloat2D with additional
 * except the x,y components are mutable
 */
public class MutableRectFloat<X> {

    public float cx, cy;
    public float cxPrev, cyPrev;
    public float w, h;

    /**
     * TODO make a MutableFloatRect proxy and adapter class for transparently controlling a graph2d node
     */
    @Deprecated
    public Graph2D.NodeVis<X> node;


    public MutableRectFloat() {

    }

    public MutableRectFloat(RectFloat r) {
        this();
        set(r);
    }

    public final MutableRectFloat setXYXY(float x1, float y1, float x2, float y2) {
        this.cx = (x1+x2)/2; this.cy = (y1+y2)/2;
        return size(
            (x2-x1), (y2-y1)
        );
    }

    public final MutableRectFloat set(float x, float y, float w, float h) {
        this.cxPrev = this.cx = x + w / 2;
        this.cyPrev = this.cy = y + h / 2;
        return size(w, h);

    }

    public final void set(MutableRectFloat r) {
        set(r.cx, r.cy, r.w, r.h);
    }

    public final void set(RectFloat r) {
        set(r.x, r.y, r.w, r.h);
    }

    public float radius() {
        float ww = w/2, hh = h/2;
        return (float) Math.sqrt((ww * ww) + (hh * hh));
    }

    public MutableRectFloat pos(float x, float y) {
        this.cx = x;
        this.cy = y;
        return this;
    }

    public MutableRectFloat move(float dx, float dy) {
        this.cx += dx;
        this.cy += dy;
        return this;
    }


    public float cx() {
        return cx;
    }

    public float cy() {
        return cy;
    }

    public void commit(float speedLimit) {
        v2 delta = new v2(cx, cy);
        float lenSq = delta.lengthSquared();
        if (lenSq > speedLimit * speedLimit) {

            delta.subbed(cxPrev, cyPrev);

            float len = (float) Math.sqrt(lenSq);
            delta.scaled(speedLimit / len);
            //x = Util.lerp(momentum, x0 + delta.x, x0);
            //y = Util.lerp(momentum, y0 + delta.y, y0);
            cx = cxPrev + delta.x;
            cy = cyPrev + delta.y;
        }

    }

    public void move(double dx, double dy) {
        move((float) dx, (float) dy);
    }


    public float area() {
        return w * h;
    }

    public float aspectRatio() {
        return h / w;
    }

    public void set(Graph2D.NodeVis<X> v) {
        set((this.node = v).bounds);
        v.mover = this;
    }

    public void clear() {
        if (node != null) {
            node.mover = null;
            node = null;
        }
        this.cxPrev = this.cyPrev = 0;
        set(RectFloat.Unit);
    }

    /**
     * keeps this rectangle within the given bounds
     */
    public void fenceInside(RectFloat bounds) {
        if ((cx != cx) || (cy != cy)) randomize(bounds);
        cx = Util.clamp(cx, bounds.left() + w / 2, bounds.right() - w / 2);
        cy = Util.clamp(cy, bounds.bottom() + h / 2, bounds.top() - h / 2);
    }

    public void randomize(RectFloat bounds) {
        throw new TODO();
    }

    public MutableRectFloat size(float w, float h) {
        this.w = w;
        this.h = h;
        return this;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "cx=" + cx +
                ", cy=" + cy +
                ", w=" + w +
                ", h=" + h +
                '}';
    }

    public float aspectExtreme() {
        return Math.max(w / h, h / w);
    }

    public float aspect() {
        return h / w;
    }

    public RectFloat immutable() {
        return RectFloat.XYWH(cx, cy, w, h);
    }

    /** stretch to maximum bounding rectangle of this rect and the provided point */
    public MutableRectFloat<X> mbr(float px, float py) {

        boolean change = false;

        float x1 = left(), x2 = right();
        if (x1 > px) {
            x1 = px;
            change = true;
        }
        if (x2 < px) {
            x2 = px;
            change = true;
        }
        float y1 = bottom(), y2 = top();
        if (y1 > py) {
            y1 = py;
            change = true;
        }
        if (y2 < py) {
            y2 = py;
            change = true;
        }

        if (change)
            return setXYXY(x1, y1, x2, y2);
        else
            return this;
    }

    public final float left() {
        return cx - w / 2;
    }

    public final float right() {
        return cx + w / 2;
    }

    public final float top() {
        return cy + h / 2;
    }

    public final float bottom() {
        return cy - h / 2;
    }


    public RectFloat normalizeScale(float cx, float cy, float cw, float ch, float minVisibleDim, float sw, float sh) {

        MutableRectFloat<X> extent = this;
        float ew = extent.w ;
        float px = (cx - extent.left()) / ew;
        float eh = extent.h;
        float py = (cy - extent.bottom()) / eh;

        float pw = Math.max(minVisibleDim, cw / ew);
        float ph = Math.max(minVisibleDim, ch / eh);

        return RectFloat.XYWH(
                px * sw,
                py * sh,
                pw * sw,
                ph * sh
        );

    }

}
