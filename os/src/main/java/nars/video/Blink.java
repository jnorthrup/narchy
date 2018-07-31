package nars.video;

import jcog.math.FloatRange;
import jcog.random.XorShift128PlusRandom;
import jcog.signal.wave2d.Bitmap2D;

import java.util.Random;

/**
 * stochastic blinking filter
 */
public class Blink implements Bitmap2D {

    private final Bitmap2D in;

    /**
     * percentage of duty cycle during which input is visible
     */
    private final FloatRange visibleProb = new FloatRange(0, 0, 1f);

    final Random rng = new XorShift128PlusRandom(1);
    boolean blinked;

    public Blink(Bitmap2D in, float rate) {
        this.in = in;
        this.visibleProb.set(rate);
    }

    @Override
    public void update() {
        blinked = rng.nextFloat() < visibleProb.floatValue();
        if (!blinked) {
            in.update();
        }
    }

    @Override
    public int width() {
        return in.width();
    }

    @Override
    public int height() {
        return in.height();
    }

    @Override
    public float brightness(int xx, int yy) {
        return blinked ? in.brightness(xx, yy) : 0f;
    }
}
