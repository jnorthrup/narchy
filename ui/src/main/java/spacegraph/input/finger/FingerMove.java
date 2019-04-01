package spacegraph.input.finger;

import jcog.math.v2;

public abstract class FingerMove extends Dragging {

    private final float xSpeed, ySpeed;
    private final v2 prev = new v2(), current = new v2();

    protected FingerMove(int button) {
        this(button, true, true);
    }

    /** for locking specific axes */
    FingerMove(int button, boolean xAxis, boolean yAxis) {
        this(button, xAxis ? 1 : 0, yAxis ? 1 : 0);
        assert(xAxis || yAxis);
    }

    private FingerMove(int button, float xSpeed, float ySpeed) {
        super(button);
        this.xSpeed = xSpeed;
        this.ySpeed = ySpeed;
    }

    /** internal coordinate vector */
    public abstract v2 pos(Finger finger);

    /** accepts an iterational position adjustment */
    protected abstract void move(float tx, float ty);

    /** whether the position adjustment is tracked incrementally (delta from last iteration),
     * or absolutely (delta from initial conditions) */
    protected boolean incremental() {
        return false;
    }

    @Override
    protected boolean drag(Finger f) {

        v2 next = pos(f);
        if (next !=null) {

            float tx = xSpeed != 0 ? (next.x - prev.x) * xSpeed : 0;
            float ty = ySpeed != 0 ? (next.y - prev.y) * ySpeed : 0;
            if (incremental()) {
                prev.set(current);
                current.set(next);
            } else {
                current.set(next);
            }

            move(tx, ty);
            return true;
        }

        return false;
    }

    @Override
    protected boolean startDrag(Finger f) {
        this.prev.set(pos(f));
        return super.startDrag(f);
    }



}
