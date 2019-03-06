package spacegraph.util;

import jcog.Util;
import jcog.tree.rtree.rect.RectFloat;

public class RectAnimator {

    /** rect being animated */
    private final MutableRectFloat rect;

    private MutableRectFloat target = new MutableRectFloat().set(0, 0, 0, 1);

    /** time units until finished: default MS */
    int ttl = Integer.MIN_VALUE;

    public RectAnimator(MutableRectFloat r) {
        this.rect = r;
    }

    public void animate(int dtMS/*..*/) {
        int t = this.ttl;
        if (t == Integer.MIN_VALUE)
            return;

        if (t < 0) {
            //finished
            rect.set(target);
            this.ttl = Integer.MIN_VALUE;
        }

        if (dtMS > t) {
            dtMS = t;
            this.ttl = 0;
        } else {
            this.ttl = t - dtMS;
        }

        animate(rect, target, dtMS);
    }

    private void animate(MutableRectFloat from, MutableRectFloat to, int dtMS) {
        float rate = 0.99f;
        from.cx = Util.lerp(rate, to.cx, from.cx);
        from.cy = Util.lerp(rate, to.cy, from.cy);
        from.w = Util.lerp(rate, to.w, from.w);
        from.h = Util.lerp(rate, to.h, from.h);
    }

    /** reset the targetting */
    public void set(MutableRectFloat target, int duration) {
        this.target = target;
        this.ttl = duration;
    }
    public void set(RectFloat target, int duration) {
        set(new MutableRectFloat(target), duration);
    }

}
