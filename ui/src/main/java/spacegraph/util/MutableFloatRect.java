package spacegraph.util;

import jcog.Util;
import jcog.tree.rtree.rect.RectFloat;
import spacegraph.space2d.container.graph.Graph2D;
import spacegraph.util.math.v2;

/**
 * similar to RectFloat2D with additional
 * except the x,y components are mutable
 */
public class MutableFloatRect<X> {

    public float x, y;
    public float x0, y0;
    public float w, h;

    public Graph2D.NodeVis<X> node;
    private transient float rad;

    public MutableFloatRect() {
        clear();
    }

    public void set(RectFloat r) {
        this.x0 = this.x = r.cx();
        this.y0 = this.y = r.cy();
        size(r.w, r.h);
    }

    public float radius() {
        return rad;
    }

    public MutableFloatRect pos(float x, float y) {
        this.x = x;
        this.y = y;
        return this;
    }

    public MutableFloatRect move(float dx, float dy) {
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

    public void commit(float speedLimit) {
        v2 delta = new v2(x, y);
        float lenSq = delta.lengthSquared();
        if (lenSq > speedLimit*speedLimit) {

            delta.subbed(x0, y0);

            float len = (float) Math.sqrt(lenSq);
            delta.scaled(speedLimit/len);
            //x = Util.lerp(momentum, x0 + delta.x, x0);
            //y = Util.lerp(momentum, y0 + delta.y, y0);
            x = x0 + delta.x;
            y = y0 + delta.y;
        }

    }

    public void move(double dx, double dy) {
        move((float)dx, (float)dy);
    }

    public void moveTo(float x, float y, float rate) {
        this.x = Util.lerp(rate, this.x, x);
        this.y = Util.lerp(rate, this.y, y);
    }

    public float area() {
        return w * h;
    }

    public float aspectRatio() {
        return h/w;
    }

    public void set(Graph2D.NodeVis<X> v) {
        set((this.node = v).bounds);
        v.mover = this;
    }

    public void clear() {
        if (node!=null) {
            node.mover = null;
            node = null;
        }
        this.x0 = this.y0 = 0;
        set(RectFloat.Unit);
    }

    /** keeps this rectangle within the given bounds */
    public void fence(RectFloat bounds) {
        x = Util.clamp(x, bounds.left()+w/2, bounds.right()-w/2);
        y = Util.clamp(y, bounds.top()+h/2, bounds.bottom()-h/2);
    }

    public void size(float w, float h) {
        this.w = w;
        this.h = h;
        this.rad = (float) Math.sqrt((w * w) + (h * h));
    }

    @Override
    public String toString() {
        return "MovingRectFloat2D{" +
                "x=" + x +
                ", y=" + y +
                ", w=" + w +
                ", h=" + h +
                '}';
    }

    public float aspectExtreme() {
        return Math.max(w/h, h/w);
    }
}
