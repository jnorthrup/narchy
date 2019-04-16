package spacegraph.input.finger;

import com.jogamp.opengl.GL2;
import jcog.TODO;
import jcog.data.atomic.AtomicFloat;
import jcog.data.bit.AtomicMetalBitSet;
import jcog.data.list.FasterList;
import jcog.math.v2;
import jcog.tree.rtree.rect.RectFloat;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.PaintSurface;
import spacegraph.space2d.hud.Overlay;
import spacegraph.space2d.hud.Zoomed;
import spacegraph.util.SurfaceTransform;
import spacegraph.video.JoglWindow;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Predicate;

import static spacegraph.input.finger.Fingering.Null;

/**
 * gestural generalization of mouse cursor's (or touchpad's, etc)
 * possible intersection with a surface and/or its sub-surfaces.
 * <p>
 * tracks state changes and signals widgets of these
 * <p>
 * TODO differentiate this between subclasses which touch only one surface at a time, and those which can touch several (multi-select)
 */
abstract public class Finger implements Predicate<Fingering> {


    public final v2 posPixel = new v2(), posScreen = new v2();
    public final AtomicMetalBitSet prevButtonDown = new AtomicMetalBitSet();
    /**
     * widget above which this finger currently hovers
     */
    protected final AtomicReference<Surface> touching = new AtomicReference<>();
    /**
     * a exclusive locking/latching state which may be requested by a surface
     */
    protected final AtomicReference<Fingering> fingering = new AtomicReference<>(Null);
    /**
     * ex: true when finger enters the window, false when it leaves
     */
    protected final AtomicBoolean active = new AtomicBoolean(false);
    protected final v2 _posGlobal = new v2();
    /**
     * last local and global positions on press (downstroke).
     * TODO is it helpful to also track upstroke position?
     */
    public final v2[] pressPosPixel;
    public final FingerRenderer rendererDefault =
            FingerRenderer.rendererCrossHairs1;
    final FasterList<SurfaceTransform> transforms = new FasterList();
    private final int buttons;

    /**
     * drag threshold (in screen pixels)
     */
    private final float dragThresholdPx = 5f;

    //@Deprecated protected transient Function<v2, v2> _screenToGlobalRect;
    private final AtomicMetalBitSet buttonDown = new AtomicMetalBitSet();
    private final AtomicFloat[] rotation = new AtomicFloat[3];

    public volatile FingerRenderer renderer = rendererDefault;

    {
        rotation[0] = new AtomicFloat();
        rotation[1] = new AtomicFloat();
        rotation[2] = new AtomicFloat();
    }

