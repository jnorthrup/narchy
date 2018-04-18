package spacegraph.input.finger;

import spacegraph.util.math.v2;

public abstract class FingerMove extends FingerDragging {

    protected final float xSpeed, ySpeed;

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

    public abstract float xStart();
    public abstract float yStart();

    public abstract void move(float tx, float ty);

    @Override public boolean drag(Finger finger) {



        v2 fh = finger.pos;
        //if (fh!=null) {
            v2 fhd = finger.hitOnDown[button];
            if (fhd!=null) {
                float tx = xStart() + (xSpeed > 0 ? (fh.x - fhd.x) * xSpeed : 0);
                float ty = yStart() + (ySpeed > 0 ? (fh.y - fhd.y) * ySpeed : 0);
                move(tx, ty);
                return true;
            }
        //}
        return false;
    }


}
