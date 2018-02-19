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
    public boolean drag(Finger finger) {

        v2 hitOnDown = finger.hitOnDown[button];
        if (hitOnDown == null)
            return false; //unknown hit

        if (before == null)
            this.before = resized.bounds;

        float fx = finger.pos.x;
        float fy = finger.pos.y;

        switch (mode) {
            case RESIZE_S: {
                //TODO not right yet
                float bh = before.h;
                float bottom = before.bottom();
                float ty = (fy - hitOnDown.y);
                resized.pos(
                        before.left(),
                        Math.min(bottom - aspectRatioRatioLimit * bh, bottom - bh + ty),
                        before.right(),
                        bottom
                );
            }
            break;
            case RESIZE_N: {
                float top = before.top();
                float bh = before.h;
                float ty = (fy - hitOnDown.y);
                resized.pos(
                        before.left(),
                        top,
                        before.right(),
                        Math.max(top + aspectRatioRatioLimit * bh, top + bh + ty)
                );
            }
            break;


            case RESIZE_NE: {
                float pmx = before.left();
                float pmy = before.top();
                float bw = before.w;
                float bh = before.h;
                float tx = (fx - hitOnDown.x);
                float ty = (fy - hitOnDown.y);
                resized.pos(
                        pmx,
                        pmy,
                        Math.max(pmx + aspectRatioRatioLimit * bw, bw + pmx + tx),
                        Math.max(pmy + aspectRatioRatioLimit * bh, bh + pmy + ty));
            }
            break;

            case RESIZE_E: {
                float pmx = before.left();
                float bw = before.w;
                float tx = (fx - hitOnDown.x);
                resized.pos(
                        pmx,
                        before.top(),
                        Math.max(pmx + aspectRatioRatioLimit * bw, bw + pmx + tx),
                        before.bottom());
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

            default:
                return false;
        }

        return true;
    }

}
