package spacegraph.input.finger;

import jcog.math.v2;
import jcog.tree.rtree.Spatialization;

public abstract class FingerMove extends Dragging {

    private final float xSpeed;
    private final float ySpeed;
    private final v2 current = new v2();
    private final v2 start = new v2();

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

        v2 pos = pos(f);
        if (pos !=null) {
            current.set(pos);
            float tx = xSpeed != 0 ? (current.x - start.x) * xSpeed : 0;
            float ty = ySpeed != 0 ? (current.y - start.y) * ySpeed : 0;

            float epsilon = Spatialization.EPSILONf;
            if (Math.abs(tx) < epsilon && Math.abs(ty) < epsilon)
                return false;

            move(tx, ty);
            return true;
        }

        return false;
    }

    @Override
    protected boolean startDrag(Finger f) {
        this.start.set(pos(f));
        return super.startDrag(f);
    }

    public abstract v2 pos(Finger finger);


}
