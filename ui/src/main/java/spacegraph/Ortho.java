package spacegraph;

import com.jogamp.newt.event.*;
import com.jogamp.opengl.GL2;
import jcog.event.ListTopic;
import jcog.event.On;
import jcog.event.Topic;
import jcog.tree.rtree.rect.RectFloat2D;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.api.tuple.Pair;
import spacegraph.input.Finger;
import spacegraph.layout.Container;
import spacegraph.math.v3;
import spacegraph.phys.util.AnimVector2f;
import spacegraph.phys.util.AnimVector3f;
import spacegraph.phys.util.Animated;
import spacegraph.render.JoglSpace;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.eclipse.collections.impl.tuple.Tuples.pair;

/**
 * orthographic widget adapter. something which goes on the "face" of a HUD ("head"s-up-display)
 */
public class Ortho extends Container implements SurfaceRoot, WindowListener, KeyListener, MouseListener {

    protected final AnimVector2f scale;
    boolean visible;

    final Finger finger;

    //temporary: world mouse coord
    protected float wmy, wmx;


    public Surface surface;
    public SpaceGraph window;
    protected final v3 cam;

    final Topic logs = new ListTopic();
    private short[] buttonsDown;
    private Animated fingerUpdate;
    private boolean focused = false;

    public Ortho() {
        this(new EmptySurface());
    }

    public Ortho(Surface content) {
        super();
        this.finger = new Finger(this);
        this.scale = new AnimVector2f(1, 1, 6f);
        this.cam = new AnimVector3f(8f);
        this.surface = content;

        this.fingerUpdate = dt -> {
            if (hasFocus()) {
                updateMouse(wmx, wmy, buttonsDown);
            }
            return true;
        };
    }

    public boolean hasFocus() {
        return focused;
    }

    @Override
    protected void doLayout(int dtMS) {
        surface.pos(bounds);
        surface.layout();
    }

    public GL2 gl() {
        return window.gl();
    }

    final Map<String, Pair<Object, Runnable>> singletons = new HashMap();


    @Override
    public Object the(String key) {
        synchronized (singletons) {
            Pair<Object, Runnable> x = singletons.get(key);
            return x == null ? null : x.getOne();
        }
    }

    @Override
    public On onUpdate(Consumer<JoglSpace> c) {
        return window.onUpdate.on(c);
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

    public void log(Object... o) {
        logs.emit(o);
    }

    public void log(Object o) {
        logs.emit(o);
    }


    public void start(SpaceGraph s) {
        this.window = s;
        s.addWindowListener(this);
        this.focused = window.window.hasFocus();
        s.addMouseListener(this);
        s.addKeyListener(this);

        surface.start(this);

        windowResized(null);

        s.dyn.addAnimation(scale);
        s.dyn.addAnimation((Animated) cam);
        s.dyn.addAnimation(fingerUpdate);

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
            if (zoomStack.size() > ZOOM_STACK_MAX) {
                zoomStack.removeFirst();
            }
            //}
            float s = scale.x;
            RectFloat2D curZoom = new RectFloat2D(cam.x - s / 2, cam.y - s / 2, cam.x + s / 2, cam.y + s / 2);
            zoomStack.addLast(()->curZoom);

            zoom(su.bounds);

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
        cam.setLerp(x, y, 0, speed);
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
        float s = Math.min(sx, sy);
        scale.set(s, s);
        return this;
    }

    @Override
    public void windowResized(WindowEvent e) {
        int W, H;
        pos(0, 0,
                W = window.getWidth(),
                H = window.getHeight());
        cam.set(W / 2f, H / 2f);
        scale(1, 1);

        layout();
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
    public void stop() {
        synchronized (this) {
            surface.stop();
        }
    }

    @Override
    public void forEach(Consumer<Surface> o) {
        o.accept(surface);
    }

    @Override
    public void windowGainedFocus(WindowEvent e) {
        focused = true;
    }

    @Override
    public void windowLostFocus(WindowEvent e) {
        focused = false;
        updateMouse(null);
    }

    @Override
    public void windowRepaint(WindowUpdateEvent e) {
        visible = true;
    }

    @Override
    public void keyPressed(KeyEvent e) {
        surface.onKey(e, true);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        surface.onKey(e, false);
    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mouseEntered(MouseEvent e) {
        updateMouse(e);
    }

    @Override
    public void mouseExited(MouseEvent e) {
        updateMouse(null);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        updateMouse(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        short[] bd = e.getButtonsDown();
        int ii = ArrayUtils.indexOf(bd, e.getButton());
        bd[ii] = -1;
        updateMouse(e, bd);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        updateMouse(e);
    }

    protected void updateMouse(MouseEvent e) {
        updateMouse(e, e != null ? e.getButtonsDown() : null);
    }


    private boolean updateMouse(MouseEvent e, short[] buttonsDown) {

        if (e != null) {

            //screen coordinates
            float sx = e.getX();

            float W = w();
            float H = h();
            float sy = H - e.getY();

            wmx = +cam.x + (-0.5f * W + sx) / scale.x;
            wmy = +cam.y + (-0.5f * H + sy) / scale.x;

            if (window != null) {
                if (window.window != null) {
                    Finger.pointer.set(window.windowX + e.getX(), window.windowY + e.getY());
                }
            }

            this.buttonsDown = buttonsDown;
            //updateMouse(sx, sy, buttonsDown);
            return true;

        } else {

            this.buttonsDown = null;
            updateMouse(wmx, wmy, null);

            return false;
        }
    }


    /** called each frame regardless of mouse activity */
    Surface updateMouse(float sx, float sy, short[] buttonsDown) {


        /*if (e == null) {
            off();
        } else {*/
        Surface s;

        //System.out.println(lx + " " + ly);
        //if (lx >= 0 && ly >= 0 && lx <= 1f && ly <= 1f) {
        if ((s = finger.on(sx, sy, wmx, wmy, buttonsDown)) != null) {
            log("on", s);
//            if (e != null)
//                e.setConsumed(true);
            return s;
        } else {
            return null;
        }
    }


    @Override
    protected void paintBelow(GL2 gl) {
        float sx = scale.x;
        //float sy = scale.y;
        gl.glTranslatef(w() / 2f, h() / 2f, 0);
        gl.glScalef(sx, sx, 1);
        gl.glTranslatef(-cam.x, -cam.y, 0);
        //gl.glTranslatef((sx) * -cam.x, sy * -cam.y, 0);
    }

    @Override
    public int childrenCount() {
        return 1;
    }


    @Override
    public void mouseDragged(MouseEvent e) {

        updateMouse(e);
    }


    @Override
    public void mouseWheelMoved(MouseEvent e) {

    }

    static final float zoomDilation = 1.05f;

    public static float getTargetHeight(RectFloat2D rect) {
        float r = rect.mag() / 2.0f * zoomDilation;
        double focus = Math.toRadians(45 /* degrees */);
        return r * (float) (Math.sin(Math.PI / 2.0 - focus / 2.0) / Math.sin(focus / 2.0));
    }
}
