package spacegraph.input.finger;

import com.jogamp.opengl.GL2;
import jcog.data.atomic.AtomicFloat;
import jcog.data.bit.AtomicMetalBitSet;
import jcog.tree.rtree.rect.RectFloat2D;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRender;
import spacegraph.space2d.widget.windo.Widget;
import spacegraph.util.math.v2;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * gestural generalization of mouse cursor's (or touchpad's, etc)
 * possible intersection with a surface and/or its sub-surfaces.
 * <p>
 * tracks state changes and signals widgets of these
 */
public class Finger {


    private final static int MAX_BUTTONS = 5;

    private final static float DRAG_THRESHOLD_PIXELS = 3f;

    public final v2 pos = new v2(), posPixel = new v2(), posScreen = new v2();


    /**
     * last local and global positions on press (downstroke).
     * TODO is it helpful to also track upstroke position?
     */
    private final v2[] pressPos = new v2[MAX_BUTTONS];
    protected final v2[] pressPosPixel = new v2[MAX_BUTTONS];

    {
        for (int i = 0; i < MAX_BUTTONS; i++) {
            pressPos[i] = new v2(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
            pressPosPixel[i] = new v2(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
        }
    }

    private final AtomicMetalBitSet buttonDown = new AtomicMetalBitSet();
    public final AtomicMetalBitSet prevButtonDown = new AtomicMetalBitSet();

    /**
     * exclusive state which may be requested by a surface
     */
    private final AtomicReference<Fingering> fingering = new AtomicReference<>(Fingering.Null);

    /**
     * widget above which this finger currently hovers
     */
    public final AtomicReference<Widget> touching = new AtomicReference<>();
    private boolean focused = false;


    public Finger() {

    }

    public static Predicate<Finger> clicked(int button, Runnable clicked) {
        return clicked(button, clicked, null, null, null);
    }

    private static Predicate<Finger> clicked(int button, Runnable clicked, Runnable armed, Runnable hover, Runnable becameIdle) {
        return clicked(button, (f) -> clicked.run(), armed, hover, becameIdle);
    }

    public static Predicate<Finger> clicked(int button, Consumer<Finger> clicked, Runnable armed, Runnable hover, Runnable becameIdle) {

        final boolean[] idle = {true};

        if (becameIdle != null)
            becameIdle.run();

        return (finger) -> {

            Surface what;
            if (finger != null && (what = finger.touching()) != null) {
                if ((clicked != null) && finger.clickedNow(button, what)) {



                    clicked.accept(finger);

                    return true;
                } else if ((armed != null) && finger.pressing(button)) {
                    armed.run();
                    return true;
                } else {
                    if (hover != null) hover.run();
                }
                idle[0] = false;
            } else {
                if (becameIdle != null && !idle[0]) {
                    becameIdle.run();
                    idle[0] = true;
                }
            }
            return false;
        };
    }

    public Surface touching() {
        return touching.get();
    }

    private static v2 relative(v2 x, Surface c) {
        v2 y = new v2(x);
        RectFloat2D b = c.bounds;
        y.sub(b.x, b.y);
        y.scaled(1f / b.w, 1f / b.h);
        return y;
    }

    /**
     * call when finger exits the window / screen, the window becomes unfingerable, etc..
     */
    public void exit() {
        focused = false;
    }

    /**
     * call when finger enters the window
     */
    public void enter() {
        focused = true;
    }

    /**
     * synch update: should be called periodically during update loop
     * for handling that involves the real timing of events
     */
    public void update() {


        Widget t = this.touching.get();
        if (t != null) {
            t.onFinger(this);
        }



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
                pressPos[b].set(pos);
                pressPosPixel[b].set(posPixel);
            }
        }

        //System.out.println(buttonSummary());
    }

    public Surface on(Surface root) {

        Fingering ff = this.fingering.get();
//        Fingering f0 = ff;
        Surface touchedNext;


        if (ff == Fingering.Null || ff.escapes()) {
            touchedNext = root.finger(this);
        } else {
            touchedNext = touching.get();
        }


        for (AtomicFloat r : rotation)
            r.getAndZero();


        on(touchedNext instanceof Widget ? (Widget) touchedNext : null);

        if (ff != Fingering.Null) {

            if (!ff.update(this)) {
                ff.stop(this);
                fingering.set(Fingering.Null);
            }

        }

        return touchedNext;
    }

    private boolean dragging(int button) {
        v2 g = pressPosPixel[button];
        return (g.distanceSq(posPixel) > DRAG_THRESHOLD_PIXELS * DRAG_THRESHOLD_PIXELS);
    }

    private void on(@Nullable Widget touchNext) {

        @Nullable Widget touchPrev = touching.getAndSet(touchNext);

        if (touchPrev == touchNext)
            return;

        if (touchPrev != null) {
            touchPrev.onFinger(null);
        }

        if (touchNext != null) {
            touchNext.onFinger(this);
        }
    }

    /**
     * allows a fingered object to push the finger off it
     */
    public boolean off(Object fingered) {
        Widget t = this.touching.get();
        if (t != null && t == fingered) {
            on(null);
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


    public boolean releasedNow(int button, Surface c) {
        return releasedNow(button) && Finger.relative(pressPos[button], c).inUnit();
    }

    /**
     * additionally tests for no dragging while pressed
     */
    public boolean clickedNow(int button) {
        return releasedNow(button) && !dragging(button);
    }

    public boolean clickedNow(int button, Surface c) {

//        System.out.println(pressing(i) + "<-" + wasPressed(i));

        if (clickedNow(button)) {
            v2 where = pressPos[button];
            if (Finger.relative(where, c).inUnit()) {
                commitButton(button); //absorb the event
                return true;
            }
        }

        return false;
    }


    /**
     * acquire an exclusive fingering state
     */
    public final boolean tryFingering(Fingering f) {

        /*if (f != null) */
        {
            Fingering cf = this.fingering.get();
            if (cf != f && cf.defer(this)) {
                //System.out.println(cf + " -> " + f + " try");
                if (f.start(this)) {
                    //System.out.println(cf + " -> " + f + " start");
                    if (this.fingering.compareAndSet(cf, f)) {

                        //System.out.println(cf + " -> " + f + " acquire");
                        cf.stop(this);

                        @Nullable FingerRenderer r = f.renderer();
                        if (r != null)
                            renderer = r;
                        return true;

                    } else {
                        f.stop(this);
                    }
                }
            }
        }


        return false;
    }

    public boolean isFingering() {
        return fingering.get() != Fingering.Null;
    }

    public v2 relativePos(Surface c) {
        return relative(pos, c);
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
                this.rotation[i].set(r); //add(r);
        }
    }

    public float rotationX() {
        return rotation[0].floatValue();
    }

    public float rotationY() {
        return rotationY(true);
    }

    public float rotationY(boolean absorb) {
        AtomicFloat r = rotation[1];
        return absorb ? r.getAndSet(0) : r.get();
    }

    public float rotationZ() {
        return rotation[2].floatValue();
    }

    final FingerRenderer rendererDefault = FingerRenderer.polygon1;

    volatile FingerRenderer renderer = rendererDefault;

    /**
     * visual overlay representation of the Finger; ie. cursor
     */
    public Surface layer() {
        return new FingerRendererSurface();
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
            if (focused)
                renderer.paint(posPixel, Finger.this, surfaceRender.dtMS, gl);
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
