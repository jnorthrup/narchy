package spacegraph.space2d.hud;

import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.event.WindowListener;
import com.jogamp.newt.event.WindowUpdateEvent;
import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.event.Off;
import jcog.exe.Exe;
import jcog.math.v2;
import jcog.tree.rtree.rect.RectFloat;
import org.eclipse.collections.api.tuple.Pair;
import spacegraph.input.finger.Finger;
import spacegraph.input.key.KeyPressed;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRender;
import spacegraph.space2d.SurfaceRoot;
import spacegraph.space2d.container.Container;
import spacegraph.util.animate.AnimVector3f;
import spacegraph.util.animate.Animated;
import jcog.math.v3;
import spacegraph.video.JoglSpace;
import spacegraph.video.JoglWindow;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static java.lang.Math.sin;
import static org.eclipse.collections.impl.tuple.Tuples.pair;

/**
 * orthographic widget adapter. something which goes on the "face" of a HUD ("head"s-up-display)
 */
public class Ortho<S extends Surface> extends Container implements SurfaceRoot, WindowListener, KeyPressed {


    private final static float focusAngle = (float) Math.toRadians(45);
    private static final int ZOOM_STACK_MAX = 8;
    public final Finger finger;
    private final NewtKeyboard keyboard;
    public final Camera cam;
    /**
     * current view area, in absolute world coords
     */
    public final v2 scale = new v2(1, 1);

    private final Map<String, Pair<Object, Runnable>> singletons = new HashMap();
    private final Deque<v3> zoomStack = new ArrayDeque();
    S surface;
    public final JoglSpace space;
    private final float camZmin = 5;
    private float camZmax = 640000;
    private float camXmin = -1, camXmax = +1;
    private float camYmin = -1, camYmax = +1;
    private final float zoomMargin = 0.1f;



    /** finger position local to this layer/camera */
    public final v2 fingerPos = new v2();


    public Ortho(JoglSpace space, S content, Finger finger, NewtKeyboard keyboard) {
        super();

        this.space = space;

        this.cam = new Camera();

        this.keyboard = keyboard;

        this.finger = finger;

        setSurface(content);

        start(space);
    }


    public void setSurface(S content) {
        synchronized (this) {
            S oldSurface = this.surface;
            if (oldSurface != null) {
                if (oldSurface == content) {
                    return;//no change
                }
                oldSurface.stop();
            }

            this.surface = content;
            if (surface.parent == null)
                surface.start(this);
        }

        layout();
    }


    @Override
    public void windowResized(WindowEvent e) {
        int W = space.io.window.getWidth();
        int H = space.io.window.getHeight();
        if (posChanged(RectFloat.WH( W, H))) {

            float camZ = targetDepth(Math.max(W, H));
            camZmax = camZ;

            if (autosize()) {
                surface.pos(bounds);
                cam.set(bounds.w / 2f, bounds.h / 2f, camZ);
            }

            layout();
        }
    }


    @Override public final void compile(SurfaceRender render) {

        float zoom = (float) (sin(Math.PI / 2 - focusAngle / 2) / (cam.z * sin(focusAngle / 2)));
        float s = zoom * Math.max(w(), h());

        render.render(cam, scale.set(s, s), surface);
    }

    @Override
    protected void paintIt(GL2 gl, SurfaceRender r) {

        //gl.glLoadIdentity();

        gl.glPushMatrix();

        gl.glScalef(scale.x, scale.y, 1);
        gl.glTranslatef((w()/2)/scale.x - cam.x, (h()/2)/scale.y - cam.y, 0);

        r.render(gl);

        gl.glPopMatrix();
    }


    @Override
    protected void doLayout(int dtMS) {

    }

    protected boolean autosize() {
        return false;
    }

    @Override
    public Object the(String key) {
        synchronized (singletons) {
            Pair<Object, Runnable> x = singletons.get(key);
            return x == null ? null : x.getOne();
        }
    }

    @Override
    public Off onUpdate(Consumer<JoglWindow> c) {
        return space.onUpdate(c);
    }

    public Off animate(Animated c) {
        return space.onUpdate(c);
    }

    private Off animate(Runnable c) {
        return space.onUpdate(c);
    }

    @Override
    public void the(String key, Object added, Runnable onRemove) {
        synchronized (singletons) {

            Pair<Object, Runnable> removed = null;
            if (added == null) {
                assert (onRemove == null);
                removed = singletons.remove(key);
            } else {
                removed = singletons.put(key, pair(added, onRemove));
            }

            if (removed != null) {
                if (removed.getOne() == added) {

                } else {
                    removed.getTwo().run();
                }
            }
        }
    }

    public void start(JoglSpace s) {
        synchronized (this) {

            s.io.addWindowListener(this);

            s.io.addKeyListener(keyboard);

            windowResized(null);

            animate(cam);


            if (surface.parent == null)
                surface.start(this);

            starting();
        }

    }



