package spacegraph.input.finger;

import spacegraph.space2d.Surface;
import spacegraph.util.math.v2;

import java.util.function.Consumer;

public class DoubleClicking {

    private static final float PIXEL_DISTANCE_THRESHOLD = 0.51f;

    private final int button;

    /** accepts the mouse point where clicked */
    private final Consumer<v2> onDoubleClick;
    private final Surface target;

    private v2 doubleClickSpot = null;

    /** in milliseconds */
    private final long maxDoubleClickTimeNS = 350 * 1000 * 1000;

    /** in milliseconds */
    private long doubleClickTime = Long.MIN_VALUE;

    private int count = 0;

    public DoubleClicking(int button, Consumer<v2> doubleClicked, Surface target) {
        this.target = target;
        this.button = button;
        this.onDoubleClick = doubleClicked;
    }


    public boolean update(Finger finger) {
        if (!_update(finger)) {
            if (count > 0)
                reset();
            return false;
        }
        return true;
    }

    public void reset() {
        doubleClickSpot = null;
        doubleClickTime = Long.MIN_VALUE;
        count = 0;
    }

    private boolean _update(Finger finger) {
        //        if (finger!=null)
//            System.out.println(finger.buttonSummary());


        if (finger == null || !finger.clickedNow(button, target))
            return count > 0; //could be in-between presses

        count++;

        v2 downHit = finger.pressPosPixel[button].clone();

        if (count == 2) {

            if (doubleClickSpot!=null) {
                if (System.nanoTime() - doubleClickTime > maxDoubleClickTimeNS)
                    return false; //too long
            }

            if (doubleClickSpot!=null && !doubleClickSpot.equals(downHit, PIXEL_DISTANCE_THRESHOLD))
                return false; //not on same point


            reset();
            onDoubleClick.accept(finger.posOrtho);
            return true;

        } else if (count == 1) {
            doubleClickSpot = downHit;
            doubleClickTime = System.nanoTime();
        }

        return true; //continues

    }

}
