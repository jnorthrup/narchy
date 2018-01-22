package spacegraph.widget.windo;

import com.jogamp.opengl.GL2;
import jcog.tree.rtree.rect.RectFloat2D;
import org.jetbrains.annotations.Nullable;
import spacegraph.Ortho;
import spacegraph.Surface;
import spacegraph.SurfaceRoot;
import spacegraph.input.Finger;
import spacegraph.input.FingerDragging;
import spacegraph.layout.Stacking;
import spacegraph.layout.Switching;
import spacegraph.render.Draw;
import spacegraph.widget.meta.MetaFrame;

import java.util.List;

/**
 * Base class for GUI widgets, similarly designed to JComponent
 */
abstract public class Widget extends Switching {


    static final int STATE_RAW = 0;
    static final int STATE_META = 1;




    /**
     * z-raise/depth: a state indicating push/pull (ex: buttons)
     * positive: how lowered the button is: 0= not touched, to 1=push through the screen
     * zero: neutral state, default for components
     * negative: how raised
     */
    protected float dz = 0;



//MARGIN
//    @Override
//    public void setParent(Surface s) {
//        super.setParent(s);
//
//        float proportion = 0.9f;
//        float margin = 0.0f;
//        //float content = 1f - margin;
//        float x = margin / 2f;
//
//        Surface content = content();
//        content.scaleLocal.set(proportion, proportion);
//        content.translateLocal.set(x, 1f - proportion, 0);
//
//    }

    /** if non-null, a finger which is currently hovering on this */
    @Nullable Finger touchedBy;

    public final Stacking content = new Stacking() {



        /**
         * indicates current level of activity of this component, which can be raised by various
         * user and system actions and expressed in different visual metaphors.
         * positive: active, hot, important
         * zero: neutral
         * negative: disabled, hidden, irrelevant
         */
        float temperature = 0;

        @Override
        public void prePaint(int dtMS) {

            //hover glow
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


        }


        @Override
        protected void paintBelow(GL2 gl) {

            //if (tangible()) {
                float dim = 1f - (dz /* + if disabled, dim further */) / 3f;
                float bri = 0.25f * dim;
                float r, g, b;
                r = g = b = bri;


                float t = this.temperature;
                if (t >= 0) {
                    //fire palette TODO improve
                    //            r += t / 2f;
                    //            g += t / 4f;

                    r += t / 4f;
                    g += t / 4f;
                    b += t / 4f;
                } else {
                    //ice palette TODO improve
                    b += -t / 2f;
                    g += -t / 4f;
                }

                gl.glColor4f(r, g, b, 0.5f);

                Draw.rect(gl, bounds);
            //}

            //rainbow backgrounds
            //Draw.colorHash(gl, this.hashCode(), 0.8f, 0.2f, 0.25f);
            //Draw.rect(gl, 0, 0, 1, 1);
        }


        @Override
        protected final void paintIt(GL2 gl) {
            paintWidget(gl, bounds);
        }

        @Override
        protected void paintAbove(GL2 gl) {
            if (touchedBy != null) {
                Draw.colorHash(gl, getClass().hashCode(), 0.5f + dz / 2f);
                //gl.glColor3f(1f, 1f, 0f);
                gl.glLineWidth(6 + dz * 6);
                Draw.rectStroke(gl, x(), y(), w(), h());
            }
        }



    };


    @Override
    protected final void paintIt(GL2 gl) {

    }

    protected void paintWidget(GL2 gl, RectFloat2D bounds) {

    }

    public Widget() {
        super();
        states(
            ()-> content,
            ()->{
                //meta
                return new MetaFrame(this);
            }
        );
    }

    public Widget(Surface... child) {
        this();
        children(child);
    }

    public Surface[] children() {
        return content.children();
    }

    public final Widget children(List<Surface> next) {
        content.set(next);
        return this;
    }

    public final Widget children(Surface... next) {
        content.set(next);
        return this;
    }
    public final Widget add(Surface x) {
        content.add(x);
        return this;
    }
    public final Widget remove(Surface x) {
        content.remove(x);
        return this;
    }


    @Override
    public boolean tangible() {
        return true;
    }

    public void touch(@Nullable Finger finger) {
        touchedBy = finger;
        if (finger != null) {

            if (finger.clickedNow(2)) { //released right button

                int curState = switched;
                int nextState = nextState(curState);
                state(nextState); //toggle

//                SurfaceRoot r = root();
//                if (r!=null) {
//                    switch (curState) {
//                        case STATE_META:
//                            r.zoom(this);
//                            break;
//                        case STATE_ZOOM:
//                            r.unzoom();
//                            break;
//                    }
//                }
            } else if (finger.pressedNow(1)) {
                finger.tryFingering(new FingerDragging(1) {

                    SurfaceRoot r = root();

                    @Override
                    public void start(Finger f) {

                    }

                    @Override
                    protected boolean drag(Finger f) {
                        ((Ortho)r).zoom(bounds, 0.5f);
                        return true;
                    }
                });
            }

        } else {
            onTouch(null, null, null);
        }
    }

    private int nextState(int curState) {
        switch (curState) {
            case STATE_RAW:
                return STATE_META;
            case STATE_META:
                return STATE_RAW;
            default:
                throw new UnsupportedOperationException();
        }
    }

}
