package spacegraph.input.finger;

import jcog.math.v2;
import spacegraph.video.JoglDisplay;

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


    protected abstract JoglDisplay window();


    @Override
    protected boolean ready(Finger f) {
        JoglDisplay w = window();
        windowStartX = w.video.getX();
        windowStartY = w.video.getY();
        return super.ready(f);
    }

    @Override public final v2 pos(Finger finger) {
        return finger.posScreen;
    }
}
