package spacegraph.util.animate;

import jcog.Util;
import jcog.data.NumberX;
import jcog.data.atomic.AtomicFloat;
import jcog.math.v3;

/** TODO: implements Animator<v3> following RectAnimator */
public class v3Anim extends v3 implements Animated {

    protected final v3 target = new v3();
    public final NumberX speed;
    private boolean running = true;

    enum InterpolationCurve {
        Exponential {
            @Override
            public void interp(float dt, v3Anim v) {
                var d = Math.max(0, v.speed.floatValue() * dt);
                v.moveDirect(d, 0.75f);
            }
        },
        Linear {
            @Override
            public void interp(float dt, v3Anim v) {

                /** constants speed: delta to move, in length */
                var d = Math.max(0, v.speed.floatValue() * dt);
                v.moveDirect(d, 1f);

            }
        },
        LERP {
            @Override
            public void interp(float dt, v3Anim v) {
                var rate = 10f * dt;
                var w = v.target;
                if (rate >= 1) {
                    v.setDirect(w);
                    v.set(w);
                } else {

                    v.setDirect(
                            Util.lerp(rate, v.x, w.x),
                            Util.lerp(rate, v.y, w.y),
                            Util.lerp(rate, v.z, w.z)
                    );
                }

            }
        };

        protected abstract void interp(float dt, v3Anim v);
    }

    /** d is the maximum distance to move the current state to the target, linearly */
    protected void moveDirect(float d, float proportion) {
        if (d < Float.MIN_NORMAL)
            return;


        var w = this.target;
        var dx = w.x - this.x;
        var dy = w.y - this.y;
        var dz = w.z - this.z;

        var lenSq = dx * dx + dy * dy + dz * dz;
        if (lenSq < Util.sqrtMIN_NORMAL)
            return;


        var len = (float) Math.sqrt(lenSq);
        d = Math.min(len*proportion, d) / len;









        
        this.addDirect(
                
                dx * d,
                dy * d,
                dz * d);
        

    }


    private final InterpolationCurve curve = InterpolationCurve.LERP;

    public v3Anim(float speed) {
        this(0, 0, 0, speed);
    }

    public v3Anim(float x, float y, float z, float speed) {
        super(x, y, z);
        target.set(this);
        this.speed = new AtomicFloat(speed);
    }

    public void stop() {
        running = false;
    }

    public void invalidate() {
        super.set(Float.NaN, Float.NaN, Float.NaN);
    }

    @Override
    public boolean animate(float dt) {

        var px = this.x;
        if (px != px) {
            super.set(target);
        } else {
            curve.interp(dt, this);
        }

        return running;
    }


    @Override
    public void set(float x, float y, float z) {
         target.set(x, y, z);
    }

    @Override
    public void add(float dx, float dy, float dz) {
        target.add( dx,  dy,  dz);
    }

    @Override
    public void add(float dx, float dy) {
        target.add( dx,  dy,  0);
    }

    public final void setDirect(v3 v) {
        setDirect(v.x, v.y, v.z);
    }

    public void setDirect(float x, float y, float z) {
        super.set(x, y, z);
    }

    protected final void addDirect(float x, float y, float z) {
        setDirect(this.x + x, this.y + y, this.z + z);
    }
}
