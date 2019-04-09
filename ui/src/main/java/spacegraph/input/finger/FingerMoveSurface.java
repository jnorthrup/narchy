package spacegraph.input.finger;

import jcog.math.v2;
import jcog.tree.rtree.rect.RectFloat;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.Surface;

public class FingerMoveSurface extends FingerMove {

    /**
     * what is being moved
     */
    private final Surface moving;

    /**
     * bounds of moved surface, captured at drag start
     */
    @Nullable
    private RectFloat before;

    public FingerMoveSurface(Surface moving) {
        this(moving, 0 /* LEFT BUTTON */, true, true);
    }

    private FingerMoveSurface(Surface moving, int button, boolean xAxis, boolean yAxis) {
        super(button, xAxis, yAxis);
        this.moving = moving;
    }

    //    @Override
//    public boolean drag(Finger f) {
//        if (super.drag(f)) {
//            if (before == null)
//                this.before = moving.bounds;
//            return true;
//        } else {
//            this.before = null;
//            return false;
//        }
//    }
    @Override
    public boolean drag(Finger f) {
        if (before == null)
            this.before = moving.bounds;

        return super.drag(f);
    }

    @Override
    public void stop(Finger finger) {
        super.stop(finger);
        before = null;
    }

    @Override
    public v2 pos(Finger finger) {
        return finger.posGlobal().clone();
    }

    @Override
    public void move(float tx, float ty) {
//        @Nullable RectFloat before = this.before;
////        if (before!=null) {
//            tx += before.x;
//            ty += before.y;
////        }
        moving.pos(RectFloat.X0Y0WH(tx + before.x, ty + before.y, moving.w(), moving.h()));
        //moving.pos(RectFloat.XYWH(tx + before.x, ty + before.y, moving.w() + tx, moving.h() + ty));
        //moving.pos(tx + before.x, ty + before.y);
    }

    @Override
    public Surface touchNext(Surface prev, Surface next) {
        return moving;
    }
}
