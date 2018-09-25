package spacegraph.util;

import jcog.Util;
import jcog.tree.rtree.rect.RectFloat2D;
import spacegraph.space2d.container.Graph2D;
import spacegraph.util.math.v2;

/**
 * similar to RectFloat2D with additional
 * except the x,y components are mutable
 */
public class MovingRectFloat2D {

    public float x, y;
    public float x0, y0;
    public float w, h;

    public Graph2D.NodeVis node;
    private transient float rad;

    public MovingRectFloat2D() {
        clear();
    }

    public void set(RectFloat2D r) {
        this.x0 = this.x = r.x;
        this.y0 = this.y = r.y;
        size(r.w, r.h);
    }

    public float radius() {
        float r = this.rad;
        if (r != r) {
            r = this.rad = (float) Math.sqrt((w * w) + (h * h));
        }
        return r;
    }

    public MovingRectFloat2D pos(float dx, float dy) {
        this.x = dx + w / 2;
        this.y = dy + h / 2;
        return this;
    }

    public MovingRectFloat2D move(float dx, float dy) {
        this.x += dx;
        this.y += dy;
        return this;
    }

    public MovingRectFloat2D move(float dx, float dy, float rate) {
        this.x = Util.lerp(rate, this.x, this.x + dx);
        this.y = Util.lerp(rate, this.y, this.y + dy);
        return this;
    }

    public float cx() {
        return x + w / 2;
    }

    public float cy() {
        return y + h / 2;
    }

    public void limitSpeed(float speed) {
        v2 delta = new v2(x, y);
        delta.subbed(x0, y0);
        float len = delta.normalize();
        if (len > speed) {
            delta.scaled(speed);
            x = x0 + delta.x;
            y = y0 + delta.y;
        } else {
            x = Util.lerp(0.5f, x0, x);
            y = Util.lerp(0.5f, y0, y);
        }

    }

    public void move(double dx, double dy) {

        x += dx;
        y += dy;
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

    public <X> void set(Graph2D.NodeVis<X> v) {
        set((this.node = v).bounds);
        v.mover = this;
    }

    public void clear() {
        if (node!=null) {
            node.mover = null;
            node = null;
        }
        this.x0 = this.y0 = 0;
        set(RectFloat2D.Unit);
    }

    /** keeps this rectangle within the given bounds */
    public void fence(RectFloat2D bounds) {
        x = Util.clamp(x, bounds.left(), bounds.right()-w);
        y = Util.clamp(y, bounds.top(), bounds.bottom()-h);
    }

    public void size(float w, float h) {
        this.w = w;
        this.h = h;
        this.rad = Float.NaN; //TODO only set NaN if w,h change
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
