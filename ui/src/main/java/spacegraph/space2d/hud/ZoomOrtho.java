package spacegraph.space2d.hud;

import com.jogamp.newt.event.MouseEvent;
import com.jogamp.opengl.GL2;
import jcog.Util;
import spacegraph.input.finger.*;
import spacegraph.space2d.Surface;
import spacegraph.space2d.widget.windo.Windo;
import spacegraph.util.math.v2;
import spacegraph.util.math.v3;
import spacegraph.video.Draw;
import spacegraph.video.JoglSpace;

/**
 * Ortho with mouse zoom controls
 */
public class ZoomOrtho extends Ortho {

    private final Surface content;
    float zoomRate = 0.2f;
    float pressZoomOutRate = zoomRate*2;



    public final static short PAN_BUTTON = 0;
    final static short ZOOM_OUT_TOUCHING_NEGATIVE_SPACE_BUTTON = 2;
    final static short MOVE_WINDOW_BUTTON = 1;

    @Deprecated private final int[] windowStart = new int[2];

    final HUD hud = new HUD();




    private int[] panStart = null;

    private boolean zoomingOut = false;
    private boolean panning;


    public ZoomOrtho(Surface content) {
        super();
        this.content = content;
        this.surface = hud;

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
    public void start(JoglSpace s) {
        super.start(s);
        hud.parent = this;
        hud.add(content);
    }

    @Override
    protected boolean maximize() {
        return true;
    }



    @Override
    public void mouseReleased(MouseEvent e) {
        hud.dragMode = null;
        panStart = null;
        super.mouseReleased(e);
    }


    final Fingering fingerContentPan = new FingerMovePixels(PAN_BUTTON) {

        private v3 camStart;
        float speed = 2f;

        @Override
        protected JoglSpace window() {
            return window;
        }

        @Override
        public void startDrag() {
            super.startDrag();
            camStart = new v3(cam);
        }

        @Override
        public void move(float dx, float dy) {
            cam.set(camStart.x - dx / scale.x * speed,
                    camStart.y + dy / scale.x * speed);
        }

    };

    final Fingering fingerWindowMove = new FingerMovePixels(MOVE_WINDOW_BUTTON) {

        @Override
        protected JoglSpace window() {
            return window;
        }

        @Override
        public void move(float dx, float dy) {
            window.setPosition(Math.round(windowStartX + dx), Math.round(windowStartY + dy));
        }
    };


//    final Fingering fingerWindowResize = new FingerResizeWindow(MOVE_WINDOW_BUTTON) {
//
//        @Override
//        protected boolean drag(Finger f) {
//            return false;
//        }
//    };

    @Override
    protected boolean finger(float dt) {

        super.finger(dt);

        if (!finger.isFingering() && finger.touching==null) {
//            if (!finger.tryFingering(fingerWindowResize))
                if (!finger.tryFingering(fingerWindowMove))
                    if (!finger.tryFingering(fingerContentPan)) {

            }
        }

        return true;
    }



    protected void _updatePan() {
        if (!finger.isFingering()) {



            panning = false;

            if (!zoomingOut && (finger.pressing(PAN_BUTTON) || finger.pressing(MOVE_WINDOW_BUTTON))) {
                //int mx = e.getX();
                //int my = window.getHeight() - e.getY();
                int mx = Finger.pointer.getX();
                int my = Finger.pointer.getY();
                if (panStart == null && finger.pressing(PAN_BUTTON) /* rising edge */) {

                    panStart = new int[2];
                    panStart[0] = mx;
                    panStart[1] = my;

                    if (finger.pressing(MOVE_WINDOW_BUTTON)) {
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

                    //TODO use FingerDragging to implement this

                    int dx = mx - panStart[0];
                    int dy = my - panStart[1];
                    if (dx == 0 && dy == 0) {

                    } else {

                        if (finger.pressing(PAN_BUTTON)) {

                            cam.add(-dx / scale.x, +dy / scale.x);
                            panStart[0] = mx;
                            panStart[1] = my;
                            panning = true;

                        } else if (finger.pressing(MOVE_WINDOW_BUTTON)) {

                            //TODO use FingerDragging to implement this

                            if (hud.potentialDragMode == Windo.DragEdit.MOVE) {
                                //if (windowMoving.compareAndSet(false, true)) {
                                window.setPosition(windowStart[0] + dx, windowStart[1] + dy);

                                //}
                            }

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

            if (finger.pressing(ZOOM_OUT_TOUCHING_NEGATIVE_SPACE_BUTTON) && Math.max(scale.x,scale.y) > scaleMin) {
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

        float sx = this.scale.targetX() * zoomMult;
        scale(sx, sx);

    }



    public class HUD extends Windo {

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

        @Override protected FingerResize fingeringResize(Windo.DragEdit mode) {
            return new FingerResizeWindow(window, 0, mode);
        }

        @Override
        public boolean opaque() {
            return false;
        }


//        @Override
//        protected void prepaint(GL2 gl) {
//
//            gl.glPushMatrix();
//            gl.glLoadIdentity();
//
//            //            {
////                //world coordinates alignment and scaling indicator
////                gl.glLineWidth(2);
////                gl.glColor3f(0.5f, 0.5f, 0.5f);
////                float cx = wmx;
////                float cy = wmy;
////                Draw.rectStroke(gl, cx + -100, cy + -100, 200, 200);
////                Draw.rectStroke(gl, cx + -200, cy + -200, 400, 400);
////                Draw.rectStroke(gl, cx + -300, cy + -300, 600, 600);
////            }
//
//            super.prepaint(gl);
//
//        }


        @Override
        protected void postpaint(GL2 gl) {
            gl.glPushMatrix();
            gl.glLoadIdentity();

            super.postpaint(gl);

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


//        String str(@Nullable Object x) {
//            if (x instanceof Object[])
//                return Arrays.toString((Object[]) x);
//            else
//                return x.toString();
//        }



        @Override
        public Surface tryTouch(Finger finger) {


            //System.out.println(hitPoint);
            if (finger != null) {

//                float lmx = finger.hit.x; //hitPoint.x;
//                float lmy = finger.hit.y; //hitPoint.y;


                smx = finger.posGlobal.x;
                smy = finger.posGlobal.y;

            }

            Surface x = super.tryTouch(finger);
            return x;
        }

//        @Override
//        public boolean fingeringWindow(Surface childFingered) {
//            return childFingered!=this && childFingered!=null;
//        }

        @Override
        public boolean fingeringBounds(Finger finger) {
            //TODO if mouse cursor is in window
            return true;
        }

        public v2 windowHitPointRel(Finger finger) {
            v2 v = new v2(finger.posGlobal);
            v.x = v.x/w(); //normalize
            v.y = v.y/h();
            return v;
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
