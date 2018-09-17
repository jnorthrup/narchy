package spacegraph.space2d.widget.windo;

import com.jogamp.opengl.GL2;
import jcog.tree.rtree.rect.RectFloat2D;
import org.jetbrains.annotations.Nullable;
import spacegraph.input.finger.Finger;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRender;
import spacegraph.space2d.SurfaceRoot;
import spacegraph.space2d.container.EmptySurface;
import spacegraph.space2d.container.collection.MutableListContainer;
import spacegraph.video.Draw;

/**
 * Base class for GUI widgets, similarly designed to JComponent
 */
public class Widget extends MutableListContainer {


    private static final float border = 0.05f;

    /**
     * z-raise/depth: a state indicating push/pull (ex: buttons)
     * positive: how lowered the button is: 0= not touched, to 1=push through the screen
     * zero: neutral state, default for components
     * negative: how raised
     */
    protected float dz = 0;

    /**
     * if non-null, a finger which is currently hovering on this
     */
    @Nullable
    private Finger touchedBy;


    /**
     * indicates current level of activity of this component, which can be raised by various
     * user and system actions and expressed in different visual metaphors.
     * positive: active, hot, important
     * zero: neutral
     * negative: disabled, hidden, irrelevant
     */
    private float temperature = 0;

    public Widget() {
        this(new EmptySurface());
    }


    public Widget(Surface content) {
        super();

        content(content);


    }

    @Override
    public boolean prePaint(SurfaceRender r) {
        if (super.prePaint(r)) {

            int dtMS = r.dtMS;

            if (dtMS > 0) {
                if (touchedBy != null) {
                    temperature = Math.min(1f, temperature + dtMS / 100f);
                }

                if (temperature != 0) {
                    float decayRate = (float) Math.exp(-dtMS / 1000f);
                    temperature *= decayRate;
                    if (Math.abs(temperature) < 0.01f)
                        temperature = 0f;
                }
            }
            return true;
        }
        return false;

    }

    @Override
    protected void paintBelow(GL2 gl, SurfaceRender rr) {

        if (Widget.this.tangible()) {
            float dim = 1f - (dz /* + if disabled, dim further */) / 3f;
            float bri = 0.25f * dim;
            float r, g, b;
            r = g = b = bri;


            float t = this.temperature;
            if (t >= 0) {


                r += t / 4f;
                g += t / 4f;
                b += t / 4f;
            } else {

                b += -t / 2f;
                g += -t / 4f;
            }

            Draw.rectRGBA(bounds, r, g, b, 0.5f, gl);
        }


    }

    @Override
    protected final void paintIt(GL2 gl) {
        paintWidget(gl, bounds);
    }

    @Override
    protected void paintAbove(GL2 gl, SurfaceRender r) {
        if (touchedBy != null) {
            Draw.colorHash(gl, getClass().hashCode(), 0.5f + dz / 2f);

            gl.glLineWidth(6 + dz * 6);
            Draw.rectStroke(gl, x(), y(), w(), h());
        }
    }

    protected boolean isTouched() {
        return touchedBy != null;
    }

    protected void paintWidget(GL2 gl, RectFloat2D bounds) {

    }


    @Override
    public boolean tangible() {
        return true;
    }

    public void onFinger(@Nullable Finger finger) {

        touchedBy = finger;
        if (finger != null) {

//            if (finger.clickedNow(1, this)) {
//
//
//            }
            if (finger.releasedNow(2 /*right button*/, this)) {


                /** auto-zoom */
                SurfaceRoot r = root();
                if (r != null) {


                    r.zoom(this);


                }

            }

        }
    }

    @Override
    protected void doLayout(int dtMS) {
        RectFloat2D r = bounds;
        float b;
        if (r.w >= r.h) {
            b = border * r.h;
        } else {
            b = border * r.w;
        }
        b *= 2;
        RectFloat2D rr = r.size(r.w - b, r.h - b);
        forEach(c -> c.pos(rr));
    }

    public Widget content(Surface next) {
        set(next);
        return this;
    }


}
