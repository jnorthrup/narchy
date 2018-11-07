package spacegraph.input.finger;

import jcog.tree.rtree.rect.RectFloat;
import spacegraph.space2d.Surface;

public class FingerSurfaceMove extends FingerMove {
    private final Surface moving;
    private RectFloat before;

    public FingerSurfaceMove(Surface moving) {
        this(moving, true, true);
    }

    private FingerSurfaceMove(Surface moving, boolean xAxis, boolean yAxis) {
        super(0, /* LEFT BUTTON */xAxis, yAxis);
        this.moving = moving;
    }

    @Override
    public boolean drag(Finger f) {
        if (before == null)
            this.before = moving.bounds;

        return super.drag(f);
    }

    @Override
    public void move(float tx, float ty) {
        tx += before.x;
        ty += before.y;
        moving.pos(tx, ty, moving.w() + tx, moving.h() + ty);
    }


}
