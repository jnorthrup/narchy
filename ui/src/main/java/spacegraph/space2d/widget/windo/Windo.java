package spacegraph.space2d.widget.windo;

import com.jogamp.opengl.GL2;
import org.jetbrains.annotations.Nullable;
import spacegraph.input.finger.*;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRender;
import spacegraph.space2d.container.unit.MutableUnitContainer;
import spacegraph.space2d.hud.Ortho;
import spacegraph.space2d.hud.ZoomOrtho;
import jcog.math.v2;
import spacegraph.video.Draw;

import java.util.EnumMap;

import static spacegraph.space2d.widget.windo.Windo.DragEdit.MOVE;

/**
 * draggable panel
 */
public class Windo extends MutableUnitContainer {

    private final static float resizeBorder = 0.1f;
    public FingerDragging dragMode = null;
    public DragEdit potentialDragMode = null;


    protected Windo() {
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
        }
        else if (dragMode != null && dragMode.isStopped()) {
            dragMode = null;
        }


        Surface other = null;
        if (/*dragMode==null && */finger != null) {
            Surface c = super.finger(finger);
            other = c;


        }


        if (other != null && other != this) {
            this.dragMode = null;
            this.potentialDragMode = null;
            return other;
        } else if (finger == null || !fingeringBounds(finger)) {


            this.dragMode = null;
            this.potentialDragMode = null;
            return null;
        } else {

            DragEdit potentialDragMode = null;


            v2 hitPoint = windowHitPointRel(finger);



            {

                if (hitPoint.x >= 0.5f - resizeBorder / 2f && hitPoint.x <= 0.5f + resizeBorder / 2) {
                    if (hitPoint.y <= resizeBorder) {
                        potentialDragMode = DragEdit.RESIZE_S;
                    }
                    if (potentialDragMode == null && hitPoint.y >= 1f - resizeBorder) {
                        potentialDragMode = DragEdit.RESIZE_N;
                    }
                }

                if (potentialDragMode == null && hitPoint.y >= 0.5f - resizeBorder / 2f && hitPoint.y <= 0.5f + resizeBorder / 2) {
                    if (hitPoint.x <= resizeBorder) {
                        potentialDragMode = DragEdit.RESIZE_W;
                    }
                    if (potentialDragMode == null && hitPoint.x >= 1f - resizeBorder) {
                        potentialDragMode = DragEdit.RESIZE_E;
                    }
                }

                if (potentialDragMode == null && hitPoint.x <= resizeBorder) {
                    if (hitPoint.y <= resizeBorder) {
                        potentialDragMode = DragEdit.RESIZE_SW;
                    }
                    if (potentialDragMode == null && hitPoint.y >= 1f - resizeBorder) {
                        potentialDragMode = DragEdit.RESIZE_NW;
                    }
                }

                if (potentialDragMode == null && hitPoint.x >= 1f - resizeBorder) {

                    if (hitPoint.y <= resizeBorder) {
                        potentialDragMode = DragEdit.RESIZE_SE;
                    }
                    if (potentialDragMode == null && hitPoint.y >= 1f - resizeBorder) {
                        potentialDragMode = DragEdit.RESIZE_NE;
                    }
                }


                if (!fingerable(potentialDragMode))
                    potentialDragMode = null;

                if (potentialDragMode == null) {
                    if (fingerable(MOVE))
                        potentialDragMode = MOVE;
                }
            }


            this.potentialDragMode = potentialDragMode;

            if (finger.pressing(ZoomOrtho.PAN_BUTTON)) {

                FingerDragging d = potentialDragMode != null ? (FingerDragging) fingering(potentialDragMode) : null;
                if (d != null && finger.tryFingering(d)) {
                    this.dragMode = d;
                    return null;
                } else {
                    this.dragMode = null;
                }
            }

            if (potentialDragMode!=null) {
                RenderWhileHovering h = potentialDragMode.hover();
                if (h!=null)
                    finger.tryFingering(h);
            } else {
                finger.tryFingering(RenderWhileHovering.Reset);
            }

            return null;
        }


    }

    protected boolean fingeringBounds(Finger finger) {
        v2 f = finger.posOrtho;
        return bounds.contains(f.x, f.y);
    }

    protected v2 windowHitPointRel(Finger finger) {
        return finger.relativePos(this);
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
        return new FingerSurfaceMove(this);
    }

    /** alllows filtering of certain finger modes */
    boolean fingerable(DragEdit d) {
        return true;
    }

    @Deprecated protected boolean opaque() {
        return true;
    }


    protected void postpaint(GL2 gl) {

        DragEdit p = potentialDragMode;
        if (p != null && p != DragEdit.MOVE) {

            Ortho root = (Ortho) root();
            if (root == null) {
                return;
            }

            float W, H;


            gl.glPushMatrix();


            float pmx, pmy;
//            if (this instanceof ZoomOrtho.HUD) {
//                W = w();
//                H = h();
//                v2 mousePos = root.finger.posPixel;
//                pmx = mousePos.x;
//                pmy = mousePos.y;
//            } else {
                W = H = 0.5f;
                v2 mousePos = root.fingerPos;
                pmx = mousePos.x;
                pmy = mousePos.y;


//            }


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
                case RESIZE_NE:
                    colorDragIndicator(gl);
                    Draw.quad2d(gl, pmx, pmy, W, H - resizeBorder, W, H, W - resizeBorder, H);
                    break;
                case RESIZE_SE:
                    colorDragIndicator(gl);
                    Draw.quad2d(gl, pmx, pmy, W, resizeBorder, W, 0, W - resizeBorder, 0);
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
    protected void paintIt(GL2 gl, SurfaceRender r) {
        paintBack(gl);


        postpaint(gl);


    }

    private void paintBack(GL2 gl) {
        if (opaque()) {
            //default
            gl.glColor4f(0.25f, 0.25f, 0.25f, 0.75f);
            Draw.rect(bounds, gl);
        }
    }

    public enum DragEdit {
        MOVE,
        RESIZE_N, RESIZE_E, RESIZE_S, RESIZE_W,
        RESIZE_NW,
        RESIZE_SW,
        RESIZE_NE,
        RESIZE_SE;

        static final EnumMap<DragEdit,FingerRenderer> cursor = new EnumMap(DragEdit.class);
        static final EnumMap<DragEdit,RenderWhileHovering> hover = new EnumMap(DragEdit.class);

        static {

            cursor.put(DragEdit.RESIZE_NE, new FingerRenderer.PolygonWithArrow(45f));
            cursor.put(DragEdit.RESIZE_SW, new FingerRenderer.PolygonWithArrow(45+180));
            cursor.put(DragEdit.RESIZE_SE, new FingerRenderer.PolygonWithArrow(-45));
            cursor.put(DragEdit.RESIZE_NW, new FingerRenderer.PolygonWithArrow(45+90));
            cursor.put(DragEdit.RESIZE_N, new FingerRenderer.PolygonWithArrow(90));
            cursor.put(DragEdit.RESIZE_S, new FingerRenderer.PolygonWithArrow(-90));
            cursor.put(DragEdit.RESIZE_E, new FingerRenderer.PolygonWithArrow(0));
            cursor.put(DragEdit.RESIZE_W, new FingerRenderer.PolygonWithArrow(180));
            //cursor.put(DragEdit.MOVE, new FingerRenderer.PolygonCrosshairs().angle(45)); //TODO something special

            cursor.forEach((k,v)-> {
                hover.put(k, new RenderWhileHovering(v) {
//                    @Override
//                    protected boolean update(Finger f) {
//                        return (f.touching() instanceof Windo);
//                    }
                });
            });
        }
        @Nullable public FingerRenderer cursor() {
            return cursor.get(this);
        }

        @Nullable
        public RenderWhileHovering hover() {
            return hover.get(this);
        }
    }



}
