package spacegraph.space2d.hud;

import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.event.WindowListener;
import com.jogamp.newt.event.WindowUpdateEvent;
import jcog.Util;
import jcog.event.Off;
import jcog.exe.Exe;
import jcog.math.v2;
import jcog.math.v3;
import jcog.tree.rtree.rect.RectFloat;
import spacegraph.input.finger.Finger;
import spacegraph.input.finger.impl.NewtKeyboard;
import spacegraph.input.key.KeyPressed;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.EmptySurface;
import spacegraph.space2d.container.unit.MutableUnitContainer;
import spacegraph.util.animate.AnimVector3f;
import spacegraph.util.animate.Animated;
import spacegraph.video.JoglSpace;

import java.util.ArrayDeque;
import java.util.Deque;

import static java.lang.Math.sin;

/**
 * orthographic widget adapter. something which goes on the "face" of a HUD ("head"s-up-display)
 */
public class Ortho<S extends Surface> extends MutableUnitContainer implements WindowListener, KeyPressed {

    private static final int ZOOM_STACK_MAX = 8;
    private final static float focusAngle = (float) Math.toRadians(45);
    /**
     * current view area, in absolute world coords
     */
    public final v2 scale = new v2(1, 1);
    public final Camera cam;
    /**
     * parent
     */
    public final JoglSpace space;
    @Deprecated
    private final NewtKeyboard keyboard;
    private final Deque<RectFloat> zoomStack = new ArrayDeque();
    private final float camZmin = 1;
    private final float zoomMargin = 0.1f;
    private float camXmin = -1, camXmax = +1;
    private float camYmin = -1, camYmax = +1;


//    /** finger position local to this layer/camera */
//    public final v2 fingerPos = new v2();

    public Ortho(JoglSpace space, NewtKeyboard keyboard) {
        super(new EmptySurface());

        this.space = space;

        this.cam = new Camera();

        this.keyboard = keyboard;

    }


    @Override
    public void windowResized(WindowEvent e) {
        int W = space.display.window.getWidth();
        int H = space.display.window.getHeight();
        if (posChanged(RectFloat.WH(W, H))) {


            if (autosize()) {
                the().pos(bounds);
            }
            if (autosize()) {
                unzoom();
            }

            layout();
        }
    }

    /**
     * full unzoom
     */
    public void unzoom() {
        cam.set(bounds.w / 2f, bounds.h / 2f, camZMax());
    }

    private float camZMax() {
        return targetDepth(Math.min(space.display.window.getWidth(), space.display.window.getHeight()));
    }

    @Override
    public final void render(ReSurface render) {

        render.on((gl) -> {

            float zoom = (float) (sin(Math.PI / 2 - focusAngle / 2) / (cam.z * sin(focusAngle / 2)));
            float s = zoom * Math.min(w(), h());

            render.set(cam, scale.set(s, s));

            gl.glPushMatrix();

            gl.glScalef(scale.x, scale.y, 1);
            gl.glTranslatef((w() / 2) / scale.x - cam.x, (h() / 2) / scale.y - cam.y, 0);
        });

        the().tryRender(render);

        render.on((gl) -> {
            gl.glPopMatrix();
        });
    }

    protected boolean autosize() {
        return false;
    }

    public Off animate(Animated c) {
        return space.onUpdate(c);
    }

    private Off animate(Runnable c) {
        return space.onUpdate(c);
    }

    @Override
    protected void starting() {
        super.starting();

        JoglSpace s = (JoglSpace) root();
        synchronized (this) {

            s.display.addWindowListener(this);

            s.display.addKeyListener(keyboard);

            windowResized(null);

            animate(cam);

        }

    }

    @Override
    public Ortho move(float x, float y) {
        throw new UnsupportedOperationException();
    }


    /**
     * delta in (-1..+1)
     * POSITIVE = ?
     * NEGATIVE = ?
     */
    public void zoomDelta(Finger finger, float delta) {
        v2 xy = cam.screenToWorld(delta < 0 ?
                        finger.posPixel
                        :
                        //TODO fix this zoom-out calculation
                        new v2(finger.posPixel.x, finger.posPixel.y)
                //new v2(w()-finger.posPixel.x, h()-finger.posPixel.y)
        );
        cam.set(xy.x, xy.y, cam.z * (1f + delta));
    }

    public void zoomNext(Surface x) {

        //TODO fix
        synchronized (zoomStack) {

            RectFloat cc = x.bounds;

            int s = zoomStack.size();
            float epsilon = Math.min(1 / h(), 1 / w());
            boolean camSame = s > 0 && zoomStack.peekLast().equals(cc, epsilon);
            if (s > 1 && camSame) {

                zoomStack.removeLast(); //POP
                zoom(zoomStack.peekLast());

            } else if (s > 0 && camSame) {

                unzoom(); //unzoom completely

            } else if (!camSame) {

                if (s + 1 >= ZOOM_STACK_MAX)
                    zoomStack.removeFirst(); //EVICT
                zoomStack.addLast(cc); //PUSH

                zoom(cc);

            }

        }

    }

