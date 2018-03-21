package spacegraph.input;

import com.jogamp.nativewindow.util.Point;
import jcog.tree.rtree.rect.RectFloat2D;
import org.jetbrains.annotations.Nullable;
import spacegraph.Surface;
import spacegraph.math.v2;
import spacegraph.widget.windo.Widget;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static java.lang.System.arraycopy;
import static java.util.Arrays.fill;

/**
 * gestural generalization of mouse cursor's (or touchpad's, etc)
 * possible intersection with a surface and/or its sub-surfaces.
 * <p>
 * tracks state changes and signals widgets of these
 */
public class Finger {

    /**
     * exclusive state which may be requested by a surface
     */
    private final AtomicReference<Fingering> fingering = new AtomicReference<>();

    /**
     * global pointer screen coordinate, set by window the (main) cursor was last active in
     */
    public final static Point pointer = new Point();

    public final v2 pos = new v2(), posGlobal = new v2();
    public final v2[] hitOnDown = new v2[5], hitOnDownGlobal = new v2[5];
    public final boolean[] buttonDown = new boolean[5];
    public final boolean[] prevButtonDown = new boolean[5];


    //TODO wheel state

    /**
     * widget above which this finger currently hovers
     */
    public @Nullable Widget touching;

    /**
     * TODO scale this to pixel coordinates, this spatial coordinate is tricky and resolution dependent anyway
     */
    final static float DRAG_THRESHOLD = 3f;


    public Finger() {

    }

    public synchronized Surface on(Surface root, float lx, float ly, short[] nextButtonDown) {

        Fingering ff = this.fingering.get();
        Fingering f0 = ff;
        Surface touchedNext;

        try {
            this.pos.set(lx, ly);

            arraycopy(this.buttonDown, 0, prevButtonDown, 0, buttonDown.length);

            fill(this.buttonDown, false);
            if (nextButtonDown != null) {
                for (short s : nextButtonDown) {
                    if (s > 0) //ignore -1 values
                        this.buttonDown[s - 1 /* start at zero=left button */] = true;
                }

                for (int j = 0, jj = hitOnDown.length; j < jj; j++) {
                    if (!prevButtonDown[j] && buttonDown[j]) {
                        hitOnDown[j] = new v2(pos);
                        hitOnDownGlobal[j] = new v2(posGlobal);
                    }
                }

            } else {
                Arrays.fill(hitOnDown, null);
            }


            //START DESCENT:

            if (ff == null || ff.escapes()) {

                touchedNext = root.onTouch(this, nextButtonDown);
                if (touchedNext instanceof Widget) {
                    if (!on((Widget) touchedNext))
                        touchedNext = null;
                } else {
                    touchedNext = null;
                }
            } else {
                touchedNext = null;
            }


            if (ff != null) {

                if (!ff.update(this)) {
                    ff.stop(this);
                    ff = null;
                }

            }


            for (int j = 0, jj = hitOnDown.length; j < jj; j++) {
                if (!buttonDown[j] && hitOnDown[j] != null) {
                    hitOnDown[j] = null; //release
                }
            }

            if (touching != touchedNext && touching != null) {
                touching.untouch();
                touching = null;
            }

        } finally {

            if (ff == null)
                fingering.compareAndSet(f0, null);

        }

        return touchedNext;
    }

    public boolean dragging(int button) {
        return (hitOnDownGlobal[button] != null && hitOnDownGlobal[button].distanceSq(posGlobal) > DRAG_THRESHOLD * DRAG_THRESHOLD);
    }


    private boolean on(@Nullable Widget touched) {

        if (touched != touching && touching != null) {
            touching.touch(null);
        }

        touching = touched;

        if (touching != null) {
            touching.touch(this);
            return true;
        }
        return false;
    }


    public boolean off() {
        if (touching != null) {
            touching.touch(null);
            touching = null;
            return true;
        }
        return false;
    }


    public boolean released(int button) {
        return !buttonDown[button];
    }

    public boolean pressed(int button) {
        return buttonDown[button];
    }

    public boolean pressedNow(int i) {
        return pressed(i) && !prevButtonDown[i];
    }

    public boolean releasedNow(int i) {
        return !pressed(i) && prevButtonDown[i];
    }

    public boolean releasedNow(int i, Surface c) {
        return releasedNow(i) && (c != null && hitOnDown[i] != null && Finger.relative(hitOnDown[i], c).inUnit());
    }


    /**
     * additionally tests for no dragging while pressed
     */
    public boolean clickedNow(int button) {
        return releasedNow(button) && !dragging(button);
    }

    public boolean clickedNow(int i, Surface c) {
        return clickedNow(i) && (c != null && hitOnDown[i] != null && Finger.relative(hitOnDown[i], c).inUnit());
    }

    /**
     * acquire an exclusive fingering state
     */
    public boolean tryFingering(Fingering f) {


        if (f!=null && fingering.compareAndSet(null, STARTING)) {
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

    public static Consumer<Finger> clicked(int button, Runnable clicked) {
        return clicked(button, clicked, null, null, null);
    }

    public static Consumer<Finger> clicked(int button, Runnable clicked, Runnable armed, Runnable hover, Runnable becameIdle) {

        final boolean[] idle = {true};

        if (becameIdle != null) becameIdle.run();

        return (finger) -> {

            if (finger != null && finger.touching != null) {
                if (finger.clickedNow(button, finger.touching)) {
                    if (clicked != null) clicked.run();
                } else if (finger.pressedNow(button)) {
                    if (armed != null) armed.run();
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
        };
    }

    public boolean isFingering() {
        return fingering.get() != null;
    }

    public v2 relativePos(Surface c) {
        return relative(pos, c);
    }


    public static v2 relative(v2 x, Surface c) {
        v2 y = new v2(x);
        RectFloat2D b = c.bounds;
        y.sub(b.x, b.y);
        y.scaled(1f / b.w, 1f / b.h);
        return y;
    }

}
