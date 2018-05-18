package spacegraph.input.finger;

import spacegraph.util.math.v2;
import spacegraph.video.JoglSpace;

/** finger move trigger, using screen pixel scale */
abstract public class FingerMovePixels extends FingerMove {

    public FingerMovePixels(int button) {
        super(button);
    }

    public FingerMovePixels(int button, boolean xAxis, boolean yAxis) {

        super(button, xAxis, yAxis);
    }

    protected volatile int windowStartX, windowStartY;


    protected abstract JoglSpace window();

    @Override
    public boolean start(Finger f) {

        JoglSpace w = window();
        windowStartX = w.getX();
        windowStartY = w.getY();

        return super.start(f);
    }

    @Override
    protected v2 pos(Finger finger) {
        return new v2(finger.posPixel.x, finger.posPixel.y);
    }

}
