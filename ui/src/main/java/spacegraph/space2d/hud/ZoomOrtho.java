package spacegraph.space2d.hud;

import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.opengl.GL2;
import jcog.event.Off;
import spacegraph.input.finger.*;
import spacegraph.space2d.Surface;
import spacegraph.space2d.widget.windo.Windo;
import spacegraph.util.math.v2;
import spacegraph.util.math.v3;
import spacegraph.video.JoglSpace;
import spacegraph.video.JoglWindow;

import java.util.function.Consumer;

/**
 * Ortho with mouse zoom controls
 */
public class ZoomOrtho extends Ortho {

    public final static short PAN_BUTTON = 0;
    private final static short MOVE_WINDOW_BUTTON = 1;
    private final HUD hud = new HUD();
    private final Fingering fingerContentPan = new FingerMovePixels(PAN_BUTTON) {

        float speed = 1f;
        private v3 camStart;

        @Override
        protected JoglSpace window() {
            return window;
        }

        @Override
        protected boolean startDrag(Finger f) {
            camStart = new v3(cam);
            return super.startDrag(f);
        }

        @Override
        public void move(float dx, float dy) {
            cam.set(camStart.x - dx / scale.x * speed,
                    camStart.y - dy / scale.y * speed);
        }

    };
    private final Fingering fingerWindowMove = new FingerMovePixels(MOVE_WINDOW_BUTTON) {


        private final v2 windowStart = new v2();

        @Override
        protected JoglSpace window() {
            return window;
        }

        @Override
        protected boolean startDrag(Finger f) {


            windowStart.set(window.getX(), window.getY());
            //System.out.println("window start=" + windowStart);
            return super.startDrag(f);
        }

        @Override
        protected v2 pos(Finger finger) {

            return finger.posScreen.clone();
        }

        @Override
        public void move(float dx, float dy) {


            window.setPosition(
                    Math.round(windowStartX + dx),
                    Math.round(windowStartY - dy));
        }

    };
    private final Surface content;

    public ZoomOrtho(Surface content) {
        super();

        this.content = content;
        this.surface = hud;


    }

    @Override
    public void start(JoglSpace s) {
        super.start(s);
        hud.start(this);
        hud.add(content);
    }

    @Override
    public boolean autoresize() {
        return true;
    }


    @Override
    public Off onUpdate(Consumer<JoglWindow> c) {
        return super.onUpdate(c);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        hud.dragMode = null;
        super.mouseReleased(e);
    }

    @Override
    protected Surface finger() {



        Surface touchPrev = finger.touching();
        Surface touchNext = super.finger();
        if (touchNext != null && touchNext != touchPrev) {
            debug(this, 1f, () -> "touch(" + touchNext + ')');
        }

        if (touchNext == null) {

            if (!finger.tryFingering(fingerWindowMove)) {
                if (!finger.tryFingering(fingerContentPan)) {
                }
            }
        }

        return touchNext;
    }


    @Override
    public void windowLostFocus(WindowEvent e) {
        hud.potentialDragMode = null;
        super.windowLostFocus(e);
    }

    @Override
    public void mouseExited(MouseEvent e) {
        hud.potentialDragMode = null;
        super.mouseExited(e);
    }

    @Override
    public void mouseWheelMoved(MouseEvent e) {
        if (e.isConsumed())
            return;

        super.mouseWheelMoved(e);


    }


    public class HUD extends Windo {


        {


            clipBounds = false;
        }


        @Override
        protected FingerResize fingeringResize(Windo.DragEdit mode) {
            return new FingerResizeWindow(window, 0, mode);
        }

        @Override
        protected Fingering fingeringMove() {
            return null;
        }

        @Override
        public boolean opaque() {
            return false;
        }


        @Override
        protected void postpaint(GL2 gl) {


            if (ZoomOrtho.this.focused()) {
                gl.glPushMatrix();
                gl.glLoadIdentity();

                super.postpaint(gl);

                finger.drawCrossHair(this, gl);

                gl.glPopMatrix();
            }

        }




        @Override
        public boolean fingeringBounds(Finger finger) {

            return true;
        }

        public v2 windowHitPointRel(Finger finger) {
            v2 v = new v2(finger.posPixel);
            v.x = v.x / w();
            v.y = v.y / h();
            return v;
        }

        @Override
        public float w() {
            return window.getWidthNext();
        }

        @Override
        public float h() {
            return window.getHeightNext();
        }
    }


}



























