package spacegraph.util;

import jcog.Util;
import jcog.tree.rtree.rect.RectFloat2D;

/**
 * similar to RectFloat2D with additional
 * except the x,y components are mutable
 */
public class MovingRectFloat2D {


    float x, y;
    final float w, h;
    final float rad;

    public MovingRectFloat2D(RectFloat2D r) {
        this.x = r.x;
        this.y = r.y;

        this.w = r.w;
        this.h = r.h;
        this.rad = (float) Math.sqrt( (w*w)+(h*h) );
    }

    public float radius() {
        return rad;
    }


    public MovingRectFloat2D move(float dx, float dy) {
        this.x += dx;
        this.y += dy;
        return this;
    }

    public RectFloat2D get(float ox, float oy) {
//        float lx1 = limits.left();
//        float x1 = left();
//        if (x1 +dx < lx1) {
//            dx = x1 - lx1; //bounce left
//        }
//        float ly1 = limits.top();
//        float y1 = top();
//        if (y1 + dy < ly1) {
//            dy = y1 - ly1; //bounce top
//        }
//        float lx2 = limits.right();
//        float x2 = right();
//        if (x2 + dx > lx2) {
//            dx = lx2 - x2; //bounce right
//        }
//        float ly2 = limits.bottom();
//        float y2 = bottom();
//        if (y2 + dy > ly2) {
//            dy = ly2 - y2; //bounce bottom
//        }
//        dx = Util.clamp(dx, -maxMovement, +maxMovement);
//        dy = Util.clamp(dy, -maxMovement, +maxMovement);
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
