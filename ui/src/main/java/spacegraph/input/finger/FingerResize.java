package spacegraph.input.finger;

import jcog.tree.rtree.rect.RectFloat2D;
import spacegraph.space2d.widget.windo.Windo;
import spacegraph.util.math.v2;

public abstract class FingerResize extends FingerDragging {
    protected final static float aspectRatioRatioLimit = 0.1f;
    protected final Windo.DragEdit mode;
    private RectFloat2D before;

    private v2 posStart;

    public FingerResize(int button, Windo.DragEdit mode) {
        super(button);
        this.mode = mode;


    }

    @Override
    public boolean start(Finger f) {
        if (super.start(f)) {
            this.posStart = pos(f);
            this.before = size();

            return true;
        }

        return false;
    }

    @Override
    public boolean drag(Finger finger) {

        if (before == null)
            return true; //not init'd yet

        v2 pos = this.pos(finger);
        float fx = pos.x;
        float fy = pos.y;

        switch (mode) {
            case RESIZE_S: {
                float bh = before.h;
                float bottom = before.bottom();
                float ty = (fy - posStart.y);
                resize(
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
                float ty = (fy - posStart.y);
                resize(
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
                float tx = (fx - posStart.x);
                float ty = (fy - posStart.y);
                resize(
                        pmx,
                        pmy,
                        Math.max(pmx + aspectRatioRatioLimit * bw, bw + pmx + tx),
                        Math.max(pmy + aspectRatioRatioLimit * bh, bh + pmy + ty));
            }
            break;

            case RESIZE_E: {
                float pmx = before.left();
                float bw = before.w;
                float tx = (fx - posStart.x);
                resize(
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
                float tx = (fx - posStart.x);
                float ty = (fy - posStart.y);
                resize(pmx - bw + tx, pmy - bh + ty, pmx, pmy); //TODO limit aspect ratio change
            }
            break;

            //TODO the other directions

            default:
                return false;
        }

        return true;
    }

    abstract protected v2 pos(Finger finger);

    /** current size */
    protected abstract RectFloat2D size();

    protected abstract void resize(float x1, float y1, float x2, float y2);
}
