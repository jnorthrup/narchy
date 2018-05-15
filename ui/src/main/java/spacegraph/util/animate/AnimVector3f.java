package spacegraph.util.animate;

import jcog.Util;
import jcog.tree.rtree.Spatialization;
import org.apache.commons.lang3.mutable.MutableFloat;
import spacegraph.util.math.v3;

public class AnimVector3f extends v3 implements Animated {

    final v3 target = new v3();
    protected final MutableFloat speed;
    private boolean running = true;

    enum InterpolationCurve {
        Exponential {
            @Override
            public void interp(float dt, AnimVector3f v) {
                float d = Math.max(0, v.speed.floatValue() * dt);
                v.moveDirect(d, 0.75f);
            }
        },
        Linear {
            @Override
            public void interp(float dt, AnimVector3f v) {

                /** constants speed: delta to move, in length */
                float d = Math.max(0, v.speed.floatValue() * dt);
                v.moveDirect(d, 1f);

            }
        },
        LERP {
            @Override
            public void interp(float dt, AnimVector3f v) {
                float rate = v.speed.floatValue() * dt;
                v3 w = v.target;
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

        abstract public void interp(float dt, AnimVector3f v);
    }

    /** d is the maximum distance to move the current state to the target, linearly */
    public void moveDirect(float d, float proportion) {
        if (d < Float.MIN_NORMAL)
            return;

        //HACK use constant velocity
        v3 w = this.target;
        float dx = w.x - this.x;
        float dy = w.y - this.y;
        float dz = w.z - this.z;

        float lenSq = dx * dx + dy * dy + dz * dz;
        if (lenSq < Spatialization.sqrtEPSILONf)
            return;

//        if (lenSq < closeEnoughSq) {
//            //within one distance
//            //System.out.println(dt + " " + "target==" + target);
//            return super.setIfChange(target.x, target.y, target.z, BulletGlobals.SIMD_EPSILON);
//
//        } else {
        float len = (float) Math.sqrt(lenSq);
        d = Math.min(len*proportion, d) / len;


//            float v = Math.min(1,
//                    (
//                    //rate //constant speed
//                    (float) (d * Math.sqrt(lenSq)) //proportional speed
//            ));


        //constant speed
        this.addDirect(
                //return super.setIfChange(
                dx * d,
                dy * d,
                dz * d);
        //, v);
//        }
    }


    InterpolationCurve curve = InterpolationCurve.Exponential;

    public AnimVector3f(float speed) {
        this(0, 0, 0, speed);
    }

    public AnimVector3f(float x, float y, float z, float speed) {
        super(x, y, z);
        target.set(this);
        this.speed = new MutableFloat(speed);
    }

    public void stop() {
        running = false;
    }

    public void invalidate() {
        super.set(Float.NaN, Float.NaN, Float.NaN);
    }

    @Override
    public boolean animate(float dt) {

        float px = this.x;
        if (px != px) {
            //invalidated
            super.set(target);
        } else {
            curve.interp(dt, this);
        }

        return running;
    }



    @Override
    public void set(float x, float y, float z) {
//        float px = this.x;
//        if (px != px || x != x)
//            super.set(x, y, z); //initialization: if invalidated, use the target value immediately

        target.set(x, y, z); //interpolation
    }

    @Override
    public void add(float dx, float dy, float dz) {
        //super.add(dx, dy, dz);
        target.add( dx,  dy,  dz);
    }

    @Override
    public void add(float dx, float dy) {
        //super.add(dx, dy, dz);
        target.add( dx,  dy,  0);
    }

    public void setDirect(v3 v) {
        super.set(v);
    }

    public void setDirect(float x, float y, float z) {
        super.set(x, y, z);
    }
    public void addDirect(float x, float y, float z) {
        setDirect(this.x + x, this.y + y, this.z + z);
    }
}
