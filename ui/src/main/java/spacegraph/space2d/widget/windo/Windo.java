package spacegraph.space2d.widget.windo;

import com.jogamp.opengl.GL2;
import jcog.math.v2;
import jcog.tree.rtree.rect.RectFloat;
import org.jetbrains.annotations.Nullable;
import spacegraph.input.finger.Finger;
import spacegraph.input.finger.state.Dragging;
import spacegraph.input.finger.state.FingerMoveSurface;
import spacegraph.input.finger.state.FingerResize;
import spacegraph.input.finger.state.FingerResizeSurface;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.ContainerSurface;
import spacegraph.space2d.container.graph.GraphEdit2D;
import spacegraph.space2d.container.unit.MutableUnitContainer;
import spacegraph.space2d.hud.Zoomed;
import spacegraph.space2d.widget.windo.util.DragEdit;
import spacegraph.video.Draw;

import static spacegraph.space2d.widget.windo.util.DragEdit.MOVE;

/**
 * draggable panel
 */
public class Windo extends MutableUnitContainer {

    private static final float resizeBorder = 0.1f;
    public static final short DRAG_BUTTON = Zoomed.PAN_BUTTON;
    public Dragging dragMode = null;
    public DragEdit potentialDragMode = null;


    private boolean fixed = false;
    private final FingerResize resize = new FingerResizeSurface(this, DRAG_BUTTON);
    private final Dragging move = new FingerMoveSurface(this, DRAG_BUTTON);
    private v2 _posGlobal = null;

    public Windo() {
        super();
    }

    public Windo(Surface content) {
        super(content);
    }

    @Override
    public Surface finger(Finger finger) {


        boolean canDrag = finger.pressed(DRAG_BUTTON);

        Dragging current = this.dragMode;
        boolean unDrag = (current != null && !current.active());

        Surface other = super.finger(finger);

        if ((other != null && other!=this.the() && other != this) || fixed()) {
            unDrag = true;
            canDrag = false;
        }

        if (unDrag) {
            if (current != null) {
                current.stop(finger);
                this.dragMode = null;
            }
            //finger.tryFingering(CursorOverlay.Reset);
        }

        if (!canDrag)
            potentialDragMode = null;

        if (canDrag && drag(finger))
            return this;
        else
            return other;

    }

    private boolean drag(Finger finger) {

        _posGlobal = finger.posGlobal();

        Dragging actualDragMode = null;
        DragEdit potentialDragMode = DragEdit.mode(finger.posRelative(this), resizeBorder);

        if (potentialDragMode != null) {
            Dragging d = fingering(potentialDragMode);
            if (d!=null) {
                if (d == resize) resize.mode(potentialDragMode);
                if (finger.test(d)) {
                    actualDragMode = d;
                }
            }
        }

        this.potentialDragMode = potentialDragMode;
        this.dragMode = actualDragMode;

        return dragMode != null;
    }

    private @Nullable Dragging fingering(DragEdit mode) {
        if (mode == null)
            return null;
        else
            return mode == MOVE ? this.move : this.resize.mode(mode);
    }

    /**
     * alllows filtering of certain finger modes
     */
    static boolean fingerable(DragEdit d) {
        return d != null; //HACK
    }

    private void postpaint(GL2 gl) {

        DragEdit p = potentialDragMode;
        if (p != null && _posGlobal != null) {

            float pmx = _posGlobal.x, pmy = _posGlobal.y;
            gl.glPushMatrix();

            float H = 0.5f;
            float W = 0.5f;
            float resizeBorder = Math.max(W, H) * Windo.resizeBorder;
            switch (p) {
                case RESIZE_N:
                    colorDragIndicator(gl);
                    Draw.quad2d(gl, pmx, pmy, W / 2, H - resizeBorder,
                            W / 2 + resizeBorder / 2, H,
                            W / 2 - resizeBorder / 2, H);
                    break;
                case RESIZE_S:
                    colorDragIndicator(gl);
                    Draw.quad2d(gl, pmx, pmy, W / 2, resizeBorder,
                            W / 2 + resizeBorder / 2, 0,
                            W / 2 - resizeBorder / 2, 0);
                    break;
                case RESIZE_E:
                    colorDragIndicator(gl);
                    Draw.quad2d(gl, pmx, pmy, W - resizeBorder, H / 2,
                            W, H / 2 + resizeBorder / 2,
                            W, H / 2 - resizeBorder / 2);
                    break;
                case RESIZE_W:
                    colorDragIndicator(gl);
                    Draw.quad2d(gl, pmx, pmy, resizeBorder, H / 2,
                            0, H / 2 + resizeBorder / 2,
                            0, H / 2 - resizeBorder / 2);
                    break;
                case RESIZE_NE:
                    colorDragIndicator(gl);
                    Draw.quad2d(gl, pmx, pmy, W, H - resizeBorder, W, H, W - resizeBorder, H);
                    break;
                case RESIZE_SE:
                    colorDragIndicator(gl);
                    Draw.quad2d(gl, pmx, pmy, W, W - resizeBorder, W, 0, W - resizeBorder, 0);
                    break;
                case RESIZE_SW:
                    colorDragIndicator(gl);
                    Draw.quad2d(gl, pmx, pmy, 0, resizeBorder, 0, 0, resizeBorder, 0);
                    break;
            }
            gl.glPopMatrix();
        }

    }

    private void colorDragIndicator(GL2 gl) {
        if (dragMode != null) {
            gl.glColor4f(0.75f, 1f, 0f, 0.75f);
        } else {
            gl.glColor4f(1f, 0.75f, 0f, 0.5f);
        }
    }

    @Override
    protected void paintIt(GL2 gl, ReSurface r) {
        //default opaque bg
        gl.glColor4f(0.2f, 0.2f, 0.2f, 0.75f);
        Draw.rect(bounds, gl);
    }

    @Override
    protected void renderContent(ReSurface r) {
        super.renderContent(r);
        r.on(this::postpaint);
    }


    /**
     * position relative to parent
     * 0  .. (0.5,0.5) center ... +1
     */
    public final ContainerSurface posRel(float cx, float cy, float pct) {
        return posRel(cx, cy, pct, pct);
    }

    public Windo sizeRel(float pctX, float pctY) {
//        EditGraph2D p = parentOrSelf(EditGraph2D.class);
//        return p != null ? (Windo) resize(p.w() * pctX, p.h() * pctY) : null;
        return posRel(0, 0, pctX, pctY);
    }

    public Windo posRel(float cx, float cy, float pctX, float pctY) {
        GraphEdit2D p = parentOrSelf(GraphEdit2D.class);
        return p != null ? posRel(p, cx, cy, pctX, pctY) : null;
    }

    public Windo posRel(Surface s, float cx, float cy, float pctX, float pctY) {
        return posRel(s.bounds, cx, cy, pctX, pctY);
    }

    public Windo posRel(RectFloat bounds, float cx, float cy, float pctX, float pctY) {
        pos(bounds.rel(cx, cy, pctX, pctY));
        return this;
    }

    @Override
    protected final boolean posChanged(RectFloat next) {
        return !fixed && super.posChanged(next);
    }

    public final Windo fixed(boolean b) {
        this.fixed = b;
        return this;
    }

    /**
     * whether this window is not able to move
     */
    public final boolean fixed() {
        return fixed;
    }
}
