package spacegraph.input.finger;

import com.jogamp.opengl.GL2;
import jcog.data.atomic.AtomicFloat;
import jcog.data.bit.AtomicMetalBitSet;
import jcog.math.v2;
import jcog.tree.rtree.rect.RectFloat;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRender;
import spacegraph.space2d.hud.Ortho;
import spacegraph.space2d.hud.SurfaceHiliteOverlay;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * gestural generalization of mouse cursor's (or touchpad's, etc)
 * possible intersection with a surface and/or its sub-surfaces.
 * <p>
 * tracks state changes and signals widgets of these
 */
abstract public class Finger {


    private final int buttons;

    /** drag threshold (in screen pixels) */
    float dragThresholdPx = 5f;

    public final v2 posPixel = new v2(), posScreen = new v2();



    /**
     * last local and global positions on press (downstroke).
     * TODO is it helpful to also track upstroke position?
     */
    public final v2[] pressPosPixel;



    private final AtomicMetalBitSet buttonDown = new AtomicMetalBitSet();
    public final AtomicMetalBitSet prevButtonDown = new AtomicMetalBitSet();

    /**
     * exclusive state which may be requested by a surface
     */
    protected final AtomicReference<Fingering> fingering = new AtomicReference<>(Fingering.Null);

    /**
     * widget above which this finger currently hovers
     */
    public final AtomicReference<Surface> touching = new AtomicReference<>();

    protected final AtomicBoolean focused = new AtomicBoolean(false);


