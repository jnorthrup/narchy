package spacegraph.input.finger;

import jcog.math.v2;
import spacegraph.video.JoglSpace;

/** finger move trigger, using screen pixel scale */
abstract public class FingerMoveWindow extends FingerMove {

    public FingerMoveWindow(int button) {
        super(button);
    }

//    public FingerMovePixels(int button, boolean xAxis, boolean yAxis) {
//
//        super(button, xAxis, yAxis);
//    }

    protected volatile int windowStartX, windowStartY;


    protected abstract JoglSpace window();


    @Override
    protected boolean startDrag(Finger f) {
        JoglSpace w = window();
        windowStartX = w.display.getX();
        windowStartY = w.display.getY();
        return super.startDrag(f);
    }

    @Override public v2 pos(Finger finger) {
        return finger.posScreen;
    }
}
