package spacegraph.space2d.hud;

import com.jogamp.newt.event.*;
import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.event.On;
import jcog.tree.rtree.rect.RectFloat2D;
import org.eclipse.collections.api.tuple.Pair;
import spacegraph.input.finger.Finger;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRoot;
import spacegraph.space2d.container.Container;
import spacegraph.space2d.container.EmptySurface;
import spacegraph.space2d.widget.windo.Widget;
import spacegraph.util.animate.AnimVector2f;
import spacegraph.util.animate.AnimVector3f;
import spacegraph.util.animate.Animated;
import spacegraph.util.math.v3;
import spacegraph.video.JoglSpace;
import spacegraph.video.JoglWindow;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static org.eclipse.collections.impl.tuple.Tuples.pair;

/**
 * orthographic widget adapter. something which goes on the "face" of a HUD ("head"s-up-display)
 */
public class Ortho extends Container implements SurfaceRoot, WindowListener, MouseListener, KeyListener {

    public final AnimVector2f scale;
    public float scaleMin = 0.25f;
    public float scaleMax = 10000f;

    public final Finger finger;




    public Surface surface;
    public JoglSpace window;
    protected final v3 cam;



    private final Animated fingerUpdate;


    private volatile boolean focused = false;

    final Map<String, Pair<Object, Runnable>> singletons = new HashMap();

    //private final AtomicBoolean fingerMoved = new AtomicBoolean(true);
    private final Set<Surface> overlays = new CopyOnWriteArraySet<>();

    public Ortho() {
        this(new EmptySurface());
    }

    public Ortho(Surface content) {
        super();
        this.finger = new Finger();

        this.scale = new AnimVector2f(1, 1, 5f) {
          //TODO add animation ifChanged -> fingerUpdated
        };

        this.cam = new AnimVector3f(12f) {
            @Override
            protected float interp(float dt) {
                float W = window.getWidth();
                float H = window.getHeight();

                dt *= scale.x / Math.max(W,H);

                float dist = super.interp(dt);
//                if (dist > 0.001f) {
//                    fingerUpdated.set(true);
//                }
                return dist;
            }
        };

        this.surface = content;

        this.fingerUpdate = this::finger;
    }


    @Override
    public void windowResized(WindowEvent e) {
        if (maximize()) {
            //re-maximize
            int ww = window.getWidth();
            int hh = window.getHeight();
            pos(0, 0, ww, hh); //triggers layout
        } else {
            layout(); //only layout
        }
    }

    protected boolean maximize() {
        return false;
    }

    public boolean hasFocus() {
        return focused;
    }

    @Override
    protected void doLayout(int dtMS) {
        cam.set(bounds.w / 2f, bounds.h / 2f);

        if (autoresize())
            surface.pos(bounds);

        //fingerMoved.set(true);
    }

    public boolean autoresize() {
        return true;
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
    public On onUpdate(Consumer<JoglWindow> c) {
        return window.onUpdate(c);
    }
    public On animate(Animated c) {
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
                    //same
                } else {
                    removed.getTwo().run();
                }
            }
        }
    }

