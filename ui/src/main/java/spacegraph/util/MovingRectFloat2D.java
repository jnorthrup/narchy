package spacegraph.util;

import jcog.Util;
import jcog.tree.rtree.rect.RectFloat2D;

/**
 * similar to RectFloat2D with additional
 * except the x,y components are mutable
 */
public class MovingRectFloat2D {

    public float x;
    public float y;
    public float w;
    public float h;
    private float rad;

    public MovingRectFloat2D() {

    }

    public void set(RectFloat2D r) {
        this.x = r.x;
        this.y = r.y;

        this.w = r.w;
        this.h = r.h;
        this.rad = (float) Math.sqrt( (w*w)+(h*h) );
    }

    public float radius() {
        return rad;
    }

    public MovingRectFloat2D pos(float dx, float dy) {
        this.x = dx + w/2;
        this.y = dy + h/2;
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

    public RectFloat2D get(float ox, float oy) {






















        return RectFloat2D.XYWH(
                ox + cx(),
                oy + cy(),
                w, h);
    }

    public float cx() {
        return x + w/2;
    }
    public float cy() {
        return y + h/2;
    }

    public void move(double dx, double dy) {
        x += dx;
        y += dy;
    }

    public void moveTo(float x, float y, float rate) {
        this.x = Util.lerp(rate, this.x, x);
        this.y = Util.lerp(rate, this.y, y);
    }
}
