package jcog.tree.rtree.rect;

import jcog.Util;
import jcog.tree.rtree.Spatialization;

/**
 * RectFloat2D with additional dx, dy vector to accumulate a movement before applying it in the form of a new RectFloat2D being generated
 */
public class MovingRectFloat2D extends RectFloat2D {

    float dx = 0, dy = 0;

    /**
     * radius cached
     */
    transient float rad = Float.NaN;

    public MovingRectFloat2D(RectFloat2D r) {
        super(r);
    }

    @Override
    public float radius() {
        float r = this.rad;
        if (r != r)
            return this.rad = super.radius();
        else
            return r;
    }

    public boolean isZeroMotion() {
        return Util.equals(dx, 0, Spatialization.EPSILON) && Util.equals(dy, 0, Spatialization.EPSILON);
    }

    @Override
    public RectFloat2D move(float dx, float dy) {
        this.dx += dx;
        this.dy += dy;
        return this;
    }

    public RectFloat2D get(float maxMovement, RectFloat2D limits) {
        float dx = this.dx;
        float dy = this.dy;
        float lx1 = limits.left();
        float x1 = left();
        if (x1 +dx < lx1) {
            dx = lx1 - x1; //bounce left
        }
        float ly1 = limits.top();
        float y1 = top();
        if (y1 + dy < ly1) {
            dy = ly1 - y1; //bounce top
        }
        float lx2 = limits.right();
        float x2 = right();
        if (x2 + dx > lx2) {
            dx = lx2 - x2; //bounce right
        }
        float ly2 = limits.bottom();
        float y2 = bottom();
        if (y2 + dy > ly2) {
            dy = ly2 - y2; //bounce bottom
        }
        dx = Util.clamp(dx, -maxMovement, +maxMovement);
        dy = Util.clamp(dy, -maxMovement, +maxMovement);
        return super.move(dx, dy);
    }
}
