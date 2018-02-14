package spacegraph.phys.util;

import jcog.Util;
import org.apache.commons.lang3.mutable.MutableFloat;
import spacegraph.math.v3;

public class AnimVector3f extends v3 implements Animated {

    final v3 target = new v3();
    final MutableFloat speed;
    private boolean running = true;

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
            //interpLinear(dt);
            interpLERP(dt);
        }

        return running;
    }

    public void interpLERP(float dt) {
        float rate = speed.floatValue() * dt;
        if (rate >= 1) {
            super.set(target.x, target.y, target.z );
        } else {

            super.set(
                    Util.lerp(rate, x, target.x),
                    Util.lerp(rate, y, target.y),
                    Util.lerp(rate, z, target.z)
            );
        }
    }

    public void interpLinear(float dt) {
        //HACK use constant velocity
        float dx = target.x - x;
        float dy = target.y - y;
        float dz = target.z - z;

        float rate = speed.floatValue() * dt;
        float lenSq = dx * dx + dy * dy + dz * dz;
        float len = (float) Math.sqrt(lenSq);

        if (len < rate) {
            //within one distance
            //System.out.println(dt + " " + "target==" + target);
            super.set(target);
        } else {
            float v = rate / len;
            //System.out.println(dt + " " + "target.." + target + " " + len + " " + v);

            super.set(
                    x + dx * v,
                    y + dy * v,
                    z + dz * v);
        }
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
}
