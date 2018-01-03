package spacegraph.widget.slider;

import com.jogamp.opengl.GL2;
import jcog.Util;
import org.eclipse.collections.api.block.procedure.primitive.FloatFloatProcedure;
import spacegraph.Surface;
import spacegraph.input.Finger;
import spacegraph.math.v2;
import spacegraph.render.Draw;

/**
 * Created by me on 6/26/16.
 */
public class XYSlider extends Surface {

    final v2 knob = new v2(0.5f, 0.5f);




    FloatFloatProcedure change = null;

    public XYSlider() {
        super();
    }

    public XYSlider change(FloatFloatProcedure change) {
        this.change = change;
        return this;
    }

    @Override
    protected boolean onTouching(Finger finger, v2 hitPoint, short[] buttons) {
        if (leftButton(buttons)) {
            if (!Util.equals(knob.x, hitPoint.x, Float.MIN_NORMAL) || !Util.equals(knob.y, hitPoint.y, Float.MIN_NORMAL)) {
                knob.set(hitPoint);
                updated();
            }
            return true;
        }
        return super.onTouching(finger, hitPoint, buttons);
    }

    protected void updated() {
        FloatFloatProcedure c = change;
        if (c!=null) {
            c.value(knob.x, knob.y);
        }
    }


    @Override
    protected void paint(GL2 gl, int dtMS) {
        gl.glColor4f(0f, 0f, 0f, 0.8f); //background
        Draw.rect(gl, bounds);

        //float margin = 0.1f;
        //float mh = margin / 2.0f;

        float px = knob.x;
        float py = knob.y;

        gl.glColor4f(0.75f, 0.75f, 0.75f, 0.75f);
        float W = Math.min(w(),h()) * 0.1f;
        float h1 = py*h() - W / 2f;
        Draw.rect(gl, x(), y()+(h1)-W/2, w(), W); //horiz

        float w1 = px*w() - W / 2f;
        Draw.rect(gl, x()+(w1)- W /2, y(), W, h()); //vert

//        //gl.glColor4f(0.2f, 0.8f, 0f, 0.75f);
//        float knobSize = this.knobWidth;
//        Draw.rect(gl, w1-knobSize/2f, h1-knobSize/2f, knobSize, knobSize, 0); //knob
    }

    public XYSlider set(float x, float y) {
        knob.set(x, y);
        return this;
    }
}
