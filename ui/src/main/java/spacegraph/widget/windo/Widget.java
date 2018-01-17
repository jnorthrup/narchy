package spacegraph.widget.windo;

import com.jogamp.opengl.GL2;
import jcog.list.FastCoWList;
import org.jetbrains.annotations.Nullable;
import spacegraph.Surface;
import spacegraph.input.Finger;
import spacegraph.layout.Stacking;
import spacegraph.layout.Switching;
import spacegraph.layout.VSplit;
import spacegraph.render.Draw;
import spacegraph.widget.button.PushButton;
import spacegraph.widget.text.Label;

import java.util.List;

import static spacegraph.layout.Grid.grid;

/**
 * Base class for GUI widgets, similarly designed to JComponent
 */
abstract public class Widget extends Switching {


    static final int STATE_RAW = 0;
    static final int STATE_META = 1;


    /** proxy to the content */
    final Stacking inner = new Stacking() {
        @Override
        public boolean tangible() {
            return false;
        }


    };

    /** if non-null, a finger which is currently hovering on this */
    @Nullable Finger touchedBy;

    /**
     * z-raise/depth: a state indicating push/pull (ex: buttons)
     * positive: how lowered the button is: 0= not touched, to 1=push through the screen
     * zero: neutral state, default for components
     * negative: how raised
     */
    protected float dz = 0;

    /**
     * indicates current level of activity of this component, which can be raised by various
     * user and system actions and expressed in different visual metaphors.
     * positive: active, hot, important
     * zero: neutral
     * negative: disabled, hidden, irrelevant
     */
    float temperature = 0;


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

    public Widget() {
        super();
        states(
            ()->inner,
            ()->{
                return new VSplit(inner, grid(new Label(toString()), new PushButton("x")), 0.1f);
            }
        );
    }

    public Widget(Surface... child) {
        this();
        children(child);
    }

    public FastCoWList<Surface> children() {
        return inner.children;
    }

    public final Widget children(List<Surface> next) {
        inner.children(next);
        return this;
    }

    public final Widget children(Surface... next) {
        inner.children(next);
        return this;
    }
    public final Widget add(Surface x) {
        inner.add(x);
        return this;
    }
    public final Widget remove(Surface x) {
        inner.remove(x);
        return this;
    }

    @Override
    public boolean tangible() {
        return true;
    }

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

        if (tangible()) {
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
        }

        //rainbow backgrounds
        //Draw.colorHash(gl, this.hashCode(), 0.8f, 0.2f, 0.25f);
        //Draw.rect(gl, 0, 0, 1, 1);
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

    //    @Override
//    protected boolean onTouching(v2 hitPoint, short[] buttons) {
////        int leftTransition = buttons[0] - (touchButtons[0] ? 1 : 0);
////
////        if (leftTransition == 0) {
////            //no state change, just hovering
////        } else {
////            if (leftTransition > 0) {
////                //clicked
////            } else if (leftTransition < 0) {
////                //released
////            }
////        }
//
//
//        return false;
//    }




    public void touch(@Nullable Finger finger) {
        touchedBy = finger;
        if (finger == null) {
            onTouch(null, null, null);
        } else {
            if (finger.clickReleased(2)) { //released right button
                //MetaFrame.toggle(this);
                state(switched == STATE_RAW ? STATE_META : STATE_RAW); //toggle
            }
        }
    }



}
