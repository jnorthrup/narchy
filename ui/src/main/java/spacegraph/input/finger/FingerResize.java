package spacegraph.input.finger;

import jcog.math.v2;
import jcog.tree.rtree.rect.RectFloat;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.widget.windo.util.DragEdit;

public abstract class FingerResize extends Dragging {
    private final static float aspectRatioRatioLimit = 0.1f;
    private final boolean invY;
    private RectFloat before;

    private final v2 posStart = new v2();

    private DragEdit mode = DragEdit.MOVE; //null

    FingerResize(int button) {
        this(button, false);
    }

    FingerResize(int button, boolean invertY) {
        super(button);
        this.invY = invertY;
    }

    @Override
    protected boolean ready(Finger f) {
        //if (f.pressed(button)) { //HACK prefilter
            this.posStart.set(pos(f));
            this.before = size();
            return super.ready(f);
//        } else {
//            return false;
//        }
    }

    public abstract DragEdit mode(Finger finger);

    @Override
    public boolean drag(Finger finger) {

        v2 pos = this.pos(finger);
        float fx = pos.x;
        float fy = pos.y;

        DragEdit m = mode(finger);
        if (m!=null) {
            switch (m) {
                case RESIZE_S: {
                    float pmy = before.top();
                    float bh = before.h;
                    float ty = (fy - posStart.y);
                    resize(before.left(), pmy - bh + ty, before.right(), pmy);
                    break;
                }

                case RESIZE_SW: {
                    float pmx = before.right();
                    float pmy = before.top();
                    float bw = before.w;
                    float bh = before.h;
                    float tx = (fx - posStart.x);
                    float ty = (fy - posStart.y);
                    resize(pmx - bw + tx, pmy - bh + ty, pmx, pmy);
                    break;
                }

                case RESIZE_NE: {
                    float pmx = before.left();
                    float pmy = before.bottom();
                    float bw = before.w;
                    float bh = before.h;
                    float tx = (fx - posStart.x);
                    float ty = (fy - posStart.y);
                    resize(pmx, pmy,
                            Math.max(pmx + aspectRatioRatioLimit * bw, bw + pmx + tx),
                            Math.max(pmy + aspectRatioRatioLimit * bh, bh + pmy + ty));
                    break;
                }
                case RESIZE_SE: {
                    float pmx = before.left();
                    float pmy = before.top();
                    float bw = before.w;
                    float bh = before.h;
                    float tx = (fx - posStart.x);
                    float ty = (fy - posStart.y);
                    resize(pmx, pmy - bh + ty, Math.max(pmx + aspectRatioRatioLimit * bw, bw + pmx + tx), pmy);
                    break;
                }
                case RESIZE_N: {
                    float top, bottom;
                    float bh = before.h;
                    float ty = (fy - posStart.y);
                    if (!invY) {
                        top = before.bottom();
                        bottom = Math.max(top + aspectRatioRatioLimit * bh, top + bh + ty);
                    } else {
                        bottom = before.top();
                        top = Math.min(bottom - aspectRatioRatioLimit * bh, bottom - bh - ty);
                    }
                    resize(
                            before.left(),
                            top,
                            before.right(),
                            bottom
                    );
                    break;
                }

                case RESIZE_NW: {
                    float pmx = before.right();
                    float pmy = before.bottom();
                    float bw = before.w;
                    float bh = before.h;
                    float tx = (fx - posStart.x);
                    float ty = (fy - posStart.y);
                    resize(pmx - bw + tx, pmy,
                            pmx,
                            Math.max(pmy + aspectRatioRatioLimit * bh, bh + pmy + ty));
                    break;
                }


                case RESIZE_E: {
                    float pmx = before.left();
                    float bw = before.w;
                    float tx = (fx - posStart.x);
                    resize(pmx, before.bottom(),
                            pmx + Math.max(aspectRatioRatioLimit * bw, bw + tx), before.top());
                    break;
                }
                case RESIZE_W: {
                    float pmx = before.right();
                    float bw = before.w;
                    float tx = (posStart.x - fx);
                    resize(pmx - Math.max(aspectRatioRatioLimit * bw, bw + tx), before.bottom(),
                            pmx, before.top());
                    break;
                }
            }
            return true;
        }

        return false;
    }

    abstract protected v2 pos(Finger finger);

    /** current size */
    protected abstract RectFloat size();

    protected abstract void resize(float x1, float y1, float x2, float y2);

    @Override
    public @Nullable FingerRenderer renderer(Finger finger) {
        DragEdit mode = mode(finger);
        return mode != null ? mode.cursor() : null;
    }

    @Override
    public void stop(Finger finger) {
        super.stop(finger);
        finger.renderer = finger.rendererDefault;
        this.mode = null;
    }

    @Nullable public FingerResize mode(DragEdit mode) {
        if (null == (this.mode = mode))
            return null;
        else
            return this;
    }
}
