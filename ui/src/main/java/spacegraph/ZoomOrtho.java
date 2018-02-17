package spacegraph;

import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.opengl.GL2;
import jcog.Util;
import org.jetbrains.annotations.Nullable;
import spacegraph.input.Finger;
import spacegraph.phys.util.AnimVector2f;
import spacegraph.render.Draw;
import spacegraph.widget.windo.Windo;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Ortho with mouse zoom controls
 */
public class ZoomOrtho extends Ortho {

    float zoomRate = 0.2f;
    float pressZoomOutRate = zoomRate;


    final static float minZoom = 0.25f;
    final static float maxZoom = 10f;

    public final static short PAN_BUTTON = 0;
    final static short ZOOM_OUT_TOUCHING_NEGATIVE_SPACE_BUTTON = 2;
    final static short MOVE_WINDOW_BUTTON = 1;

    private final int[] moveTarget = new int[2];
    @Deprecated
//    private final int[] resizeTarget = new int[2];
    private final int[] windowStart = new int[2];
//    private InsetsImmutable windowInsets;

    final HUD hud = new HUD();
    private int pmx, pmy;

    final AtomicBoolean windowMoving = new AtomicBoolean(false);

    private int[] panStart = null;

    private boolean zoomingOut = false;
    private boolean panning;


    public ZoomOrtho(Surface content) {
        super(content);

//        initContent = content;
//        this.surface = hud;


//        this.surface = new Stacking(this.surface, overlay);
//        overlay.children().add(new Widget() {
//
//            @Override
//            protected void paintComponent(GL2 gl) {
//
//                gl.glColor4f(1f, 0f, 1f, 0.3f);
//
//
//                pos(cx(), cy());
//
//                float w = (ZoomOrtho.this.window.getWidth() / ZoomOrtho.this.scale.x);
//                float h = (ZoomOrtho.this.window.getHeight() / ZoomOrtho.this.scale.y);
//                scale(w, h);
//
//                Draw.rect(gl, 0.25f, 0.25f, 0.5f, 0.5f);
//            }
//
//        });
    }

    @Override
    public void windowResized(WindowEvent e) {
        pos(0, 0, window.getWidth(), window.getHeight()); //re-maximize
    }

//    @Override
//    public void setSurface(Surface content) {
//        hud.children(content);
//    }

//    @Override
//    public void start(SpaceGraph s) {
//        super.start(s);
//
//        //window.window.setUndecorated(true);
//    }



//    int windowMinWidth = 64;
//    int windowMinHeight = 64;


    private void moveWindow() {
        try {
            window.window.setPosition(moveTarget[0], moveTarget[1]);
        } finally {
            windowMoving.set(false);
        }
    }

