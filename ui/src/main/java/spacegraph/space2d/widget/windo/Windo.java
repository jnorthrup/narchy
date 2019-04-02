package spacegraph.space2d.widget.windo;

import com.jogamp.opengl.GL2;
import jcog.math.v2;
import jcog.tree.rtree.rect.RectFloat;
import org.jetbrains.annotations.Nullable;
import spacegraph.input.finger.*;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Container;
import spacegraph.space2d.container.graph.EditGraph2D;
import spacegraph.space2d.container.unit.MutableUnitContainer;
import spacegraph.space2d.hud.ZoomOrtho;
import spacegraph.space2d.widget.windo.util.DragEdit;
import spacegraph.video.Draw;

import static spacegraph.space2d.widget.windo.util.DragEdit.MOVE;

/**
 * draggable panel
 */
public class Windo extends MutableUnitContainer {

    private final static float resizeBorder = 0.1f;
    public Dragging dragMode = null;
    public DragEdit potentialDragMode = null;


    private boolean fixed = false;

    public Windo() {
        super();
    }

    public Windo(Surface content) {
        super(content);
    }

    @Override
    public Surface finger(Finger finger) {


        if (finger == null) {
            dragMode = null;
            potentialDragMode = null;
        } else if (dragMode != null && dragMode.isStopped()) {
            dragMode = null;
        }


        Surface other = null;
        if (/*dragMode==null && */finger != null) {
            other = super.finger(finger);
        }


        if (other != null && other != this) {
            unfinger(finger);
            return other;
        } else if (finger == null || !fingeringRelative(finger).inUnit()) {


            unfinger(finger);
            return null;
        } else {

            if (!fixed())
                return drag(finger);
            else
                return null;
        }


    }

    @Nullable
    private Surface drag(Finger finger) {


        DragEdit potentialDragMode = DragEdit.edge(fingeringRelative(finger), resizeBorder);


        if (!fingerable(potentialDragMode))
            potentialDragMode = null;

        if (potentialDragMode == null) {
            if (fingerable(MOVE))
                potentialDragMode = MOVE;
        }


        this.potentialDragMode = potentialDragMode;


        if (finger.pressing(ZoomOrtho.PAN_BUTTON)) {
            Dragging d =
                    potentialDragMode != null ? (Dragging) fingering(potentialDragMode) : null;

            if (d != null && finger.tryFingering(d)) {
                this.dragMode = d;
                return null;
            } else {
                this.dragMode = null;
            }
        }

        if (potentialDragMode != null) {
            RenderWhileHovering h = potentialDragMode.hover();
            if (h != null)
                finger.tryFingering(h);
        } else {
            finger.tryFingering(RenderWhileHovering.Reset);
        }

        return null;
    }

    public void unfinger(Finger finger) {
        this.dragMode = null;
        this.potentialDragMode = null;
        finger.tryFingering(RenderWhileHovering.Reset);
    }



    protected v2 fingeringRelative(Finger finger) {
        return finger.posRelative(this);
    }


    private Fingering fingering(DragEdit mode) {

        switch (mode) {
            case MOVE:
                return fingeringMove();

            default:
                return fingeringResize(mode);
        }

    }

    protected FingerResize fingeringResize(DragEdit mode) {
        return new FingerResizeSurface(this, mode);
    }

    protected Fingering fingeringMove() {
        return new FingerMoveSurface(this);
    }

    /**
     * alllows filtering of certain finger modes
     */
    boolean fingerable(DragEdit d) {
        return true;
    }

    @Deprecated
    protected boolean opaque() {
        return true;
    }


    private transient v2 posGlobal = null;

    @Override
    public void fingerTouch(Finger finger, boolean touching) {
//        Ortho root = (Ortho) root();
//        if (root == null)
//            return;

//        v2 mousePos = root.fingerPos;
//        float pmx = mousePos.x, pmy = mousePos.y;

        posGlobal = //finger.posGlobal(this);
                    finger.posPixel;
    }

    protected void postpaint(GL2 gl) {

        DragEdit p = potentialDragMode;
        if (posGlobal!=null && p != null && p != DragEdit.MOVE) {

            float pmx = posGlobal.x, pmy = posGlobal.y;
            float W = 0.5f,H = 0.5f;
            gl.glPushMatrix();

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
        if (opaque()) {
            //default
            gl.glColor4f(0.25f, 0.25f, 0.25f, 0.75f);
            Draw.rect(bounds, gl);
        }
    }

    @Override
    protected void compileChildren(ReSurface r) {
        super.compileChildren(r);
        r.on(this::postpaint);
    }


    /**
     * position relative to parent
     * 0  .. (0.5,0.5) center ... +1
     */
    public final Container posRel(float cx, float cy, float pct) {
        return posRel(cx, cy, pct, pct);
    }

    public Windo sizeRel(float pctX, float pctY) {
        EditGraph2D p = parent(EditGraph2D.class);
        return p!=null ? (Windo) size(p.w() * pctX, p.h() * pctY) : null;
    }

    public Windo posRel(float cx, float cy, float pctX, float pctY) {
        EditGraph2D p = parent(EditGraph2D.class);
        return p!=null ? posRel(p, cx, cy, pctX, pctY) : null;
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

    /** whether this window is not able to move */
    public final boolean fixed() {
        return fixed;
    }
}
