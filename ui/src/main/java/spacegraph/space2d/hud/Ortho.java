package spacegraph.space2d.hud;

import com.jogamp.newt.event.*;
import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.event.Off;
import jcog.exe.Exe;
import jcog.tree.rtree.rect.RectFloat2D;
import org.eclipse.collections.api.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import spacegraph.input.finger.Finger;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRender;
import spacegraph.space2d.SurfaceRoot;
import spacegraph.space2d.container.Container;
import spacegraph.space2d.container.EmptySurface;
import spacegraph.space2d.widget.windo.Widget;
import spacegraph.util.animate.AnimVector3f;
import spacegraph.util.animate.Animated;
import spacegraph.util.math.v2;
import spacegraph.util.math.v3;
import spacegraph.video.JoglSpace;
import spacegraph.video.JoglWindow;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static java.lang.Math.sin;
import static org.eclipse.collections.impl.tuple.Tuples.pair;

/**
 * orthographic widget adapter. something which goes on the "face" of a HUD ("head"s-up-display)
 */
public class Ortho extends Container implements SurfaceRoot, WindowListener, MouseListener, KeyListener {


    private final static float focusAngle = (float) Math.toRadians(45);
    private static final int ZOOM_STACK_MAX = 8;
    public final Finger finger;
    public final Camera cam;
    /**
     * current view area, in absolute world coords
     */
    public final v2 scale = new v2(1, 1);
    private final AtomicBoolean focused = new AtomicBoolean(false);
    private final Map<String, Pair<Object, Runnable>> singletons = new HashMap();
    private final Deque<v3> zoomStack = new ArrayDeque();
    private final Runnable fingerUpdate;
    private final Set<Surface> overlays = new CopyOnWriteArraySet<>();
    Surface surface;
    public JoglSpace window;
    private final float camZmin = 5;
    private volatile float camZmax = 640000;
    private volatile float camXmin = -1, camXmax = +1;
    private volatile float camYmin = -1, camYmax = +1;
    private final float zoomMargin = 0.25f;


    Ortho() {
        this(new EmptySurface());
    }

    Ortho(Surface content) {
        super();
        this.finger = new Finger();


        this.cam = new Camera();


        this.surface = content;

        this.fingerUpdate = () -> {
            if (focused())
                finger();


        };
    }


    @Override
    public void windowResized(WindowEvent e) {


        //doLayout(0);
    }

    final boolean focused() {
        return focused.getOpaque();
    }

    @Override
    public void prePaint(int dtMS) {


        int W = window.window.getWidth();
        int H = window.window.getHeight();
        if (posChanged(RectFloat2D.XYXY(0, 0, W, H))) {
            layout();
        }

        float scale = (float) (sin(Math.PI / 2 - focusAngle / 2) / (cam.z * sin(focusAngle / 2)));
        float s = Math.max(W, H) * scale;
        this.scale.set(s, s);

        //}
        super.prePaint(dtMS);
    }

    @Override
    protected void doLayout(int dtMS) {

        float camZ = targetDepth(Math.max(bounds.w, bounds.h));

        if (autoresize()) {
            surface.pos(bounds);

            camZmax = camZ;

        }

        cam.set(bounds.w / 2f, bounds.h / 2f, camZ);

    }

    boolean autoresize() {
        return false;
    }

    public GL2 gl() {
        return window.gl;
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
        return window.onUpdate(c);
    }

    public Off animate(Animated c) {
        return window.onUpdate(c);
    }

    private Off animate(Runnable c) {
        return window.onUpdate(c);
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
            this.window = s;
            s.addWindowListener(this);
            if (window.window.hasFocus())
                mouseEntered(null);

            s.addMouseListenerPre(this);

            s.addKeyListener(this);

            animate(cam);
            animate(fingerUpdate);

            windowResized(null);

            assert (surface.parent == null);
            surface.start(this);
            //Exe.invokeLater(() -> surface.start(this));
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

    @Override
    public void zoom(Surface su) {

        //TODO not right
        synchronized (zoomStack) {

            //System.out.println("before: " + zoomStack);
            if (zoomStack.isEmpty()) {
                zoomStack.add( cam.snapshot() );
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

                    { //if (zoomStack.isEmpty() || !zoomStack.peekLast().equals(x, epsilon)) {

                        if (zoomStack.size() >= ZOOM_STACK_MAX)
                            zoomStack.removeFirst();

                        zoomStack.addLast(x);
                    }
                }

                //System.out.println("zoom " + su + " " + cam.snapshot());
            }

            //System.out.println("after: " + zoomStack);
        }

    }

    void unzoom(Surface su) {
        zoomStack.removeLast();
        v3 prev = zoomStack.peekLast();
        zoom(prev);

        System.out.println("unzoom " + prev + " " + su);
    }

    public void zoom(RectFloat2D b) {
        zoom(b.cx(), b.cy(), b.w, b.h, zoomMargin);
    }

    private float targetDepth(float viewDiameter) {
        return (float) ((viewDiameter * sin(Math.PI / 2 - focusAngle / 2)) / sin(focusAngle / 2));
    }

