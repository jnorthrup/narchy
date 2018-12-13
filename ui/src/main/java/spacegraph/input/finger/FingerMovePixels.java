package spacegraph.input.finger;

import jcog.math.v2;
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
    protected boolean startDrag(Finger f) {
        JoglSpace w = window();
        windowStartX = w.io.getX();
        windowStartY = w.io.getY();
        return super.startDrag(f);
    }

    @Override
    protected v2 pos(Finger finger) {
        return finger.posPixel;
    }

}