    @Override
    public SurfaceRoot root() {
        return this;
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
    public void zoomDelta(float delta) {
        v2 xy = cam.screenToWorld(delta < 0 ?
                finger.posPixel
                :
                //TODO fix this zoom-out calculation
                new v2(finger.posPixel.x, finger.posPixel.y)
                //new v2(w()-finger.posPixel.x, h()-finger.posPixel.y)
        );
        cam.set(xy.x, xy.y, cam.z * (1f + delta));
    }

    public void zoomNext(Surface su) {

        //TODO not right
        synchronized (zoomStack) {

            //System.out.println("before: " + zoomStack);
            if (zoomStack.isEmpty()) {
                zoomStack.add(cam.snapshot());
            }

            float epsilon = Math.min(h() / scale.y, w() / scale.x);
            v3 x0 = cam.snapshot();
            if (zoomStack.size() > 1 && su.bounds.contains(x0.x, x0.y) && zoomStack.peekLast().equals(x0, epsilon)) {


                unzoom(su);


            } else {


                {
                    v3 x = cam.snapshot();

                    { //if (zoomStack.isEmpty() || !zoomStack.peekLast().equals(x, epsilon)) {
                        if (zoomStack.size() >= ZOOM_STACK_MAX)
                            zoomStack.removeFirst();

                        zoomStack.addLast(x);
                    }
                }

                zoom(su.bounds);

                {
                    v3 x = cam.snapshot();

                    if (zoomStack.size() >= ZOOM_STACK_MAX)
                        zoomStack.removeFirst();

                    zoomStack.addLast(x);
                }

            }

        }

    }

    void unzoom(Surface su) {
        zoomStack.removeLast();
        v3 prev = zoomStack.peekLast();
        zoom(prev);
    }

    public void zoom(RectFloat b) {
        zoom(b.cx(), b.cy(), b.w, b.h, zoomMargin);
    }


    private float targetDepth(float viewDiameter) {
        return (float) ((viewDiameter * sin(Math.PI / 2 - focusAngle / 2)) / sin(focusAngle / 2));
    }

    private void zoom(float x, float y, float sx, float sy, float margin) {
        zoom(x, y, targetDepth( /*Math.max(sx, sy)*/ (float) (Math.sqrt(sx * sx + sy * sy) * (1 + margin))));
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
    public boolean stop() {
        synchronized (this) {
            stopping();
            assert (surface.parent == this);
            return surface.stop();
        }
    }

    @Override
    public void forEach(Consumer<Surface> o) {
        o.accept(surface);
    }

    @Override
    public boolean whileEach(Predicate<Surface> o) {
        return o.test(surface);
    }

    @Override
    public boolean whileEachReverse(Predicate<Surface> o) {
        return whileEach(o);
    }



    @Override
    public void windowRepaint(WindowUpdateEvent e) {
        visible = true;
    }

    @Override
    public boolean keyFocus(Surface s) {
        return keyboard.focus(s);
    }





    /**
     * called each frame regardless of mouse activity
     * TODO split this into 2 methods.  one picks the current touchable
     * and the other method invokes button changes on the result of the first.
     * this will allow rapid button changes to propagate directly to
     * the picked surface even in-between pick updates which are invoked
     * during the update loop.
     */
    protected Surface finger() {
        /** layer specific, separate from Finger */
        float pmx = finger.posPixel.x, pmy = finger.posPixel.y;
        float wmx = +cam.x + (-0.5f * w() + pmx) / scale.x;
        float wmy = +cam.y + (-0.5f * h() + pmy) / scale.y;
        fingerPos.set(wmx, wmy);

        return finger.touching();
    }


    @Override
    public boolean visible() {
        return visible;
    }

    @Override
    public int childrenCount() {
        return 1;
    }




    public final S the() {
        return surface;
    }


    public class Camera extends AnimVector3f {

        /** TODO atomic */
        private float CAM_RATE = 3f;

        {
            setDirect(0, 0, 1); //(camZmin + camZmax) / 2);
        }

        public Camera() {
            super(1);
        }

        public v3 snapshot() {
            return new v3(target.x, target.y, target.z);
        }

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


//            @Override
//            public void set(float x, float y, float z) {
//                super.set(camX(x), camY(y), camZ(z));
//            }

        @Override
        public void setDirect(float x, float y, float z) {
            super.setDirect(camX(x), camY(y), camZ(z));
        }

        public float camZ(float z) {
            return Util.clamp(z, camZmin, Math.max(camZmin, camZmax));
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
                    ((x- w() /2) /scale.x + cam.x) ,
                    ((y- h() /2) /scale.y + cam.y)
            );
        }

        /** immediately get to where its going */
        public void complete() {
            setDirect(target.x, target.y, target.z);
        }

        public final RectFloat worldToScreen(Surface t) {
            return worldToScreen(t.bounds);
        }


        /** TODO optimize */
        public RectFloat worldToScreen(RectFloat b) {
            v2 ul = worldToScreen(new v2(b.left(), b.top()));
            v2 br = worldToScreen(new v2(b.right(), b.bottom()));
            return RectFloat.XYXY(ul, br);
        }
    }

}
