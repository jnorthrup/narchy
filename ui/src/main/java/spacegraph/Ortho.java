package spacegraph;

import com.jogamp.newt.event.*;
import com.jogamp.opengl.GL2;
import jcog.event.ListTopic;
import jcog.event.On;
import jcog.event.Topic;
import jcog.tree.rtree.rect.RectFloat2D;
import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.collections.api.tuple.Pair;
import org.jetbrains.annotations.Nullable;
import spacegraph.input.Finger;
import spacegraph.math.v3;
import spacegraph.phys.util.AnimVector2f;
import spacegraph.phys.util.AnimVector3f;
import spacegraph.phys.util.Animated;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import static org.eclipse.collections.impl.tuple.Tuples.pair;

/**
 * orthographic widget adapter. something which goes on the "face" of a HUD ("head"s-up-display)
 */
public class Ortho extends Surface implements SurfaceRoot, WindowListener, KeyListener, MouseListener {

    protected final AnimVector2f scale;
    boolean visible;

    final Finger finger;

    //temporary: world mouse coord
    protected float wmy, wmx;

    /**
     * window width/height in pixels
     */
    protected int W;
    protected int H;

    public Surface surface;
    public SpaceGraph window;
    protected final v3 cam;

    final Topic logs = new ListTopic();

    public Ortho() {
        this.finger = new Finger(this);
        this.scale = new AnimVector2f(1, 1, 6f);
        this.cam = new AnimVector3f(8f);
    }

    public Ortho(Surface content) {
        this();
        setSurface(content);
    }

    @Override
    public void layout() {
        surface.layout();
        //surface.print(System.out, 0);
    }

    @Override
    public GL2 gl() {
        return window.gl();
    }

    final Map<String, Pair<Object, Runnable>> singletons = new HashMap();


    @Override
    public synchronized Object the(String key) {
        synchronized (singletons) {
            Pair<Object, Runnable> x = singletons.get(key);
            return x == null ? null : x.getOne();
        }
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

    @Override
    public On onLog(Consumer o) {
        return logs.on(o);
    }

    public void log(Object... o) {
        logs.emit(o);
    }

    public void log(Object o) {
        logs.emit(o);
    }

    public void setSurface(Surface content) {
        this.surface = content;
    }


    public void start(SpaceGraph s) {
        this.window = s;
        windowResized(null);
        s.addWindowListener(this);
        s.addMouseListener(this);
        s.addKeyListener(this);
        s.dyn.addAnimation(scale);
        s.dyn.addAnimation((Animated) cam);
        surface.start(this);
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
    final ArrayDeque<RectFloat2D> zoomStack = new ArrayDeque();
    static final int ZOOM_STACK_MAX = 8; //FOR safety

    @Override
    public void zoom(float x, float y, float sx, float sy) {

        synchronized (zoomStack) {
            ///if (!zoomStack.isEmpty()) {
//                if (!zoomStack.getLast().contains(nextZoom)) {
//                    zoomStack.clear();
/*                } else */ if (zoomStack.size() > ZOOM_STACK_MAX) {
                    zoomStack.removeFirst();
                }
            //}
            float s = scale.x;
            RectFloat2D curZoom = new RectFloat2D(cam.x - s / 2, cam.y - s / 2, cam.x + s / 2, cam.y + s / 2);
            zoomStack.addLast(curZoom);



            cam.set(x, y);
            float s0 = Math.max(sx, sy) * (1 + zoomMargin);
            scale(W / s0, H / s0);

        }

    }

    @Override
    public void unzoom() {
        synchronized (zoomStack) {
            if (!zoomStack.isEmpty()) {
                RectFloat2D z = zoomStack.removeLast();
                scale(z.w(), z.h());
                cam.set((float) z.center(0), (float) z.center(1));
            }
        }
    }

    @Override
    public Ortho scale(float sx, float sy) {
        float s = Math.max(sx, sy);
        scale.set(s, s);
        return this;
    }


    @Override
    public float w() {
        return W;
    }

    @Override
    public float h() {
        return H;
    }

    @Override
    public void windowResized(WindowEvent e) {
        W = window.getWidth();
        H = window.getHeight();
        resized();
    }


    @Override
    public Surface pos(float x1, float y1, float x2, float y2) {
        throw new UnsupportedOperationException();
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
    public synchronized void stop() {
        surface.stop();
    }

    @Override
    public void windowGainedFocus(WindowEvent e) {

    }

    @Override
    public void windowLostFocus(WindowEvent e) {
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

    private void updateMouse(MouseEvent e) {
        updateMouse(e, e != null ? e.getButtonsDown() : null);
    }


    private boolean updateMouse(MouseEvent e, short[] buttonsDown) {
        float x, y;


        if (e != null) {

            //screen coordinates
            float sx = e.getX();
            float sy = H - e.getY();

            wmx = +cam.x + (-0.5f * W + sx) / scale.x;
            wmy = +cam.y + (-0.5f * H + sy) / scale.x;

            updateMouse(e, sx, sy, buttonsDown);
            return true;

        } else {

            updateMouse(null, wmx, wmy, null);

            return false;
        }
    }

    public Surface updateMouse(@Nullable MouseEvent e, float sx, float sy, short[] buttonsDown) {

        if (e != null) {
            if (window != null) {
                if (window.window != null) {
                    Finger.pointer.set(window.windowX + e.getX(), window.windowY + e.getY());
                }
            }

        }

        /*if (e == null) {
            off();
        } else {*/
        Surface s;

        //System.out.println(lx + " " + ly);
        //if (lx >= 0 && ly >= 0 && lx <= 1f && ly <= 1f) {
        if ((s = finger.on(sx, sy, wmx, wmy, buttonsDown)) != null) {
            log("on", s);
            if (e != null)
                e.setConsumed(true);
            return s;
        } else {
            return null;
        }
    }

    @Override
    protected void paint(GL2 gl, int dtMS) {


        float sx = scale.x;
        //float sy = scale.y;
        gl.glTranslatef(W / 2f, H / 2f, 0);
        gl.glScalef(sx, sx, 1);
        gl.glTranslatef(-cam.x, -cam.y, 0);
        //gl.glTranslatef((sx) * -cam.x, sy * -cam.y, 0);

        surface.render(gl, dtMS);
    }

    protected void resized() {

        surface.pos(0, 0, W, H);

        scale(1, 1);
        cam.set(W / 2f, H / 2f);

        layout();

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
