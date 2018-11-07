package spacegraph.space2d.widget;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.tree.rtree.rect.RectFloat;
import spacegraph.input.finger.Finger;
import spacegraph.input.key.KeyPressed;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRender;
import spacegraph.space2d.SurfaceRoot;
import spacegraph.space2d.container.EmptySurface;
import spacegraph.space2d.container.unit.MutableUnitContainer;
import spacegraph.video.Draw;

/**
 * Base class for GUI widgets, similarly designed to JComponent
 */
public class Widget extends MutableUnitContainer<Surface> implements KeyPressed {


    private static final float border = 0.05f;

    static final float backgroundAlpha = 0.8f;

    /**
     * z-raise/depth: a state indicating push/pull (ex: buttons)
     * positive: how lowered the button is: 0= not touched, to 1=push through the screen
     * zero: neutral state, default for components
     * negative: how raised
     */
    protected float dz = 0;

    protected boolean focused;

    /**
     * indicates current level of activity of this component, which can be raised by various
     * user and system actions and expressed in different visual metaphors.
     * positive: active, hot, important
     * zero: neutral
     * negative: disabled, hidden, irrelevant
     */
    private float temperature = 0;

    protected transient Finger touchedBy = null;

    public Widget() {
        this(new EmptySurface());
    }


    public Widget(Surface content) {
        super(content);
    }


    public void requestFocus() {
        SurfaceRoot r = root();
        if (r!=null) {
            r.keyFocus(this);
        }
    }

//    @Override
//    public boolean prePaint(SurfaceRender r) {
//        if (super.prePaint(r)) {
//
////            int dtMS = r.dtMS;
////
////            if (dtMS > 0) {
////                if (touchedBy != null) {
////                    temperature = Math.min(1f, temperature + dtMS / 100f);
////                }
////
////                if (temperature != 0) {
////                    float decayRate = (float) Math.exp(-dtMS / 1000f);
////                    temperature *= decayRate;
////                    if (Math.abs(temperature) < 0.01f)
////                        temperature = 0f;
////                }
////            }
//            return true;
//        }
//        return false;
//
//    }

    @Override
    protected void paintBelow(GL2 gl, SurfaceRender rr) {

        float dim = 1f - (dz /* + if disabled, dim further */) / 3f;
        float bri = 0.25f * dim;
        float r, g, b;
        r = g = b = bri;


        float t = this.temperature;
        temperature = Util.clamp(temperature * 0.95f, 0, 1f);
        if (t >= 0) {


            r += t / 4f;
            g += t / 4f;
            b += t / 4f;
        } else {

            b += -t / 2f;
            g += -t / 4f;
        }


        Draw.rectRGBA(bounds, r, g, b, backgroundAlpha, gl);

    }

    @Override
    protected void paintAbove(GL2 gl, SurfaceRender r) {

        if (focused) {
            float t = this.temperature;
            RectFloat b = this.bounds;
            float th = Math.min(b.w, b.h) * (0.1f + 0.1f * t);
            gl.glColor4f(0.5f + 0.5f * t,0.25f, 0.15f, 0.5f);
            Draw.rectFrame(b,  th, gl);
        }
    }

    @Override
    protected final void paintIt(GL2 gl, SurfaceRender r) {
        paintWidget(bounds, gl);
    }

//    @Override
//    protected void paintAbove(GL2 gl, SurfaceRender r) {
//        if (touchedBy != null) {
//            Draw.colorHash(gl, getClass().hashCode(), 0.5f + dz / 2f);
//
//            gl.glLineWidth(6 + dz * 6);
//            Draw.rectStroke(gl, x(), y(), w(), h());
//        }
//    }


    protected void paintWidget(RectFloat bounds, GL2 gl) {

    }


    @Override
    public Surface finger(Finger finger) {
        Surface s = super.finger(finger);
        if (s == null) {
            if (!focused && finger.pressedNow(0) || finger.pressedNow(2))
                requestFocus();

            if (finger.clickedNow(2 /*right button*/, this)) {


                /** auto-zoom */
                SurfaceRoot r = root();
                if (r != null) {


                    r.zoom(this);


                }

            }
            return this;
        }
        return s;
    }

    @Override
    protected RectFloat innerBounds() {
        RectFloat r = bounds;
        float b;
        if (r.w >= r.h) {
            b = border * r.h;
        } else {
            b = border * r.w;
        }
        b *= 2;
        return r.size(r.w - b, r.h - b);
    }

    @Override
    public void fingerTouch(Finger finger, boolean touching) {
        if (touching) {
            activate(0.5f);
            touchedBy = finger;
        } else {
            touchedBy = null;
        }
    }

    /** add temperature to this widget, affecting its display and possibly other behavior.  useful for indicating
     *  transient activity */
    public void activate(float inc) {
        this.temperature = Util.clamp(temperature + inc, 0, 1f);
    }


    @Override
    public void keyStart() {
        focused = true;
        activate(1f);
    }

    @Override
    public void keyEnd() {
        focused = false;
    }


    @Override
    public boolean key(KeyEvent e, boolean pressedOrReleased) {
        if (pressedOrReleased) {
            switch (e.getKeyCode()) {
                case KeyEvent.VK_TAB:
                    //TODO
                    // focus traversal
                    break;
                case KeyEvent.VK_PRINTSCREEN:
                    System.out.println(this); //TODO debugging
                    return true;
            }
        }
        return false;
    }

    public final boolean focused() {
        return focused;
    }
}
