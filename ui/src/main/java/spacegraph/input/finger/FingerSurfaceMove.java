package spacegraph.input.finger;

import jcog.tree.rtree.rect.RectFloat2D;
import spacegraph.space2d.Surface;

public class FingerSurfaceMove extends FingerMove {
    private final Surface moving;
    private RectFloat2D before;

    public FingerSurfaceMove(Surface moving) {
        this(moving, true, true);
    }

    public FingerSurfaceMove(Surface moving, boolean xAxis, boolean yAxis) {
        super(0, /* LEFT BUTTON */xAxis, yAxis);
        this.moving = moving;
    }

    @Override
    public boolean drag(Finger finger) {
        if (before == null)
            this.before = moving.bounds;

        return super.drag(finger);
    }

    @Override
    public float xStart() {
        return before.x;
    }

    @Override
    public float yStart() {
        return before.y;
    }

    @Override
    public void move(float tx, float ty) {
        moved(tx, ty, moving.w() + tx, moving.h() + ty);
    }

    protected void moved(float x1, float y1, float x2, float y2) {
        moving.pos(x1, y1, x2, y2);
    }


}
