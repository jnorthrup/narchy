package spacegraph.input.finger;

import spacegraph.util.math.v2;

public abstract class FingerMove extends FingerDragging {

    protected final float xSpeed, ySpeed;

    public FingerMove(int button) {
        this(button, true, true);
    }

    /** for locking specific axes */
    public FingerMove(int button, boolean xAxis, boolean yAxis) {
        this(button, xAxis ? 1 : 0, yAxis ? 1 : 0);
        assert(xAxis || yAxis);
    }

    public FingerMove(int button, float xSpeed, float ySpeed) {
        super(button);
        this.xSpeed = xSpeed;
        this.ySpeed = ySpeed;
    }


    public abstract void move(float tx, float ty);

    @Override public boolean drag(Finger finger) {



        v2 current = pos(finger);
        if (current!=null) {
            v2 start = startPos(finger);
            if (start != null) {
                float tx = xStart() + (xSpeed > 0 ? (current.x - start.x) * xSpeed : 0);
                float ty = yStart() + (ySpeed > 0 ? (current.y - start.y) * ySpeed : 0);
                move(tx, ty);
                return true;
            }
        }

        return false;
    }

    protected v2 startPos(Finger finger) {
        return finger.hitOnDown[button];
    }
    protected v2 pos(Finger finger) {
        return finger.pos;
    }

    public float xStart() { return 0; }

    public float yStart() { return 0; }


}
