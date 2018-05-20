package spacegraph.space2d.hud;

import com.jogamp.newt.event.*;
import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.event.On;
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

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static java.lang.Math.sin;
import static org.eclipse.collections.impl.tuple.Tuples.pair;

/**
 * orthographic widget adapter. something which goes on the "face" of a HUD ("head"s-up-display)
 */
public class Ortho extends Container implements SurfaceRoot, WindowListener, MouseListener, KeyListener {

//    public final AnimVector2f scale;
//    public float scaleMin = 0.25f;
//    public float scaleMax = 10000f;

    public final Finger finger;


    float camZmin = 20;
    float camZmax = 64000;

    public Surface surface;
    public JoglSpace window;
    public final v3 cam;

    /** current view area, in absolute world coords */
    public final v2 scale = new v2(1,1);



    private final Runnable fingerUpdate;


    public final AtomicBoolean focused = new AtomicBoolean(false);

    final Map<String, Pair<Object, Runnable>> singletons = new HashMap();

    private final Set<Surface> overlays = new CopyOnWriteArraySet<>();

    private float pixelRes;
    final static float focusAngle = (float) Math.toRadians(45);

    public Ortho() {
        this(new EmptySurface());
    }

    public Ortho(Surface content) {
        super();
        this.finger = new Finger();

//        //TODO use AnimVector3f to represent scale via bounds
//        this.scale = new AnimVector2f(1, 1, 4f) {
//          //TODO add animation ifChanged -> fingerUpdated
//        };

        this.cam = new AnimVector3f(1) {

            float CAM_RATE = 2f;

            {
                setDirect(0,0, (camZmin + camZmax)/2);
            }

            @Override
            public boolean animate(float dt) {
                if (super.animate(dt)) {
                    float W = bounds.w;
                    float H = bounds.h;
                    speed.set(Math.max(W, H) * CAM_RATE);
                    return true;
                }
                return false;
            }


            @Override
            public void set(float x, float y, float z) {
                super.set(x, y, Util.clamp(z, camZmin, camZmax));
            }

            @Override
            public void setDirect(float x, float y, float z) {
                super.setDirect(x, y, Util.clamp(z, camZmin, camZmax));
            }
        };



        this.surface = content;

        this.fingerUpdate = ()->{
            if (focused.get())
                finger();
        };
    }



    @Override
    public void windowResized(WindowEvent e) {
        if (autoresize()) {
            //re-maximize
            int ww = window.getWidth();
            int hh = window.getHeight();
            pos(0, 0, ww, hh); //triggers layout
        } else {
            layout(); //only layout
        }
    }


    public boolean focused() {
        return focused.get();
    }

    @Override
    public void prePaint(int dtMS) {

        float W = window.getWidth();
        float H = window.getHeight();

        pixelRes = Math.max(W, H);

        //scale(camZ) = pixelRes * sin(pi/2 - focus) / (camZ * sin(focus))
        //        float scale =  (float) (pixelRes / ((sin(focusAngle / 2) * cam.z) / sin(Math.PI / 2 - focusAngle / 2)));
        float scale =  (float) (pixelRes * sin(Math.PI/2 - focusAngle/2) / (cam.z * sin(focusAngle/2)));

        this.scale.set(scale,scale); //TODO use right aspect ratio
        //System.out.println(cam + " "  + this.scale + " "  + scale);

        super.prePaint(dtMS);
    }

    @Override
    protected void doLayout(int dtMS) {
        if (autoresize())
            surface.pos(bounds);

        cam.set(bounds.w / 2f, bounds.h / 2f, targetDepth(Math.max(bounds.w, bounds.h)));




        //fingerMoved.set(true);
    }

    public boolean autoresize() {
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
    public On onUpdate(Consumer<JoglWindow> c) {
        return window.onUpdate(c);
    }
    public On animate(Animated c) {
        return window.onUpdate(c);
    }
    public On animate(Runnable c) {
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
            if (window.window.hasFocus())
                mouseEntered(null);

            s.addMouseListenerPre(this);

            s.addKeyListener(this);

            animate((Animated) cam);
            animate(fingerUpdate);

            windowResized(null);

            assert(surface.parent==null);
            Exe.invokeLater(()->surface.start(this));
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
        zoom(b.cx(), b.cy(), b.w, b.h, zoomMargin);
    }

    float targetDepth(float viewDiameter) {
        float targetDepth = (float) ((viewDiameter * sin(Math.PI / 2 - focusAngle / 2)) / sin(focusAngle / 2));
        //System.out.println("viewdiameter " + viewDiameter + " targetDepth=" + targetDepth);
        return targetDepth;
    }

    public void zoom(float diameter) {
        zoom(cam.x, cam.y, diameter);
    }
//    public void zoomRelative(float diameterChange) {
//        zoom(cam.x, cam.y, diameterChange * );
//    }

    public void zoom(float x, float y, float sx, float sy, float margin) {
        zoom(x, y, Math.max(sx,sy)*(1+margin));
    }

    public void zoom(float x, float y, float viewDiameter) {
        //float s0 = (1 + margin);
        cam.set(x, y, targetDepth(viewDiameter));
        //cam.setLerp(x, y, 0, speed);
        //scale(w() / (sx * s0), h() / (sy * s0));
    }

//    @Override
//    public void unzoom() {
//        synchronized (zoomStack) {
//            if (!zoomStack.isEmpty()) {
//                RectFloat2D z = zoomStack.removeLast().get();
//                scale(z.w, z.h);
//                cam.set(z.cx(), z.cy());
//            }
//        }
//    }
//
//    public Ortho scale(float sx, float sy) {
//        float s = Util.clamp(Math.min(sx, sy), scaleMin, scaleMax);
//        scale.set(s, s);
//        return this;
//    }




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
            assert(surface.parent == this);
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
        if (t !=null) {
            if (!t.tryKey(e, pressOrRelease))
                e.setConsumed(true);
        }

    }

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mouseEntered(@Nullable MouseEvent e) {
        if (focused.compareAndSet(false,true)) {
            finger.enter();
            if (e!=null)
                update(true, e);
        }
    }

    @Override
    public void mouseExited(MouseEvent e) {
        if (focused.compareAndSet(true,false)) {
            update(false, null);
            finger.exit();
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
//        if (e.isConsumed())
//            return;
        if (update(false, e, e.getButtonsDown())) {
            if (finger.touching.get() !=null)
                e.setConsumed(true);
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
//        if (e.isConsumed())
//            return;

        short[] bd = e.getButtonsDown();

        //invert to negative value to indicate release
        for (int i = 0, bdLength = bd.length; i < bdLength; i++)
            bd[i] = (short) -bd[i];

        update(false, e, bd);

        if (finger.touching.get() !=null)
            e.setConsumed(true);

    }


    @Override
    public void mouseDragged(MouseEvent e) {
//        if (e.isConsumed())
//            return;
        if (update(true, e))
            if (finger.touching.get() !=null)
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
            JoglSpace w = this.window;

            int pmx = e.getX();
            int pmy = w.window.getHeight() - e.getY();
            float wmx = +cam.x + (-0.5f * w() + pmx) / scale.x;
            float wmy = +cam.y + (-0.5f * h() + pmy) / scale.y;

            finger.posPixel.set(pmx, pmy);
            finger.posScreen.set(w.window.getX() + pmx, w.window.getY() + pmy);
            finger.pos.set(wmx, wmy);
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
    protected Surface finger() {

        /*if (e == null) {
            off();
        } else {*/

        assert(focused());

        finger.update();

        return finger.on(surface);
    }



    @Override
    protected void paintBelow(GL2 gl) {


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
