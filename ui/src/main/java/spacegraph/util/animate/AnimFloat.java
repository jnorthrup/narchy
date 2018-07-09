package spacegraph.util.animate;

import jcog.Util;
import org.apache.commons.lang3.mutable.MutableFloat;


public class AnimFloat extends MutableFloat implements Animated {

    private float target;
    private final MutableFloat speed;
    private boolean running = true;

    AnimFloat(float current, float speed) {
        super(Float.NaN);
        set(current);
        target = current;
        this.speed = new MutableFloat(speed);
    }

    public void stop() {
        running = false;
    }

    public void invalidate() {
        super.set(Float.NaN);
    }

    @Override
    public boolean animate(float dt) {

        float x = floatValue();
        if (x!=x) {
            
            super.set(target);
        } else {
            
            interpLERP(dt);
        }

        return running;
    }

    private void interpLERP(float dt) {
        float rate = speed.floatValue() * dt;
        
        super.set(
                Util.lerp(rate, floatValue(), target)
        );
    }

    @Override
    public void set(float value) {
        if (!Float.isFinite(floatValue()))
            super.set(value);
        target = value;
    }

}
