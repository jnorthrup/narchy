package spacegraph.space2d.hud;

import com.jogamp.newt.event.MouseEvent;
import com.jogamp.opengl.GL2;
import jcog.Util;
import spacegraph.input.finger.*;
import spacegraph.space2d.Surface;
import spacegraph.space2d.widget.windo.Windo;
import spacegraph.util.math.v2;
import spacegraph.util.math.v3;
import spacegraph.video.JoglSpace;

/**
 * Ortho with mouse zoom controls
 */
public class ZoomOrtho extends Ortho {

    private final Surface content;
    float zoomRate = 0.2f;

    public final static short PAN_BUTTON = 0;
    final static short MOVE_WINDOW_BUTTON = 1;

    final HUD hud = new HUD();

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
        super.mouseReleased(e);
    }


    final Fingering fingerContentPan = new FingerMovePixels(PAN_BUTTON) {

        private v3 camStart;
        float speed = 1f;

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
                    camStart.y + dy / scale.y * speed);
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
    protected void finger() {

        super.finger();

        if (!finger.isFingering() && finger.touching==null) {
//            if (!finger.tryFingering(fingerWindowResize))
                if (!finger.tryFingering(fingerWindowMove))
                    if (!finger.tryFingering(fingerContentPan)) {

            }
        }
    }



    @Override
    public void mouseExited(MouseEvent e) {
        hud.potentialDragMode = null;
        super.mouseExited(e);
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
        protected Fingering fingeringMove() {
            return null;
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


            if (ZoomOrtho.this.focused()) {
                gl.glPushMatrix();
                gl.glLoadIdentity();

                super.postpaint(gl);

                finger.drawCrossHair(this, gl);

                gl.glPopMatrix();
            }

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
