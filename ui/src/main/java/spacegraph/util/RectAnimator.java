package spacegraph.util;

import jcog.Util;
import jcog.tree.rtree.rect.RectFloat;
import org.eclipse.collections.api.tuple.primitive.ObjectFloatPair;
import spacegraph.util.animate.Animator;

import java.util.concurrent.atomic.AtomicReference;

import static org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair;

abstract public class RectAnimator implements Animator<MutableRectFloat> {

    /** time units until the next movement is finished (in seconds) */
    protected volatile float ttl = Float.NaN;

    /** for determining the total intended animation time */
    protected float length;

    public static class ExponentialRectAnimator extends RectAnimator {

        public ExponentialRectAnimator(MutableRectFloat animated) {
            super(animated);
        }

        @Override
        protected void animate(MutableRectFloat from, MutableRectFloat to, float dt) {

            float r = (float) Math.exp(-dt/length); //TODO verify
            from.cx = Util.lerpSafe(r, to.cx, from.cx);
            from.cy = Util.lerpSafe(r, to.cy, from.cy);
            from.size(
                    Util.lerpSafe(r, to.w, from.w),
                    Util.lerpSafe(r, to.h, from.h)
            );

        }
    }
    /** rect being animated */
    private final MutableRectFloat rect;

    private final MutableRectFloat target = new MutableRectFloat().set(0, 0, 0, 1);



    public RectAnimator(MutableRectFloat toAnimate) {
        this.rect = toAnimate;
    }

    @Override
    public final MutableRectFloat animated() {
        return rect;
    }


    @Override public final boolean animate(float dt/*..*/) {


        ObjectFloatPair<MutableRectFloat> n = next.getAndSet(null);
        if (n!=null) {
            target.set(n.getOne());
            this.length = this.ttl = n.getTwo();
        }

        float t = this.ttl;
        if (t==t) {


            boolean finished = t <= 0;

            if (finished) {
                //finished
                this.ttl = Float.NaN;
                rect.set(target);
            } else {
                //continue
                if (dt > t) {
                    dt = t;
                    this.ttl = 0;
                } else {
                    this.ttl = t - dt;
                }
                animate(rect, target, dt);
            }
        }

        return true;
    }

    abstract protected void animate(MutableRectFloat from, MutableRectFloat to, float dt);


    private final AtomicReference<ObjectFloatPair<MutableRectFloat>> next = new AtomicReference();

    /** reset the targetting */
    public void set(MutableRectFloat target, float ttl) {
        this.next.set(pair(target, ttl));
    }

    public void set(RectFloat target, float ttl) {
        set(new MutableRectFloat(target), ttl);
    }

}
