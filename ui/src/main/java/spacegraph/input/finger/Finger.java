package spacegraph.input.finger;

import com.jogamp.nativewindow.util.Point;
import jcog.data.bit.AtomicMetalBitSet;
import jcog.tree.rtree.rect.RectFloat2D;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.Surface;
import spacegraph.space2d.widget.windo.Widget;
import spacegraph.util.math.v2;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

/**
 * gestural generalization of mouse cursor's (or touchpad's, etc)
 * possible intersection with a surface and/or its sub-surfaces.
 * <p>
 * tracks state changes and signals widgets of these
 */
public class Finger {

    /**
     * global pointer screen coordinate, set by window the (main) cursor was last active in
     */
    public final static Point pointer = new Point();
    final static int MAX_BUTTONS = 5;
    /**
     * TODO scale this to pixel coordinates, this spatial coordinate is tricky and resolution dependent anyway
     */
    final static float DRAG_THRESHOLD = 3f;
    public final v2 pos = new v2(), posGlobal = new v2();
    public final v2[] hitOnDown = new v2[MAX_BUTTONS], hitOnDownGlobal = new v2[MAX_BUTTONS];
    public final AtomicMetalBitSet buttonDown = new AtomicMetalBitSet();
    public final AtomicMetalBitSet prevButtonDown = new AtomicMetalBitSet();

    //TODO wheel state
    /**
     * exclusive state which may be requested by a surface
     */
    private final AtomicReference<Fingering> fingering = new AtomicReference<>();
    /**
     * dummy intermediate placeholder state
     */
    private final Fingering STARTING = new Fingering() {

        @Override
        public boolean start(Finger f) {
            return false;
        }

        @Override
        public boolean update(Finger f) {
            return true;
        }
    };
    /**
     * widget above which this finger currently hovers
     */
    public @Nullable Widget touching;


    public Finger() {

    }

    public static Predicate<Finger> clicked(int button, Runnable clicked) {
        return clicked(button, clicked, null, null, null);
    }

    public static Predicate<Finger> clicked(int button, Runnable clicked, Runnable armed, Runnable hover, Runnable becameIdle) {

        final boolean[] idle = {true};

        if (becameIdle != null)
            becameIdle.run();

        return (finger) -> {

            Surface what;
            if (finger != null && (what = finger.touching) != null) {
                if ((clicked != null) && finger.clickedNow(button, what)) {
                    clicked.run();
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

    public static v2 relative(v2 x, Surface c) {
        v2 y = new v2(x);
        RectFloat2D b = c.bounds;
        y.sub(b.x, b.y);
        y.scaled(1f / b.w, 1f / b.h);
        return y;
    }

    /**
     * synch update: should be called periodically during update loop
     * for handling that involves the real timing of events
     */
    public void update() {

        Widget t = this.touching;
        if (t != null) {
            t.onFinger(this);
        }

        for (int b = 0;  b< MAX_BUTTONS; b++) {
            boolean wasPressed = wasPressed(b);
            boolean pressed = pressing(b);
            if (!wasPressed && pressed) {
                hitOnDown[b] = new v2(pos);
                hitOnDownGlobal[b] = new v2(posGlobal);
            } else if (wasPressed && !pressed) {
                hitOnDownGlobal[b] = hitOnDown[b] = null; //release
            }
        }


        //finally:
        prevButtonDown.copyFrom(buttonDown);
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


        //System.out.println(Arrays.toString(nextButtonDown));
        for (short b : nextButtonDown) {

            boolean pressed = (b > 0);

            if (!pressed) b = (short) -b; //invert to positive
            b--; //shift to actual button ID

            buttonDown.set(b, pressed);

        }

    }

    public Surface on(Surface root) {

        Fingering ff = this.fingering.get();
        Fingering f0 = ff;
        Surface touchedNext;

        try {

            //START DESCENT:

            if (ff == null || ff.escapes()) {

                touchedNext = root.tryTouch(this);

            } else {
                touchedNext = touching;
            }

            //System.out.println(pos + " " + posGlobal + " " + ff + " " + touchedNext);

            if (ff != null) {

                if (!ff.update(this)) {
                    ff.stop(this);
                    ff = null;
                }

            }


        } finally {

            if (ff == null)
                fingering.compareAndSet(f0, null);

        }

        on(touchedNext instanceof Widget ? (Widget) touchedNext : null);

        return touchedNext;
    }

    public boolean dragging(int button) {
        return (hitOnDownGlobal[button] != null && hitOnDownGlobal[button].distanceSq(posGlobal) > DRAG_THRESHOLD * DRAG_THRESHOLD);
    }

    private boolean on(@Nullable Widget touched) {

        if (touched != touching && touching != null) {
            touching.onFinger(null);
        }

        if ((touching = touched) != null) {
            touching.onFinger(this);
            return true;
        } else {
            return false;
        }
    }

    public boolean off() {
        if (touching != null) {
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

    public boolean pressedNow(int button) {
        return pressing(button) && !wasPressed(button);
    }

    public boolean wasPressed(int button) {
        return prevButtonDown.get(button);
    }

    public boolean wasReleased(int button) {
        return !wasPressed(button);
    }

    public boolean releasedNow(int button) {
        return !pressing(button) && wasPressed(button);
    }

    public boolean releasedNow(int i, Surface c) {
        return releasedNow(i) && (c != null && hitOnDown[i] != null && Finger.relative(hitOnDown[i], c).inUnit());
    }

    /**
     * additionally tests for no dragging while pressed
     */
    public boolean clickedNow(int button) {
        boolean clicked = releasedNow(button);
        boolean notDragging = !dragging(button);
        //System.out.println("released: " + clicked + " notDragging: " + notDragging);
        return clicked && notDragging;
    }

    public boolean clickedNow(int i, Surface c) {
        if (clickedNow(i)) {
            v2 where = hitOnDown[i];
            if (where != null)
                if (Finger.relative(where, c).inUnit())
                    return true;
        }
        return false;
    }

    /**
     * acquire an exclusive fingering state
     */
    public boolean tryFingering(Fingering f) {


        if (f != null && fingering.compareAndSet(null, STARTING)) {
            if (f.start(this)) {
                fingering.set(f);
                //root.surface.onTouch(this, ArrayUtils.EMPTY_SHORT_ARRAY); //release all fingering on surfaces
                return true;
            } else {
                fingering.set(null);
                return false;
            }
        }


        return false;
    }

    public boolean isFingering() {
        return fingering.get() != null;
    }

    public v2 relativePos(Surface c) {
        return relative(pos, c);
    }

}