    protected Finger(int buttons) {
        assert (buttons < 32);
        this.buttons = buttons;
        pressPosPixel = new v2[this.buttons];
        for (int i = 0; i < this.buttons; i++) {
            pressPosPixel[i] = new v2(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
        }
    }


    public static v2 normalize(v2 pPixel, JoglWindow win) {
        v2 y = new v2(pPixel);
        y.sub(win.getX(), win.getY());
        y.scale(1f / win.getWidth(), 1f / win.getHeight());
        return y;
    }

    public static v2 normalize(v2 p, RectFloat b) {
        v2 y = new v2(p);
        y.sub(b.x, b.y);
        y.scale(1f / b.w, 1f / b.h);
        return y;
    }

    public Surface touching() {
        return touching.getOpaque();
    }

    /**
     * call when finger exits the window / screen, the window becomes unfingerable, etc..
     */
    public void exit() {
        active.set(false);
    }

    /**
     * call when finger enters the window
     */
    public void enter() {
        active.set(true);
    }

    /**
     * commit all buttons
     */
    protected void commitButtons() {
        prevButtonDown.copyFrom(buttonDown);
    }

    /**
     * commit one button
     */
    private void commitButton(int button) {
        prevButtonDown.set(button, buttonDown.get(button));
    }

    public String buttonSummary() {
        return prevButtonDown.toBitString() + " -> " + buttonDown.toBitString();
    }

    /**
     * asynch updates: buttons and motion
     * if a surface is touched, calls its
     * event handler.  this could mean that there is either mouse
     * motion or button status has changed.
     */
    public void update(@Nullable short[] nextButtonDown) {

        if (nextButtonDown!=null) {
            for (short b : nextButtonDown) {

                boolean pressed = (b > 0);

                if (!pressed) b = (short) -b;
                b--;

                buttonDown.set(b, pressed);

                if (pressed && !wasPressed(b)) {
                    pressPosPixel[b].set(posPixel);
                }
            }
        }

//        System.out.println(buttonSummary());
    }

    /**
     * call once per frame
     */
    protected final void clearRotation() {
        for (AtomicFloat r : rotation)
            r.getAndZero();
    }

    private boolean _dragging(int button) {
        v2 g = pressPosPixel[button];
        return (g.distanceSq(posPixel) > dragThresholdPx * dragThresholdPx);
    }

    public boolean dragging(int button) {
        return pressed(button) && _dragging(button);
    }

    /**
     * allows a fingered object to push the finger off it
     */
    public boolean off(Surface fingered) {
        return touching.compareAndSet(fingered, null);
    }

    public boolean released(int button) {
        return !pressed(button);
    }

    public boolean pressed(int button) {
        return buttonDown.get(button);
    }

    private boolean wasPressed(int button) {
        return prevButtonDown.get(button);
    }

    public boolean wasReleased(int button) {
        return !wasPressed(button);
    }

    public boolean releasedNow(int button) {
        return !pressed(button) && wasPressed(button);
    }

    public boolean pressedNow(int button) {
        return pressed(button) && !wasPressed(button);
    }

    /**
     * additionally tests for no dragging while pressed
     */
    public final boolean clickedNow(int button) {
        return clickedNow(button, null);
    }

    public boolean clickedNow(int button, @Nullable Surface c) {

//        System.out.println(pressing(i) + "<-" + wasPressed(i));

        if (releasedNow(button) && !_dragging(button)) {
            if (c == null || intersects(c.bounds)) {
                commitButton(button); //absorb the event
                return true;
            }
        }

        return false;
    }

    /**
     * acquire an exclusive fingering state
     */
    @Override public final boolean test(Fingering next) {

        Fingering prev = this.fingering.get();

        if (prev != Null) {
            if (!prev.update(this)) {
                prev.stop(this);
                fingering.set(Null);
                renderer = rendererDefault;
                prev = Null;
            }
        }

        if (prev!=next) {


            if (prev.defer(this)) {


                if (next.start(this)) {

                    prev.stop(this);

                    fingering.set(next);

                    @Nullable FingerRenderer r = next.renderer(this);
                    renderer = (r != null) ? r : rendererDefault;

                    return true;
                }
            }
        }



        return false;
    }

    /**
     * warning: the vector instance returned by this and other methods are mutable.  so they may need to be cloned when accessed to record the state across time.
     */
    abstract public v2 posGlobal();
    //FingerRenderer.polygon1;

    protected void rotationAdd(float[] next) {
        for (int i = 0; i < next.length; i++) {
            float r = next[i];
            if (r != 0)
                this.rotation[i].set(r);
        }
    }

    public final float rotationX(boolean take) {
        return rotation(0, take);
    }

    public final float rotationY(boolean take) {
        return rotation(1, take);
    }

    public final float rotationZ(boolean take) {
        return rotation(2, take);
    }

    protected float rotation(int which, boolean take) {
        AtomicFloat r = rotation[which];
        return take ? r.getAndSet(0) : r.get();
    }

    /**
     * visual overlay representation of the Finger; ie. cursor
     */
    public Surface overlayCursor() {
        return new FingerRendererSurface();
    }

    private boolean focused() {
        return active.getOpaque();
    }

    public final v2 posRelative(Surface s) {
        return posRelative(s.bounds);
    }

    public v2 posRelative(RectFloat b) {
        //return normalize(posGlobal(), b);
//        RectFloat r = globalToPixel(b);
//        return new v2(
//        (posPixel.x - r.x)/r.w, (posPixel.y - r.y)/r.h
//        );

        v2 g = posGlobal();
        return new v2((g.x - b.x)/b.w, (g.y - b.y)/b.h);
    }


    public Fingering fingering() {
        return fingering.getOpaque();
    }

    public boolean intersects(RectFloat bounds) {
        //System.out.println(bounds + " contains " + posGlobal() + " ? " + bounds.contains(posGlobal()));
        //return globalToPixel(bounds).contains(posPixel);
        return posRelative(bounds).inUnit();
    }

    public RectFloat globalToPixel(RectFloat bounds) {
        int n = transforms.size();
        switch (n) {
            case 0:
                return bounds;
            case 1:
                return ((Zoomed.Camera) transforms.getLast()).globalToPixel(bounds);
            default:
                throw new TODO();
        }
    }


    public final <S extends Surface> S push(SurfaceTransform t, Function<Finger, S> fingering) {
        v2 p = _posGlobal.clone();
        transforms.add(t);
        try {


            _posGlobal.set(((Zoomed.Camera)t).pixelToGlobal(_posGlobal.x, _posGlobal.y));
            //System.out.println(p + " " + _posGlobal);

            S result = fingering.apply(this);

            return result;

        } finally {
            _posGlobal.set(p);
            transforms.removeLast();
        }
    }

    public static final class TouchOverlay extends Overlay {

        private final Finger f;

        public TouchOverlay(Finger f, Zoomed.Camera cam) {
//            this(f::touching, cam);
            super(cam);
            this.f = f;
        }

//        public ZoomBoundsOverlay(Supplier<Surface> touching, Zoomed.Camera cam) {
//            super(cam);
//            this.touching = touching;
//        }

        @Override
        protected boolean enabled() {
            return f.focused();
        }

        @Override
        protected Surface target() {
            Surface s = f.touching();
            if (s != null) {
                //color HASH
                //color.setAt()
            }
            return s;
        }

        @Override
        protected void paint(Surface t, GL2 gl, ReSurface reSurface) {
            drawBoundsFrame(t, gl);

            //paintCaption(t, reSurface, gl);

        }
    }

    private final class FingerRendererSurface extends PaintSurface {
        {
            clipBounds = false;
        }

        @Override
        protected void paint(GL2 gl, ReSurface reSurface) {
            if (focused()) {
                renderer.paint(posPixel, Finger.this, reSurface.dtS(), gl);

                //for debugging:
//                if (ortho!=null) {
//                    renderer.paint(
//                        ortho.cam.worldToScreen(ortho.cam.screenToWorld(posPixel)),
//                            Finger.this, surfaceRender.dtMS, gl);
//                }
            }
        }
    }

// /**
//     * dummy intermediate placeholder state
//     */
//    private final Fingering STARTING = new Fingering() {
//
//        @Override
//        public boolean start(Finger f) {
//            return false;
//        }
//
//        @Override
//        public boolean update(Finger f) {
//            return true;
//        }
//    };

    /**
     * marker interface for surfaces which absorb wheel motion, to prevent other system handling from it (ex: camera zoom)
     */
    public static interface ScrollWheelConsumer {
    }
}
