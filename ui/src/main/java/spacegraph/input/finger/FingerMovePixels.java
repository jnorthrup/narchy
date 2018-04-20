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

    private v2 startPos;
    protected volatile int windowStartX;
    protected volatile int windowStartY;

    @Override
    public boolean drag(Finger f) {
        if (startPos == null) {
            startDrag();
        }
        return super.drag(f);
    }

    public void startDrag() {
        startPos = pos(null);
        JoglSpace window = window();
        windowStartX = window.windowX;
        windowStartY = window.windowY;
    }

    protected abstract JoglSpace window();


    @Override
    public void stop(Finger finger) {
        startPos = null;
        super.stop(finger);
    }


    @Override
    protected v2 startPos(Finger finger) {
        return startPos;
    }

    @Override
    protected v2 pos(Finger finger) {
        return new v2(Finger.pointer.getX(), Finger.pointer.getY());
    }

}