    private void zoom(float x, float y, float sx, float sy, float margin) {
        zoom(x, y, targetDepth(Math.max(sx, sy) * (1 + margin)));
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
    public boolean stop() {
        synchronized (this) {
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
    public void windowGainedFocus(WindowEvent e) {
        if (focused.compareAndSet(false, true)) {
            finger.enter();
            update(false, null);
        }
    }

    @Override
    public void windowLostFocus(WindowEvent e) {
        if (focused.compareAndSet(true, false)) {
            update(false, null);
            finger.exit();
        }
    }

    @Override
    public void windowRepaint(WindowUpdateEvent e) {
        visible = true;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        setKey(e, true);
    }

    @Override
    public void keyReleased(KeyEvent e) {

        setKey(e, false);
    }

    private void setKey(KeyEvent e, boolean pressOrRelease) {
        if (e.isConsumed())
            return;

        Widget t = finger.touching.get();
        if (t != null) {
            if (!t.key(e, pressOrRelease))
                e.setConsumed(true);
        }

    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mouseEntered(@Nullable MouseEvent e) {
        if (focused.compareAndSet(false, true)) {
            finger.enter();
            if (e != null)
                update(false, e);
        }
    }

    @Override
    public void mouseExited(MouseEvent e) {
        if (focused.compareAndSet(true, false)) {
            update(false, null);
            finger.exit();
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {


        if (update(false, e, e.getButtonsDown())) {
            if (finger.touching.get() != null)
                e.setConsumed(true);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {


        short[] bd = e.getButtonsDown();


        for (int i = 0, bdLength = bd.length; i < bdLength; i++)
            bd[i] = (short) -bd[i];

        update(false, e, bd);

        if (finger.touching.get() != null)
            e.setConsumed(true);

    }


    @Override
    public void mouseDragged(MouseEvent e) {


        if (update(true, e))
            if (finger.touching.get() != null)
                e.setConsumed(true);
    }

    @Override
    public void mouseMoved(MouseEvent e) {

        update(true, e);


    }

    private boolean update(boolean moved, MouseEvent e) {
        return update(moved, e, null);
    }

    private boolean update(boolean moved, MouseEvent e, short[] buttonsDown) {

        if (moved) {

            JoglSpace w = this.window;
            int pmx = e.getX();
            int pmy = w.getHeight() - e.getY();
            float wmx = +cam.x + (-0.5f * w() + pmx) / scale.x;
            float wmy = +cam.y + (-0.5f * h() + pmy) / scale.y;

            finger.posPixel.set(pmx, pmy);
            finger.posScreen.set(w.getX() + pmx, w.getScreenY() - (e.getY() + w.getY()));
            finger.pos.set(wmx, wmy);
        }

        if (buttonsDown != null) {
            finger.update(buttonsDown);
        }

        return e != null;
    }

    /**
     * called each frame regardless of mouse activity
     * TODO split this into 2 methods.  one picks the current touchable
     * and the other method invokes button changes on the result of the first.
     * this will allow rapid button changes to propagate directly to
     * the picked surface even in-between pick updates which are invoked
     * during the update loop.
     */
    Surface finger() {

        /*if (e == null) {
            off();
        } else {*/

        //assert (focused());

        finger.update();

        return finger.on(surface);
    }


    @Override
    protected void paintBelow(GL2 gl, SurfaceRender r) {


        gl.glLoadIdentity();


        gl.glTranslatef(w() / 2f, h() / 2f, 0);
        gl.glScalef(scale.x, scale.y, 1);
        gl.glTranslatef(-cam.x, -cam.y, 0);

        gl.glPushMatrix();
    }

    @Override
    protected void paintAbove(GL2 gl, SurfaceRender r) {

        gl.glPopMatrix();

        if (!overlays.isEmpty()) {
            overlays.forEach(s -> s.render(gl, r));
        }
    }

    @Override
    public boolean visible() {
        return visible;
    }

    @Override
    public int childrenCount() {
        return 1;
    }


    @Override
    public void mouseWheelMoved(MouseEvent e) {
        finger.rotationAdd(e.getRotation());
    }

    public void addOverlay(Surface s) {
        synchronized(overlays) {
            if (s.start(this)) {
                overlays.add(s);
            }
        }
    }

    public void removeOverlay(Surface s) {
        synchronized(overlays) {
            boolean ss = overlays.remove(s);
            if (ss) {
                s.stop();
            }
        }
    }

    public void set(Surface content) {
        synchronized (this) {
            if (this.surface == content) return;

            if (this.surface != null) {
                this.surface.stop();
            }

            this.surface = content;

            this.surface.start(this);
        }

        layout();
    }


    protected class Camera extends AnimVector3f {

        float CAM_RATE = 2f;

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
            if (!Ortho.this.focused())
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

            //absorb remaining rotationY
            float zoomRate = 0.5f;

            if (!(finger.touching() instanceof Finger.WheelAbsorb)) {
                float dy = finger.rotationY(false);
                if (dy != 0) {
                    cam.set(cam.x, cam.y, cam.z * (1f + (dy * zoomRate)));
                }
            }

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
            t.add(-x,-y,-z);
            return t.lengthSquared();
        }
    }
}