//    public On onLog(Consumer o) {
//        return logs.on(o);
//    }




    public void start(JoglSpace s) {
        synchronized (this) {
            this.window = s;
            s.addWindowListener(this);
            this.focused = window.window.hasFocus();
            s.addMouseListenerPre(this);

            s.addKeyListener(this);

            animate(scale);
            animate((Animated) cam);
            animate(fingerUpdate);

            windowResized(null);

            surface.start(this);
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


    float zoomMargin = 0.25f;
    final ArrayDeque<Supplier<RectFloat2D>> zoomStack = new ArrayDeque();
    static final int ZOOM_STACK_MAX = 8; //FOR safety

    @Override
    public void zoom(Surface su) {

        synchronized (zoomStack) {
            ///if (!zoomStack.isEmpty()) {
//                if (!zoomStack.getLast().contains(nextZoom)) {
//                    zoomStack.clear();
            /*                } else */
            RectFloat2D target;
//            if (!zoomStack.isEmpty() && zoomStack.getLast().gfinger.touching==su) { //zoomStack.size()>1 && zoomStack.getLast().get().equals(su.bounds /* TODO maybe fuzzy equals */)) {
//                zoomStack.removeLast();
//                target = zoomStack.getLast().get();
//            } else {
            {
                if (zoomStack.size() > ZOOM_STACK_MAX) {
                    zoomStack.removeFirst();
                }
                //}
                float s = scale.x;
                RectFloat2D curZoom = RectFloat2D.XYXY(cam.x - s / 2, cam.y - s / 2, cam.x + s / 2, cam.y + s / 2);
                zoomStack.addLast(() -> curZoom);

                target = su.bounds;
            }

            zoom(target);


        }

    }

    public void zoom(RectFloat2D b) {
        zoom(b.cx(), b.cy(), b.w, b.h, zoomMargin, 1);
    }

    public void zoom(RectFloat2D b, float speed) {
        zoom(b.cx(), b.cy(), b.w, b.h, zoomMargin, speed);
    }

    public void zoom(float x, float y, float sx, float sy, float margin, float speed) {
        float s0 = (1 + margin);
        cam.set(x, y);
        //cam.setLerp(x, y, 0, speed);
        scale(w() / (sx * s0), h() / (sy * s0));
    }

    @Override
    public void unzoom() {
        synchronized (zoomStack) {
            if (!zoomStack.isEmpty()) {
                RectFloat2D z = zoomStack.removeLast().get();
                scale(z.w, z.h);
                cam.set(z.cx(), z.cy());
            }
        }
    }

    public Ortho scale(float sx, float sy) {
        float s = Util.clamp(Math.min(sx, sy), scaleMin, scaleMax);
        scale.set(s, s);
        return this;
    }




    @Override
    public void windowMoved(WindowEvent e) {

    }

    @Override
    public void windowDestroyNotify(WindowEvent e) {
        visible = false;
        stop();
    }

    @Override
    public void windowDestroyed(WindowEvent e) {

    }

    @Override
    public boolean stop() {
        return surface.stop();
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
        focused = true;
        update(false, null);
//        fingerMoved.set(true);
        
    }

    @Override
    public void windowLostFocus(WindowEvent e) {
        update(false, null);
//        fingerMoved.set(true);
        focused = false;
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

        Widget t = finger.touching;
        if (t !=null) {
            if (!t.tryKey(e, pressOrRelease))
                e.setConsumed(true);
        }

    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {
        focused = true;
        update(true, e);
//        fingerMoved.set(true);
    }

    @Override
    public void mouseExited(MouseEvent e) {
        update(false, null);
        focused = false;
    }

    @Override
    public void mousePressed(MouseEvent e) {
//        if (e.isConsumed())
//            return;
        if (update(false, e, e.getButtonsDown())) {
            if (finger.touching!=null)
                e.setConsumed(true);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
//        if (e.isConsumed())
//            return;
        System.out.println("released: " + e);
        short[] bd = e.getButtonsDown();

        //invert to negative value to indicate release
        for (int i = 0, bdLength = bd.length; i < bdLength; i++)
            bd[i] = (short) -bd[i];

        update(false, e, bd);

        if (finger.touching!=null) e.setConsumed(true);

    }


    @Override
    public void mouseDragged(MouseEvent e) {
//        if (e.isConsumed())
//            return;
        if (update(true, e))
            if (finger.touching!=null)
                e.setConsumed(true);
    }

    @Override
    public void mouseMoved(MouseEvent e) {

        update(true, e);


    }

    protected boolean update(boolean moved, MouseEvent e) {
        return update(moved, e, null);
    }

    private boolean update(boolean moved, MouseEvent e, short[] buttonsDown) {

        if (moved) {
            //        int windowWidth = window.getWidth();
            int windowHeight = window.getHeight();
            int pmx = e.getX();
            int pmy = windowHeight - e.getY();

            //screen coordinates
            float sx = e.getX();

            float W = w();
            float H = h();
            float sy = window.getHeight() - e.getY();

            float wmx = +cam.x + (-0.5f * W + sx) / scale.x;
            float wmy = +cam.y + (-0.5f * H + sy) / scale.x;

            finger.pos.set(wmx, wmy);
            finger.posGlobal.set(pmx, pmy);

            if (window.window != null) {
                Finger.pointer.set(window.windowX + e.getX(), window.windowY + e.getY());
            }
        }

        if (buttonsDown!=null) {
            finger.update(buttonsDown); //calls .update()
        } else if (moved) {
            finger.update(); //just move event
        }

        return e!=null;
    }

    /** called each frame regardless of mouse activity
     * TODO split this into 2 methods.  one picks the current touchable
     * and the other method invokes button changes on the result of the first.
     * this will allow rapid button changes to propagate directly to
     * the picked surface even in-between pick updates which are invoked
     * during the update loop.
     * */
    protected boolean finger(float dt) {

        /*if (e == null) {
            off();
        } else {*/



        Surface touching = finger.touching;
        Surface touchedNext = finger.on(surface);

        finger.update();

        if (touchedNext!=null && touchedNext!=touching) {
            debug(this, 1f, ()->"touch(" + touchedNext + ')');
        }
        return true;
    }



    @Override
    protected void paintBelow(GL2 gl) {


        gl.glLoadIdentity();

        float sx = scale.x;
        //float sy = scale.y;
        gl.glTranslatef(w() / 2f, h() / 2f, 0);
        gl.glScalef(sx, sx, 1);
        gl.glTranslatef(-cam.x, -cam.y, 0);
        //gl.glTranslatef((sx) * -cam.x, sy * -cam.y, 0);

        gl.glPushMatrix();
    }

    @Override
    protected void paintAbove(GL2 gl, int dtMS) {

        gl.glPopMatrix();

        if (!overlays.isEmpty()) {

            overlays.forEach(s -> s.render(gl, dtMS));

        }
    }

    @Override
    public int childrenCount() {
        return 1;
    }




    @Override
    public void mouseWheelMoved(MouseEvent e) {

    }

    public void addOverlay(Surface s) {
        overlays.add(s);
    }
    public void removeOverlay(Surface s) {
        overlays.remove(s);
    }

    public void set(Surface content) {
        synchronized(this) {
            if (this.surface == content) return;

            if (this.surface !=null) {
                this.surface.stop();
            }

            this.surface = content;

            this.surface.start(this);
        }

        layout();
    }

//    static final float zoomDilation = 1.05f;

//    public static float getTargetHeight(RectFloat2D rect) {
//        float r = rect.mag() / 2.0f * zoomDilation;
//        double focus = Math.toRadians(45 /* degrees */);
//        return r * (float) (Math.sin(Math.PI / 2.0 - focus / 2.0) / Math.sin(focus / 2.0));
//    }
}
