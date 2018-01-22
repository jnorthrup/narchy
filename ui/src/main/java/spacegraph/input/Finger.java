package spacegraph.input;

import com.jogamp.nativewindow.util.Point;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Nullable;
import spacegraph.Ortho;
import spacegraph.Surface;
import spacegraph.math.v2;
import spacegraph.widget.windo.Widget;

import java.util.Arrays;
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
    private Fingering fingering = null;

    /**
     * global pointer screen coordinate, set by window the (main) cursor was last active in
     */
    public final static Point pointer = new Point();

    public final v2 hit = new v2(), hitGlobal = new v2();
    public final v2[] hitOnDown = new v2[5], hitOnDownGlobal = new v2[5];
    public final boolean[] buttonDown = new boolean[5];
    public final boolean[] prevButtonDown = new boolean[5];
    private final Ortho root;


    //TODO wheel state

    /**
     * widget above which this finger currently hovers
     */
    public @Nullable Widget touching;

    /**
     * TODO scale this to pixel coordinates, this spatial coordinate is tricky and resolution dependent anyway
     */
    final static float DRAG_THRESHOLD = 2f;

    public Finger(Ortho root) {
        this.root = root;
    }


    public Surface on(float sx, float sy, float lx, float ly, short[] nextButtonDown) {
        return on(this.root.surface, sx, sy, lx, ly, nextButtonDown);
    }

    public Surface on(Surface root, float sx, float sy, float lx, float ly, short[] nextButtonDown) {
        this.hit.set(lx, ly);
        this.hitGlobal.set(sx, sy);

        arraycopy(this.buttonDown, 0, prevButtonDown, 0, buttonDown.length);

        fill(this.buttonDown, false);
        if (nextButtonDown != null) {
            for (short s : nextButtonDown) {
                if (s > 0) //ignore -1 values
                    this.buttonDown[s - 1 /* start at zero=left button */] = true;
            }

            for (int j = 0, jj = hitOnDown.length; j < jj; j++) {
                if (!prevButtonDown[j] && buttonDown[j]) {
                    hitOnDown[j] = new v2(hit);
                    hitOnDownGlobal[j] = new v2(hitGlobal);
                }
            }

        } else {
            Arrays.fill(hitOnDown, null);
        }


        //START DESCENT:
        Surface s;
        if (root!=null && fingering == null) {

            s = root.onTouch(this, hit, nextButtonDown);
            if (s instanceof Widget) {
                if (!on((Widget) s))
                    s = null;
            } else {
                if (touching!=null) {
                    touching.touch(null);
                    touching = null;
                }
                s = null;
            }
        } else {
            s = null;
            synchronized (this) {
                if (!fingering.update(this)) {
                    fingering = null;
                }
            }
        }

        for (int j = 0, jj = hitOnDown.length; j < jj; j++) {
            if (!buttonDown[j] && hitOnDown[j] != null) {
                hitOnDown[j] = null; //release
            }
        }

//        if (s != null)
//            on((Widget) s);


        return s;
    }

    public boolean dragging(int button) {
        return (hitOnDownGlobal[button] != null && hitOnDownGlobal[button].distanceSq(hitGlobal) >= DRAG_THRESHOLD * DRAG_THRESHOLD);
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

    /** additionally tests for no dragging while pressed */
    public boolean clickedNow(int button) {
        return releasedNow(button) && !dragging(button);
    }

    /**
     * acquire an exclusive fingering state
     */
    public boolean tryFingering(Fingering f) {
        if (root != null) {
            synchronized (this) {
                if (fingering == null) {
                    fingering = f;
                    f.start(this);
                    root.surface.onTouch(this, null, ArrayUtils.EMPTY_SHORT_ARRAY); //release all fingering on surfaces
                    return true;
                }
            }
        }
        return false;
    }

    public static Consumer<Finger> clicked(int button, Runnable clicked) {
        return clicked(button, clicked, null, null, null);
    }
    public static Consumer<Finger> clicked(int button, Runnable clicked, Runnable armed, Runnable hover, Runnable becameIdle) {

        final boolean[] idle = {true};

        if (becameIdle!=null) becameIdle.run();

        return (finger) -> {

            if (finger != null) {
                if (finger.clickedNow(button)) {
                    if (clicked!=null) clicked.run();
                } else if (finger.pressedNow(button)) {
                    if (armed!=null) armed.run();
                } else {
                    if (hover!=null) hover.run();
                }
                idle[0] = false;
            } else {
                if (becameIdle!=null && !idle[0]) {
                    becameIdle.run();
                    idle[0] = true;
                }
            }
        };
    }

    public boolean isFingering() {
        return fingering!=null;
    }
}
