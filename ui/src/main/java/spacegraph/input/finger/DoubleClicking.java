package spacegraph.input.finger;

import spacegraph.util.math.v2;

import java.util.function.Consumer;

public class DoubleClicking {

    static final float PIXEL_DISTANCE_THRESHOLD = 0.51f;

    private final int button;

    /** accepts the mouse point where clicked */
    private final Consumer<v2> onDoubleClick;

    v2 doubleClickSpot = null;

    /** in milliseconds */
    final long maxDoubleClickTime = 350;

    /** in milliseconds */
    long doubleClickTime = Long.MIN_VALUE;

    int count = 0;

    public DoubleClicking(int button, Consumer<v2> doubleClicked) {
        this.button = button;
        this.onDoubleClick = doubleClicked;
    }


    public boolean update(Finger finger) {
        if (finger!=null && finger.pressedNow(button))  {
            //System.out.println("click " + doubleClickSpot + " " + finger.hitOnDown[0] + " " + (System.currentTimeMillis() - doubleClickTime));
            v2 downHit = finger.hitOnDownGlobal[button];
            if (downHit!=null && doubleClickSpot!=null && doubleClickSpot.equals(downHit, PIXEL_DISTANCE_THRESHOLD) &&
                    System.currentTimeMillis() - doubleClickTime < maxDoubleClickTime) {
                //System.out.println("double click");
                if (count++ == 2) {
                    doubleClickSpot = null;
                    doubleClickTime = Long.MIN_VALUE;
                    count = 0;
                    onDoubleClick.accept(finger.pos);
                    return true;
                }
            }

            doubleClickSpot = finger.hitOnDownGlobal[button];
            doubleClickTime = System.currentTimeMillis();
        }

        return false;
    }

}
