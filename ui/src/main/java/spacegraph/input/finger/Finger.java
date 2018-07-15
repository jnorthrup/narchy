package spacegraph.input.finger;

import com.jogamp.opengl.GL2;
import jcog.data.atomic.AtomicFloat;
import jcog.data.bit.AtomicMetalBitSet;
import jcog.tree.rtree.rect.RectFloat2D;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.Surface;
import spacegraph.space2d.widget.windo.Widget;
import spacegraph.util.math.v2;
import spacegraph.video.Draw;

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
    /**
     * TODO scale this to pixel coordinates, this spatial coordinate is tricky and resolution dependent anyway
     */
    private final static float DRAG_THRESHOLD = 3f;
    public final v2 pos = new v2(), posPixel = new v2(), posScreen = new v2();
    private final v2[] hitOnDown = new v2[MAX_BUTTONS];
    public final v2[] hitOnDownGlobal = new v2[MAX_BUTTONS];
    private final AtomicMetalBitSet buttonDown = new AtomicMetalBitSet();
    public final AtomicMetalBitSet prevButtonDown = new AtomicMetalBitSet();

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
    public final AtomicReference<@Nullable Widget> touching = new AtomicReference<>();


    public Finger() {

    }

    public static Predicate<Finger> clicked(int button, Runnable clicked) {
        return clicked(button, clicked, null, null, null);
    }

    private static Predicate<Finger> clicked(int button, Runnable clicked, Runnable armed, Runnable hover, Runnable becameIdle) {
        return clicked(button, (f) -> clicked.run(), armed, hover, becameIdle );
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

    /** call when finger exits the window / screen, the window becomes unfingerable, etc..*/
    public void exit() {






    }

    /** call when finger enters the window */
    public void enter() {



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

        for (int b = 0;  b< MAX_BUTTONS; b++) {
            boolean wasPressed = wasPressed(b);
            boolean pressed = pressing(b);
            if (!wasPressed && pressed) {
                hitOnDown[b] = new v2(pos);
                hitOnDownGlobal[b] = new v2(posPixel);
            } else if (wasPressed && !pressed) {
                hitOnDownGlobal[b] = hitOnDown[b] = null; 
            }
        }


        
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


        
        for (short b : nextButtonDown) {

            boolean pressed = (b > 0);

            if (!pressed) b = (short) -b; 
            b--; 

            buttonDown.set(b, pressed);

        }

    }

    public Surface on(Surface root) {

        Fingering ff = this.fingering.get();
        Fingering f0 = ff;
        Surface touchedNext;

        try {

            

            if (ff == null || ff.escapes()) {
                touchedNext = root.tryTouch(this);
            } else {
                touchedNext = touching.get(); 
            }

            

            if (ff != null) {

                if (!ff.update(this)) {
                    ff.stop(this);
                    ff = null;
                }

            }


        } finally {

            if (ff == null)
                fingering.compareAndSet(f0, null);

            for (AtomicFloat r : rotation)
                r.getAndZero();

        }

        on(touchedNext instanceof Widget ? (Widget) touchedNext : null);

        return touchedNext;
    }

    private boolean dragging(int button) {
        return (hitOnDownGlobal[button] != null && hitOnDownGlobal[button].distanceSq(posPixel) > DRAG_THRESHOLD * DRAG_THRESHOLD);
    }

    private void on(@Nullable Widget touchNext) {

        @Nullable Widget touchPrev = touching.getAndSet(touchNext);

        if (touchPrev == touchNext)
            return;

        if (touchPrev != null) {
            touchPrev.onFinger(null);
        }

        if (touchNext!=null) {
            touchNext.onFinger(this);
        }
    }

    /** allows a fingered object to push the finger off it */
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

    public boolean pressedNow(int button) {
        return pressing(button) && !wasPressed(button);
    }

    private boolean wasPressed(int button) {
        return prevButtonDown.get(button);
    }

    public boolean wasReleased(int button) {
        return !wasPressed(button);
    }

    private boolean releasedNow(int button) {
        return !pressing(button) && wasPressed(button);
    }

    public boolean releasedNow(int i, Surface c) {
        return releasedNow(i) && (c != null && hitOnDown[i] != null && Finger.relative(hitOnDown[i], c).inUnit());
    }

    /**
     * additionally tests for no dragging while pressed
     */
    private boolean clickedNow(int button) {
        boolean clicked = releasedNow(button);
        boolean notDragging = !dragging(button);
        
        return clicked && notDragging;
    }

    public boolean clickedNow(int i, Surface c) {
        if (clickedNow(i)) {
            v2 where = hitOnDown[i];
            if (where != null)
                return Finger.relative(where, c).inUnit();
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


    /** TODO make very configurable */
    public void drawCrossHair(Surface window, GL2 gl) {

        gl.glLineWidth(4f);

        float ch = 175f; 
        float cw = 175f; 

        float smx = posPixel.x;
        float smy = posPixel.y;

        gl.glColor4f(0.5f, 0.5f, 0.5f, 0.25f);
        Draw.rectStroke(gl, smx - cw / 2f, smy - ch / 2f, cw, ch);

        gl.glColor4f(0.5f, 0.5f, 0.5f, 0.5f);
        Draw.line(gl, smx, smy - ch, smx, smy + ch);
        Draw.line(gl, smx - cw, smy, smx + cw, smy);

    }

    private final AtomicFloat[] rotation = new AtomicFloat[3];

    {
        rotation[0] = new AtomicFloat();
        rotation[1] = new AtomicFloat();
        rotation[2] = new AtomicFloat();
    }
    public boolean rotationAdd(float[] rotation) {
        for (int i = 0; i < rotation.length; i++)
            this.rotation[i].add(rotation[i]);
        Surface t = touching();

        return (t instanceof Finger.RotationAbsorbed);
    }

    public float rotationX() {
        return rotation[0].floatValue();
    }
    public float rotationY() {
        return rotation[1].floatValue();
    }
    public float rotationZ() {
        return rotation[2].floatValue();
    }

    public interface RotationAbsorbed {
    }
}
