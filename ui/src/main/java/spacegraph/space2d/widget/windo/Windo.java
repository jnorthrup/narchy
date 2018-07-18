package spacegraph.space2d.widget.windo;

import com.jogamp.opengl.GL2;
import spacegraph.input.finger.*;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.Stacking;
import spacegraph.space2d.hud.Ortho;
import spacegraph.space2d.hud.ZoomOrtho;
import spacegraph.util.math.v2;
import spacegraph.video.Draw;

import static spacegraph.space2d.widget.windo.Windo.DragEdit.MOVE;

/**
 * draggable panel
 */
public class Windo extends Stacking {

    private final static float resizeBorder = 0.1f;
    public FingerDragging dragMode = null;
    public DragEdit potentialDragMode = null;
    private boolean hover;

    protected Windo() {
        super();




















    }

    public Windo(Surface content) {
        set(content);
    }

    @Override
    public Surface tryTouch(Finger finger) {


        if (dragMode!=null && dragMode.isStopped()) {
            dragMode = null;
        }
        if (finger == null) {
            dragMode = null;
            potentialDragMode = null;
        }

        Surface other = null;
        if (/*dragMode==null && */finger!=null) {
            Surface c = super.tryTouch(finger);
            other = c;





        }




        if (other!=null && other!=this) {
            this.dragMode = null;
            this.potentialDragMode = null;
            this.hover = false;
            return other;
        } else if (finger == null || !fingeringBounds(finger)) {


            this.dragMode = null;
            this.potentialDragMode = null;
            this.hover = false;
            return null;
        } else {

            DragEdit potentialDragMode = null;

            
            v2 hitPoint = windowHitPointRel(finger);

            this.hover = true;

            
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
                
                FingerDragging d = potentialDragMode!=null ? (FingerDragging) fingering(potentialDragMode) : null;
                if (d != null && finger.tryFingering(d)) {
                    
                    this.dragMode = d;
                } else {
                    this.dragMode = null;
                }
            }


            return null;
        }


    }

    protected boolean fingeringBounds(Finger finger) {
        v2 f = finger.pos;
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

    boolean fingerable(DragEdit d) {
        return true;
    }

    protected boolean opaque() {
        return true;
    }


    protected void postpaint(GL2 gl) {

        DragEdit p = potentialDragMode;
        if (p != null && p!=DragEdit.MOVE) {

            Ortho root = (Ortho) root();
            if (root==null) {
                return;
            }

            float W, H;


            gl.glPushMatrix();



            float pmx, pmy;
            if (this instanceof ZoomOrtho.HUD) {
                W = w();
                H = h();
                v2 mousePos = root.finger.posPixel;
                pmx = mousePos.x; pmy = mousePos.y;
            } else {
                W = H = 0.5f;
                v2 mousePos = root.finger.pos;
                pmx = mousePos.x; pmy = mousePos.y;
                
                
                
            }


            float resizeBorder = Math.max(W, H) * Windo.resizeBorder;
            switch (p) {
                case RESIZE_N:
                    colorDragIndicator(gl);
                    Draw.quad2d(gl, pmx, pmy, W/2, H-resizeBorder,
                            W/2+resizeBorder/2, H,
                            W/2-resizeBorder/2, H);
                    break;
                case RESIZE_S:
                    colorDragIndicator(gl);
                    Draw.quad2d(gl, pmx, pmy, W/2, resizeBorder,
                            W/2+resizeBorder/2, 0,
                            W/2-resizeBorder/2, 0);
                    break;
                case RESIZE_E:
                    colorDragIndicator(gl);
                    Draw.quad2d(gl, pmx, pmy, W-resizeBorder, H/2,
                            W,H/2+resizeBorder/2,
                            W,H/2-resizeBorder/2);
                    break;
                case RESIZE_NE:
                    colorDragIndicator(gl);
                    Draw.quad2d(gl, pmx, pmy, W, H-resizeBorder, W, H, W - resizeBorder, H);
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
        if (dragMode!=null) {
            gl.glColor4f(0.75f, 1f, 0f, 0.75f);
        } else {
            gl.glColor4f(1f, 0.75f, 0f, 0.5f);
        }
    }

    @Override
    protected void paintIt(GL2 gl) {
        paintBack(gl);





















        postpaint(gl);


    }

    private void paintBack(GL2 gl) {
        if (opaque()) {
            gl.glColor4f(0.5f,0.5f,0.5f, 0.5f);
            Draw.rect(bounds, gl);
        }
    }

    public enum DragEdit {
        MOVE,
        RESIZE_N, RESIZE_E, RESIZE_S, RESIZE_W,
        RESIZE_NW,
        RESIZE_SW,
        RESIZE_NE,
        RESIZE_SE
    }



































































































































































}
