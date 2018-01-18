package spacegraph.input;

import jcog.tree.rtree.rect.RectFloat2D;
import spacegraph.Surface;
import spacegraph.math.v2;
import spacegraph.widget.windo.Windo;

/**
 * resizes a rectangular surface in one of the four cardinal or four diagonal directions
 */
public class FingerResize extends FingerDragging {

    protected final static float aspectRatioRatioLimit = 0.1f;

    private final Surface resized;
    private final Windo.DragEdit mode;

    private RectFloat2D before;

    public FingerResize(Surface target, Windo.DragEdit mode) {
        super(0);
        this.resized = target;
        this.mode = mode;
    }

    @Override
    public void start(Finger f) {
        this.before = resized.bounds;
    }

    @Override
    public boolean drag(Finger finger) {

        float fx = finger.hit.x;
        float fy = finger.hit.y;

        v2 hitOnDown = finger.hitOnDown[button];

        switch (mode) {

            case RESIZE_N: {
                float pmy = before.top();
                float bh = before.h;
                float ty = (fy - hitOnDown.y);
                resized.pos(before.left(), pmy,
                        before.bottom(), Math.max(pmy + aspectRatioRatioLimit * bh, bh + pmy + ty));
            }
            break;
            case RESIZE_E: {
                float pmx = before.left();
                float bw = before.w;
                float tx = (fx - hitOnDown.x);
                resized.pos(pmx, before.top(),
                        Math.max(pmx + aspectRatioRatioLimit * bw, bw + pmx + tx), before.bottom());
            }
            break;
//            case RESIZE_S: {
//                //TODO not right yet
//                float pmy = before.top();
//                float bh = before.h();
//                float ty = (fy - hitOnDown.y);
//                resized.pos(before.left(), before.max.y - bh + ty*2,
//                        before.max.x, Math.max(pmy + aspectRatioRatioLimit * bh, bh + pmy - ty));
//            }
//            break;

            case RESIZE_NE: {
                float pmx = before.left();
                float pmy = before.top();
                float bw = before.w;
                float bh = before.h;
                float tx = (fx - hitOnDown.x);
                float ty = (fy - hitOnDown.y);
                resized.pos(pmx, pmy, Math.max(pmx + aspectRatioRatioLimit * bw, bw + pmx + tx), Math.max(pmy + aspectRatioRatioLimit * bh, bh + pmy + ty));
            }
            break;

            case RESIZE_SW: {
                float pmx = before.right();
                float pmy = before.bottom();
                float bw = before.w;
                float bh = before.h;
                float tx = (fx - hitOnDown.x);
                float ty = (fy - hitOnDown.y);
                resized.pos(pmx - bw + tx, pmy - bh + ty, pmx, pmy); //TODO limit aspect ratio change
            }
            break;

            //TODO the other directions
        }

        return true;
    }

}
