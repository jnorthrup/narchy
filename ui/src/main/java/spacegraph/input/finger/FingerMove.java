package spacegraph.input.finger;

import jcog.math.v2;

public abstract class FingerMove extends FingerDragging {

    private final float xSpeed;
    private final float ySpeed;
    private v2 startPos, current = new v2();

    public FingerMove(int button) {
        this(button, true, true);
    }

    /** for locking specific axes */
    FingerMove(int button, boolean xAxis, boolean yAxis) {
        this(button, xAxis ? 1 : 0, yAxis ? 1 : 0);
        assert(xAxis || yAxis);
    }

    public FingerMove(int button, float xSpeed, float ySpeed) {
        super(button);
        this.xSpeed = xSpeed;
        this.ySpeed = ySpeed;
    }


    protected abstract void move(float tx, float ty);

    @Override
    protected boolean drag(Finger f) {

        current.set(pos(f));
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
    protected boolean startDrag(Finger f) {
        this.startPos = pos(f).clone();
        return super.startDrag(f);
    }

    v2 pos(Finger finger) {
        return finger.posOrtho;
    }


}
