package spacegraph.input.finger;

import spacegraph.util.math.v2;

public abstract class FingerMove extends FingerDragging {

    protected final float xSpeed, ySpeed;
    protected v2 startPos;

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
            v2 start = startPos;
            float tx = (xSpeed != 0 ? (current.x - start.x) * xSpeed : 0);
            float ty = (ySpeed != 0 ? (current.y - start.y) * ySpeed : 0);
            move(tx, ty);
            return true;
        }

        return false;
    }

    @Override
    public boolean start(Finger f) {
        this.startPos = pos(f).clone();
        return super.start(f);
    }

    protected v2 pos(Finger finger) {
        return finger.pos;
    }


}
