package spacegraph.util;

import jcog.TODO;
import jcog.Util;
import jcog.math.v2;
import jcog.tree.rtree.rect.RectFloat;
import spacegraph.space2d.container.graph.NodeVis;

/**
 * similar to RectFloat2D with additional
 * except the x,y components are mutable
 *
 * the x and y correspond to the center of the float (unlke RectFloat which corresponds to bottom-left corner)
 */
public class MutableRectFloat<X> extends v2 {

    private float cxPrev;
    private float cyPrev;
    public float w;
    public float h;

    /**
     * TODO make a MutableFloatRect proxy and adapter class for transparently controlling a graph2d node
     */
    @Deprecated
    public NodeVis<X> node;


    public MutableRectFloat() {

    }

    public MutableRectFloat(RectFloat r) {
        this();
        set(r);
    }

    private MutableRectFloat setXYXY(float x1, float y1, float x2, float y2) {
        this.x = (x1+x2)/ 2.0F; this.y = (y1+y2)/ 2.0F;
        return size(
            (x2-x1), (y2-y1)
        );
    }

    private MutableRectFloat setXYWH(float x, float y, float w, float h) {
        this.cxPrev = this.x = x;
        this.cyPrev = this.y = y;
        return size(w, h);
    }

    public final MutableRectFloat setX0Y0WH(float x, float y, float w, float h) {
        this.cxPrev = this.x = x + w / 2.0F;
        this.cyPrev = this.y = y + h / 2.0F;
        return size(w, h);
    }

    public final void set(MutableRectFloat r) {
        setXYWH(r.x, r.y, r.w, r.h);
    }

    final boolean setIfChanged(MutableRectFloat r, float epsilon) {
        if (equals(r, epsilon))
            return false;
        set(r);
        return true;
    }

    public final void set(RectFloat r) {
        setX0Y0WH(r.x, r.y, r.w, r.h);
    }

    public float radius() {
        float ww = w/ 2.0F, hh = h/ 2.0F;
        return (float) Math.sqrt((double) ((ww * ww) + (hh * hh)));
    }

    public MutableRectFloat pos(float x, float y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public MutableRectFloat move(float dx, float dy) {
        this.x += dx;
        this.y += dy;
        return this;
    }


    public float cx() {
        return x;
    }

    public float cy() {
        return y;
    }

    public void commitLerp(float rate) {
        v2 v = new v2(cxPrev, cyPrev);
        v.lerp(new v2(x, y)/*this*/, rate);
        set(v);
    }

//    public void commit(float speedLimit) {
//        v2 delta = new v2(x -cxPrev, y -cyPrev);
//        float lenSq = delta.lengthSquared();
//        if (lenSq > speedLimit * speedLimit) {
//            float len = (float) Math.sqrt(lenSq);
//            delta.scale(speedLimit / len);
//            x = cxPrev + delta.x; cxPrev = x;
//            y = cyPrev + delta.y; cyPrev = y;
//        }
//
//    }

    public void move(double dx, double dy) {
        move((float) dx, (float) dy);
    }


    public float area() {
        return w * h;
    }

    public float aspectRatio() {
        return h / w;
    }

    public void set(NodeVis<X> v) {
        set((this.node = v).bounds);
        v.mover = this;
    }

    public void clear() {
        if (node != null) {
            node.mover = null;
            node = null;
        }
        this.cxPrev = this.cyPrev = (float) 0;
        set(RectFloat.Unit);
    }

    /**
     * keeps this rectangle within the given bounds
     */
    public void clamp(RectFloat bounds) {
        if ((x != x) || (y != y)) randomize(bounds);
        x = Util.clampSafe(x, bounds.left() + w / 2.0F, bounds.right() - w / 2.0F);
        y = Util.clampSafe(y, bounds.bottom() + h / 2.0F, bounds.top() - h / 2.0F);
    }

    private static void randomize(RectFloat bounds) {
        throw new TODO();
    }

    public MutableRectFloat size(float w, float h) {
        this.w = w;
        this.h = h;
        return this;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '{' +
                "cx=" + x +
                ", cy=" + y +
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
        return RectFloat.XYWH(x, y, w, h);
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
        return x - w / 2.0F;
    }

    public final float right() {
        return x + w / 2.0F;
    }

    public final float top() {
        return y + h / 2.0F;
    }

    public final float bottom() {
        return y - h / 2.0F;
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