    protected Finger(int buttons) {
        assert(buttons<32);
        this.buttons = buttons;
        pressPosPixel = new v2[this.buttons];
        for (int i = 0; i < this.buttons; i++) {
            pressPosPixel[i] = new v2(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
        }
    }


    public static Predicate<Finger> clicked(Surface what, int button, Runnable clicked) {
        return clicked(what, button, clicked, null, null, null);
    }

    private static Predicate<Finger> clicked(Surface what, int button, Runnable clicked, Runnable armed, Runnable hover, Runnable becameIdle) {
        return clicked(what, button, (f) -> clicked.run(), armed, hover, becameIdle);
    }

    public static Predicate<Finger> clicked(Surface what, int button, Consumer<Finger> clicked, Runnable armed, Runnable hover, Runnable becameIdle) {

        if (becameIdle != null)
            becameIdle.run();

        final AtomicBoolean idle = new AtomicBoolean(false);

        return f -> {

            if (f != null /*&& (what = f.touching()) != null*/) {

                idle.set(false);

                if (f.clickedNow(button, what)) {

                    if (clicked != null)
                        clicked.accept(f);

                } else if (f.pressing(button) ) {
                    if (armed!=null)
                        armed.run();

                } else {
                    if (hover != null)
                        hover.run();
                }

            } else {
                if (idle.compareAndSet(false, true)) {
                    if (becameIdle != null)
                        becameIdle.run();
                }
            }
            return false;
        };
    }

    public Surface touching() {
        return touching.getOpaque();
    }

    private v2 posGlobal(v2 x, Ortho o) {
        return o.cam.screenToWorld(x);
    }

    /**
     * call when finger exits the window / screen, the window becomes unfingerable, etc..
     */
    public void exit() {
        focused.set(false);
    }

    /**
     * call when finger enters the window
     */
    public void enter() {
        focused.set(true);
    }

    /** commit all buttons */
    private void commitButtons() {
        prevButtonDown.copyFrom(buttonDown);
    }

    /** commit one button */
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
    public void update(short[] nextButtonDown) {

        commitButtons();

        for (short b : nextButtonDown) {

            boolean pressed = (b > 0);

            if (!pressed) b = (short) -b;
            b--;

            buttonDown.set(b, pressed);

            if (pressed && !wasPressed(b)) {
                pressPosPixel[b].set(posPixel);
            }
        }

        //System.out.println(buttonSummary());
    }

    /** call once per frame */
    public final void clearRotation() {
        for (AtomicFloat r : rotation)
            r.getAndZero();
    }


    protected Surface touching(Surface next) {
        Surface prev = touching.getAndSet(next);
        if (prev!=next) {
            if (prev!=null)
                prev.fingerTouch(this, false);

            if (next!=null)
                next.fingerTouch(this, true);
        }
        return prev;
    }

    private boolean _dragging(int button) {
        v2 g = pressPosPixel[button];
        return (g.distanceSq(posPixel) > dragThresholdPx * dragThresholdPx);
    }

    public boolean dragging(int button) {
        return pressedNow(button) && !releasedNow(button) && _dragging(button);
    }



    //final static v2 OOB = new v2(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);

//    public v2 relativePos(v2 screen, Surface x) {
//
//        Ortho root = ortho; //(Ortho) x.root();
//        if (root!=null)
//            return relative(root.cam.screenToWorld(screen), x);
//        else
//            return OOB;
//
//
//    }
    /**
     * allows a fingered object to push the finger off it
     */
    public boolean off(Surface fingered) {
        if (touching.compareAndSet(fingered, null)) {
            fingered.fingerTouch(this, false);
            return true;
        }
        return false;
    }

    public boolean released(int button) {
        return !pressing(button);
    }

    public boolean pressing(int button) {
        return buttonDown.get(button);
    }


    private boolean wasPressed(int button) {
        return prevButtonDown.get(button);
    }

    public boolean wasReleased(int button) {
        return !wasPressed(button);
    }

    public boolean releasedNow(int button) {
        return !pressing(button) && wasPressed(button);
    }

    public boolean pressedNow(int button) {
        return pressing(button) && !wasPressed(button);
    }

//
//    public boolean releasedNow(int button, Surface c) {
//        return releasedNow(button) && relativePos(pressPosPixel[button], c).inUnit();
//    }



    /**
     * additionally tests for no dragging while pressed
     */
    public final boolean clickedNow(int button) {
        return clickedNow(button, null);
    }

    public boolean clickedNow(int button, @Nullable Surface c) {

//        System.out.println(pressing(i) + "<-" + wasPressed(i));

        if (releasedNow(button) && !_dragging(button)) {
            if (c==null || c.bounds.contains(posGlobal(c))) {
                commitButton(button); //absorb the event
                return true;
            }
        }

        return false;
    }


    /**
     * acquire an exclusive fingering state
     */
    public final boolean tryFingering(Fingering next) {

        Fingering prev = this.fingering.get();

        if (prev != next && prev.defer(this)) {

            //System.out.println(cf + " -> " + f + " try");

            if (next.start(this)) {

                //System.out.println(cf + " -> " + f + " start");

                if (this.fingering.compareAndSet(prev, next)) {

                    //System.out.println(cf + " -> " + f + " acquire");

                    prev.stop(this);

                    @Nullable FingerRenderer r = next.renderer();
                    renderer = (r != null) ? r : rendererDefault;

                    return true;

                } else {
                    next.stop(this);
                }
            }
        }


        return false;
    }

//    public boolean isFingering() {
//        return fingering.get() != Fingering.Null;
//    }



    public v2 posGlobal(Surface c) {
        Ortho orthoParent = c.parent(Ortho.class);
        if (orthoParent!=null)
            return posGlobal( posPixel, orthoParent);
        else
            return posPixel.clone();
    }


    private final AtomicFloat[] rotation = new AtomicFloat[3];

    {
        rotation[0] = new AtomicFloat();
        rotation[1] = new AtomicFloat();
        rotation[2] = new AtomicFloat();
    }

    public void rotationAdd(float[] next) {
        for (int i = 0; i < next.length; i++) {
            float r = next[i];
            if (r != 0)
                this.rotation[i].set(r);
        }
    }

    public float rotationX() {
        return rotation[0].floatValue();
    }

    public float rotationY(boolean absorb) {
        AtomicFloat r = rotation[1];
        return absorb ? r.getAndSet(0) : r.get();
    }

    public float rotationZ() {
        return rotation[2].floatValue();
    }

    final FingerRenderer rendererDefault =
            FingerRenderer.rendererCrossHairs1;
            //FingerRenderer.polygon1;

    volatile FingerRenderer renderer = rendererDefault;

    /**
     * visual overlay representation of the Finger; ie. cursor
     */
    public Surface cursorSurface() {
        return new FingerRendererSurface();
    }

    public Surface zoomBoundsSurface(Ortho.Camera cam) {
        return new FingerZoomBoundsSurface(cam);
    }

    public boolean focused() {
        return focused.getOpaque();
    }

    public static v2 posRelative(v2 p, Surface s) {
        v2 y = new v2(p);
        RectFloat b = s.bounds;
        y.sub(b.x, b.y);
        y.scaled(1f / b.w, 1f / b.h);
        return y;
    }

    public v2 posRelative(Surface s) {
        return posRelative(posGlobal(s), s);
    }


    /**
     * HACK marker interface for surfaces which absorb wheel motion, to prevent other system handling from it (ex: camera zoom)
     */
    public interface WheelAbsorb {
    }

    private final class FingerRendererSurface extends Surface {
        {
            clipBounds = false;
        }

        @Override
        protected void paint(GL2 gl, SurfaceRender surfaceRender) {
            if (focused()) {
                renderer.paint(posPixel, Finger.this, surfaceRender.dtMS, gl);

                //for debugging:
//                if (ortho!=null) {
//                    renderer.paint(
//                        ortho.cam.worldToScreen(ortho.cam.screenToWorld(posPixel)),
//                            Finger.this, surfaceRender.dtMS, gl);
//                }
            }
        }
    }
    private final class FingerZoomBoundsSurface extends SurfaceHiliteOverlay {

        public FingerZoomBoundsSurface(Ortho.Camera cam) {
            super(cam);
        }


        @Override protected boolean enabled() {
            return focused();
        }

        @Override protected Surface target() {
            Surface s = touching();
            if (s!=null) {
                //color HASH
                //color.setAt()
            }
            return s;
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
}