    public void zoom(RectFloat b) {
        zoom(b.cx(), b.cy(), b.w, b.h, zoomMargin);
    }

    /**
     * choose best zoom radius for the target rectangle according to current view aspect ratio
     */
    private float targetDepth(float w, float h, float margin) {
        float d = Math.max(w, h);
//        if (((((float) pw()) / ph()) >= 1) == ((w / h) >= 1))
//            d = h; //limit by height
//        else
//            d = w; //limit by width

        return targetDepth(d * (1 + margin));
    }


    private float targetDepth(float viewDiameter) {
        return (float) ((viewDiameter * sin(Math.PI / 2 - focusAngle / 2)) / sin(focusAngle / 2));
    }

    private void zoom(float x, float y, float sx, float sy, float margin) {
        zoom(x, y, targetDepth(sx, sy, margin));
    }

    public final void zoom(v3 v) {
        zoom(v.x, v.y, v.z);
    }

    public void zoom(float x, float y, float z) {
        cam.set(x, y, z);
    }


    @Override
    public void windowMoved(WindowEvent e) {

    }

    @Override
    public void windowDestroyNotify(WindowEvent e) {
        Exe.invokeLater(this::stop);
    }

    @Override
    public void windowDestroyed(WindowEvent e) {

    }

    @Override
    public void windowGainedFocus(WindowEvent e) {

    }

    @Override
    public void windowLostFocus(WindowEvent e) {

    }


    @Override
    public void windowRepaint(WindowUpdateEvent e) {
        visible = true;
    }


//    /**
//     * called each frame regardless of mouse activity
//     * TODO split this into 2 methods.  one picks the current touchable
//     * and the other method invokes button changes on the result of the first.
//     * this will allow rapid button changes to propagate directly to
//     * the picked surface even in-between pick updates which are invoked
//     * during the update loop.
//     */
//    protected Surface finger() {
//        /** layer specific, separate from Finger */
//        float pmx = finger.posPixel.x, pmy = finger.posPixel.y;
//        float wmx = +cam.x + (-0.5f * w() + pmx) / scale.x;
//        float wmy = +cam.y + (-0.5f * h() + pmy) / scale.y;
//        fingerPos.setAt(wmx, wmy);
//
//        return finger.touching();
//    }


    public class Camera extends AnimVector3f {

        /**
         * TODO atomic
         */
        private final float CAM_RATE = 3f;

        {
            setDirect(0, 0, 1); //(camZmin + camZmax) / 2);
        }

        public Camera() {
            super(1);
        }

//        public v3 snapshot() {
//            return new v3(target.x, target.y, target.z);
//        }

        @Override
        public boolean animate(float dt) {
            //System.out.println(this);
            if (super.animate(dt)) {
                update();
                return true;
            }
            return false;
        }

        protected void update() {
            if (!Ortho.this.visible())
                return;

            //System.out.println(z);
            float W = bounds.w;
            float H = bounds.h;
            speed.set(Math.max(W, H) * CAM_RATE);

            float visW = W / scale.x / 2, visH = H / scale.y / 2; //TODO optional extra margin
            camXmin = 0 + visW;
            camYmin = 0 + visH;
            camXmax = bounds.w - visW;
            camYmax = bounds.h - visH;
        }

        @Override
        public void setDirect(float x, float y, float z) {
            super.setDirect(camX(x), camY(y), camZ(z));
        }

        public float camZ(float z) {
            return Util.clamp(z, camZmin, Math.max(camZmin, camZMax()));
        }

        public float camY(float y) {
            return Util.clamp(y, camYmin, Math.max(camYmin, camYmax));
        }

        public float camX(float x) {
            return Util.clamp(x, camXmin, Math.max(camXmin, camXmax));
        }

        public float motionSq() {
            v3 t = new v3(target);
            t.add(-x, -y, -z);
            return t.lengthSquared();
        }

        public v2 worldToScreen(float wx, float wy) {
            return new v2(
                    ((wx - cam.x) * scale.x) + w() / 2,
                    ((wy - cam.y) * scale.y) + h() / 2
            );
        }

        public final v2 worldToScreen(v2 xy) {
            return worldToScreen(xy.x, xy.y);
        }

        public final v2 screenToWorld(v2 xy) {
            return screenToWorld(xy.x, xy.y);
        }

        public v2 screenToWorld(float x, float y) {
            return new v2(
                    ((x - w() / 2) / scale.x + cam.x),
                    ((y - h() / 2) / scale.y + cam.y)
            );
        }

        /**
         * immediately get to where its going
         */
        public void complete() {
            setDirect(target.x, target.y, target.z);
        }

        public final RectFloat worldToScreen(Surface t) {
            return worldToScreen(t.bounds);
        }


        /**
         * TODO optimize
         */
        public RectFloat worldToScreen(RectFloat b) {
            v2 ul = worldToScreen(new v2(b.left(), b.bottom()));
            v2 br = worldToScreen(new v2(b.right(), b.top()));
            return RectFloat.XYXY(ul, br);
        }
    }

}