    private void resizeWindow(int x, int y, int w, int h) {
        try {
            //System.out.println(Arrays.toString(moveTarget) + " " + Arrays.toString(resizeTarget));
            window.window.setSize(w, h);
            window.window.setPosition(x, y);
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            windowMoving.set(false);
            surface.layout();
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {

        super.mouseMoved(e);


        int windowWidth = window.getWidth();
        int windowHeight = window.getHeight();
        pmx = e.getX();
        pmy = windowHeight - e.getY();

        if ((pmx < hud.resizeBorder) && (pmy < hud.resizeBorder)) {
            hud.potentialDragMode = Windo.DragEdit.RESIZE_SW; //&& window.isResizable()
        } else if ((pmx > windowWidth - hud.resizeBorder) && (pmy < hud.resizeBorder)) {
            hud.potentialDragMode = Windo.DragEdit.RESIZE_SE;  //&& window.isResizable()
        } else {
            hud.potentialDragMode = Windo.DragEdit.MOVE;
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        hud.dragMode = null;
        panStart = null;
        super.mouseReleased(e);
    }


    @Override
    Surface updateMouse(float sx, float sy, short[] buttonsDown) {
        Surface s = super.updateMouse(sx, sy, buttonsDown);

        if (s == null) {
            updatePan();
        }

        return s;
    }



    protected void updatePan() {
        if (!finger.isFingering()) {

            boolean[] bd = finger.buttonDown; //e.getButtonsDown();

            panning = false;

            if (!zoomingOut && (bd[PAN_BUTTON] || bd[MOVE_WINDOW_BUTTON])) {
                //int mx = e.getX();
                //int my = window.getHeight() - e.getY();
                int mx = Finger.pointer.getX();
                int my = Finger.pointer.getY();
                if (panStart == null && !finger.prevButtonDown[PAN_BUTTON] /* rising edge */) {

                    panStart = new int[2];
                    panStart[0] = mx;
                    panStart[1] = my;

                    if (bd[MOVE_WINDOW_BUTTON]) {
                        //TODO compute this in the EDT on the first invocation
                        //Point p = new Point();

                        windowStart[0] = window.windowX;
                        windowStart[1] = window.windowY;
                        //window.window.getInsets();

                        //TODO
                        //hud.dragMode = hud.potentialDragMode;

                        //System.out.println("window drag mode: " + dragMode);
                    }

                } else if (panStart!=null) {

                    int dx = mx - panStart[0];
                    int dy = my - panStart[1];
                    if (dx == 0 && dy == 0) {

                    } else {

                        if (bd[PAN_BUTTON]) {

                            cam.add(-dx / scale.x, +dy / scale.x);
                            panStart[0] = mx;
                            panStart[1] = my;
                            panning = true;

                        } else if (bd[MOVE_WINDOW_BUTTON]) {

                            //compute even if the window is in progress

                            //TODO
//                        if (hud.dragMode == Windo.WindowDragging.MOVE) {
//
//
//                            if (windowMoving.compareAndSet(false, true)) {
//                                moveTarget[0] = windowStart[0] + dx;
//                                moveTarget[1] = windowStart[1] + dy;
//                                window.window.getScreen().getDisplay().getEDTUtil().invoke(true, this::moveWindow);
//                            }
//
//                        } else if (hud.dragMode == Windo.WindowDragging.RESIZE_SE) {
//
//                            int windowWidth = window.getWidth();
//                            int windowHeight = window.getHeight();
//
//                            windowStart[0] = window.windowX;
//                            windowStart[1] = window.windowY;
//
//                            moveTarget[0] = windowStart[0];
//                            moveTarget[1] = windowStart[1];
//
//
//                            resizeTarget[0] = Math.min(window.window.getScreen().getWidth(), Math.max(windowMinWidth, windowWidth + dx));
//                            resizeTarget[1] = Math.min(window.window.getScreen().getHeight(), Math.max(windowMinHeight, windowHeight + dy));
//
//                            if (windowMoving.compareAndSet(false, true)) {
//
//                                window.window.getScreen().getDisplay().getEDTUtil().invoke(true, () ->
//                                        resizeWindow(windowStart[0], windowStart[1], resizeTarget[0], resizeTarget[1]));
//                                //this::resizeWindow);
//                                if (panStart != null) {
//                                    panStart[0] = mx;
//                                    panStart[1] = my;
//                                }
//                            }
//
//                        }
//
                        }
                    }
                }
            } else {

                panStart = null;
                hud.dragMode = null;

            }

            if (bd[ZOOM_OUT_TOUCHING_NEGATIVE_SPACE_BUTTON] && Math.max(scale.x,scale.y) > minZoom) {
                if (finger.touching==null) {

                    panStart = null;
                    scale.scaled(1f * (1f - pressZoomOutRate));

                    zoomingOut = true;
                    hud.dragMode = null; //HACK TODO properly integrate this with the above event handling
                }

            } else {
                zoomingOut = false;
            }
        }
    }

    @Override
    public void mouseExited(MouseEvent e) {
        super.mouseExited(e);
        hud.potentialDragMode = null;
    }

    @Override
    public void mouseWheelMoved(MouseEvent e) {
        super.mouseWheelMoved(e);

        //when wheel rotated on negative (empty) space, adjust scale
        //if (mouse.touching == null) {
        //System.out.println(Arrays.toString(e.getRotation()) + " " + e.getRotationScale());
        float dWheel = e.getRotation()[1];


        float zoomMult = Util.clamp(1f + -dWheel * zoomRate, 0.5f, 1.5f);

        AnimVector2f s = this.scale;
        float psx = s.targetX();
        float psy = psx;
        float sx = psx * zoomMult;

        sx = Math.max(sx, minZoom);
        sx = Math.min(sx, maxZoom);
        scale.set(sx, sx);

    }

    private class HUD extends Windo {

        float smx, smy;
//        final CurveBag<PLink> notifications = new CurveBag(PriMerge.plus, new ConcurrentHashMap(), new XorShift128PlusRandom(1));

        {
//            notifications.setCapacity(8);
//            notifications.putAsync(new PLink("ready", 0.5f));
            clipTouchBounds = false;
        }


//        @Override
//        public synchronized void start(@Nullable SurfaceBase parent) {
//            super.start(parent);
////            root().onLog(t -> {
////
////                String m;
////                if (t instanceof Object[])
////                    m = Arrays.toString((Object[]) t);
////                else
////                    m = t.toString();
////
//////                notifications.putAsync(new PLink(m, 1f));
//////                notifications.commit();
////            });
//        }

//        final Widget bottomRightMenu = new Widget() {
//
//        };

        @Override
        public boolean opaque() {
            return false;
        }

        @Override
        protected void prepaint(GL2 gl) {

            gl.glPushMatrix();
            gl.glLoadIdentity();

            //            {
//                //world coordinates alignment and scaling indicator
//                gl.glLineWidth(2);
//                gl.glColor3f(0.5f, 0.5f, 0.5f);
//                float cx = wmx;
//                float cy = wmy;
//                Draw.rectStroke(gl, cx + -100, cy + -100, 200, 200);
//                Draw.rectStroke(gl, cx + -200, cy + -200, 400, 400);
//                Draw.rectStroke(gl, cx + -300, cy + -300, 600, 600);
//            }
        }

        @Override
        protected void postpaint(GL2 gl) {
            gl.glLineWidth(8f);

            float ch = 175f; //TODO proportional to ortho height (pixels)
            float cw = 175f; //TODO proportional to ortho width (pixels)

            gl.glColor4f(0.5f, 0.5f, 0.5f, 0.25f);
            Draw.rectStroke(gl, smx - cw / 2f, smy - ch / 2f, cw, ch);

            gl.glColor4f(0.5f, 0.5f, 0.5f, 0.5f);
            Draw.line(gl, smx, smy - ch, smx, smy + ch);
            Draw.line(gl, smx - cw, smy, smx + cw, smy);

            gl.glPopMatrix();
        }


        String str(@Nullable Object x) {
            if (x instanceof Object[])
                return Arrays.toString((Object[]) x);
            else
                return x.toString();
        }

        {
//            set(
//                    overlay
//                    //bottomRightMenu.scale(64,64)
//            );
        }

        {
//            clipTouchBounds = false;
        }


        @Override
        public Surface onTouch(Finger finger, short[] buttons) {


            //System.out.println(hitPoint);
            if (finger != null) {

//                float lmx = finger.hit.x; //hitPoint.x;
//                float lmy = finger.hit.y; //hitPoint.y;


                smx = finger.hitGlobal.x;
                smy = finger.hitGlobal.y;

            }

            Surface x = super.onTouch(finger, buttons);
            if (x == this) {
                return null; //pass-thru
            } else
                return x;
        }

        @Override
        public boolean fingerable() {
            return false; //prevent drag the HUD itself
        }

        @Override
        public float w() {
            return window.getWidth();
        }

        @Override
        public float h() {
            return window.getHeight();
        }
    }

}
//    @Override
//    protected Finger newFinger() {
//        return new DebugFinger(this);
//    }
//
//    class DebugFinger extends Finger {
//
//        final Surface overlay = new Surface() {
//
//            @Override
//            protected void paint(GL2 gl) {
//                super.paint(gl);
//
//                gl.glColor4f(1f,1f, 0f, 0.85f);
//                gl.glLineWidth(3f);
//                Draw.rectStroke(gl, 0,0,10,5);
//            }
//        };
//
//        public DebugFinger(Ortho root) {
//            super(root);
//        }
//
//        protected void start() {
//            //window.add(new Ortho(overlay).maximize());
//        }
//    }
