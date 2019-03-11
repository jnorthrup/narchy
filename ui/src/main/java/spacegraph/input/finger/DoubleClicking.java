package spacegraph.input.finger;

import jcog.math.v2;
import spacegraph.space2d.Surface;

import java.util.function.Consumer;

public class DoubleClicking {

    private static final float PIXEL_DISTANCE_THRESHOLD = 0.51f;

    private final int button;

    /** accepts the mouse point where clicked */
    private final Consumer<v2> onDoubleClick;
    private final Surface clicked;

    private v2 doubleClickSpot = null;

    /** in milliseconds */
    private final long maxDoubleClickTimeNS = 350 * 1000 * 1000;

    /** in milliseconds */
    private long doubleClickTime = Long.MIN_VALUE;


    public DoubleClicking(int button, Consumer<v2> doubleClicked, Surface clicked) {
        this.clicked = clicked;
        this.button = button;
        this.onDoubleClick = doubleClicked;
    }


//    public boolean update(Finger finger) {
//        if (finger.clickedNow(button, clicked)) {
//
//        }
//    }



    public void reset() {
        doubleClickSpot = null;
        doubleClickTime = Long.MIN_VALUE;
        count = 0;
    }

    int count = 0;
    public boolean update(Finger finger) {
        //        if (finger!=null)
//            System.out.println(finger.buttonSummary());


        if (!finger.clickedNow(button, clicked))
            return count > 0; //could be in-between presses

        count++;

        v2 downHit = finger.pressPosPixel[button].clone();

        if (count == 2) {

            if (doubleClickSpot!=null) {
                if (System.nanoTime() - doubleClickTime > maxDoubleClickTimeNS) {
                    reset();
                    return false; //too long
                }
            }

            if (doubleClickSpot!=null && !doubleClickSpot.equals(downHit, PIXEL_DISTANCE_THRESHOLD)) {
                reset();
                return false; //not on same point
            }



            reset();
            onDoubleClick.accept(finger.posGlobal(clicked));
            return true;

        } else if (count == 1) {
            doubleClickSpot = downHit;
            doubleClickTime = System.nanoTime();
        }

        return true; //continues

    }

}
