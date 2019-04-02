package spacegraph.space2d.widget;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.tree.rtree.rect.RectFloat;
import spacegraph.input.finger.Finger;
import spacegraph.input.key.KeyPressed;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRoot;
import spacegraph.space2d.container.EmptySurface;
import spacegraph.space2d.container.unit.MutableUnitContainer;
import spacegraph.util.math.Color4f;
import spacegraph.video.Draw;

/**
 * Base class for GUI widgets, similarly designed to JComponent
 */
public class Widget extends MutableUnitContainer<Surface> implements KeyPressed {


    public static final float marginPctDefault = 0.04f;

    private static final float border = 0.05f;

    /** the fundamental current 'color' of the widget. not necessarily to match or conflict
     * with what it actually displays but instead to provide a distinctive color for widgets
     * which otherwise do not specify styling.
     */
    public final Color4f color = new Color4f()
            //default random color tint
            .hsl(System.identityHashCode(this), 0.2f, 0.1f).a(0.9f);

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
    protected float pri = 0;

    public Widget() {
        this(new EmptySurface());
    }


    public Widget(Surface content) {
        super(content);
    }


    /** request focus */
    public <W extends Widget> W focus() {
        SurfaceRoot r = root();
        if (r!=null) {
            r.keyFocus(this);
        }
        return (W) this;
    }

    @Override
    protected void paintIt(GL2 gl, ReSurface r) {
        //pri decay
        //////            1 - (float) Math.exp(-(((double) dt) / n.dur()) / memoryDuration.floatValue());
        float DECAY_PERIOD = 2; //TODO use exponential decay formula
        double decayRate = Math.exp(-(((double) r.dtS()) / DECAY_PERIOD));
        pri = (float) (pri * decayRate);
        //float PRI_DECAY = 0.97f; //TODO use exponential decay formula
        //pri = Util.clamp(pri * PRI_DECAY, 0, 1f);

        paintWidget(bounds, gl);
    }

    @Override
    protected void compileChildren(ReSurface r) {
        super.compileChildren(r);

        if (focused) {
            //focused indicator
            r.on((gl)->{
                if (focused) {
                    float t = this.pri;
                    RectFloat b = this.bounds;
                    float th = Math.min(b.w, b.h) * (0.1f + 0.1f * t);
                    gl.glColor4f(0.5f + 0.5f * t, 0.25f, 0.15f, 0.5f);
                    Draw.rectFrame(b, th, gl);
                }
            });
        }
    }



    protected void paintWidget(RectFloat bounds, GL2 gl) {
        float dim = 1f - (dz /* + if disabled, dim further */) / 3f;
        float bri = 0.1f * dim;
        color.set( rgb-> Util.or(rgb,bri,pri/8), gl);
        Draw.rect(bounds, gl);
    }


    @Override
    public Surface finger(Finger finger) {
        Surface s = super.finger(finger);
        if (s == null) {

            priAtleast(0.1f);

            if (!focused && finger.pressedNow(0) || finger.pressedNow(2))
                focus();

            return this;
        }
        return s;
    }

    private void priAtleast(float p) {
        pri = Math.max(pri, p);
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
            pri(0.5f);
        } else {

        }
    }

    /** add temperature to this widget, affecting its display and possibly other behavior.  useful for indicating
     *  transient activity */
    public void pri(float inc) {
        this.pri = Util.unitize(pri + inc);
    }

    public float pri() { return pri; }

    @Override
    public void keyStart() {
        focused = true;
        pri(1f);
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
