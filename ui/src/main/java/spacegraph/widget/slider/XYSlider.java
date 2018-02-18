package spacegraph.widget.slider;

import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.tree.rtree.rect.RectFloat2D;
import org.eclipse.collections.api.block.procedure.primitive.FloatFloatProcedure;
import spacegraph.Surface;
import spacegraph.input.Finger;
import spacegraph.math.v2;
import spacegraph.render.Draw;
import spacegraph.widget.windo.Widget;

/**
 * Created by me on 6/26/16.
 */
public class XYSlider extends Widget {

    final v2 knob = new v2(0.5f, 0.5f);

    FloatFloatProcedure change = null;
    private final float[] knobColor = new float[] { 0.75f, 0.75f, 0.75f };


    private static final float _low = 0.2f;
    private static final float _HIH = 0.8f;
    private static final float[] lefAlphaCorners = new float[] {_low, _HIH, _HIH, _low};
    private static final float[] rihAlphaCorners = new float[] {_HIH, _low, _low, _HIH};
    private static final float[] topAlphaCorners = new float[] {_HIH, _HIH, _low, _low};
    private static final float[] botAlphaCorners = new float[] {_low, _low, _HIH, _HIH};
    private boolean pressing;


    public XYSlider() {
        super();
    }

    public XYSlider change(FloatFloatProcedure change) {
        this.change = change;
        return this;
    }

    @Override
    public Surface onTouch(Finger finger, short[] buttons) {
        if (finger!=null && leftButton(buttons)) {
            pressing = true;
            v2 hitPoint = finger.relativeHit(content);
            if (hitPoint.inUnit()) {
                if (!Util.equals(knob.x, hitPoint.x, Float.MIN_NORMAL) || !Util.equals(knob.y, hitPoint.y, Float.MIN_NORMAL)) {
                    knob.set(hitPoint);
                    updated();
                }
                return this;
            }
        } else {
            pressing = false;
        }
        return super.onTouch(finger, buttons);
    }



    protected void updated() {
        FloatFloatProcedure c = change;
        if (c!=null) {
            c.value(knob.x, knob.y);
        }
    }

    @Override
    protected void paintWidget(GL2 gl, RectFloat2D bounds) {
        gl.glColor4f(0f, 0f, 0f, 0.8f); //background
        Draw.rect(gl, bounds);

        //float margin = 0.1f;
        //float mh = margin / 2.0f;

        float px = knob.x;
        float py = knob.y;

        float knobThick = pressing ? 0.08f : 0.04f;


        float bw = bounds.w;
        float bh = bounds.h;
        float KT = Math.min(bw, bh) * knobThick;
        float kw = bounds.x+(px* bw);
        float kh = bounds.y+(py* bh);
        float KTH = KT / 2;
        Draw.rectAlphaCorners(gl,
                bounds.x, kh - KTH,
                kw - KTH, kh + KTH, knobColor, lefAlphaCorners);
        Draw.rectAlphaCorners(gl,
                kw + KTH, kh - KTH,
                bounds.x + bw, kh + KTH, knobColor, rihAlphaCorners);

        Draw.rectAlphaCorners(gl,
                kw - KTH, bounds.y,
                kw + KTH, kh- KTH, knobColor, botAlphaCorners);
        Draw.rectAlphaCorners(gl,
                kw - KTH, kh + KTH,
                kw + KTH, bounds.y + bh, knobColor, topAlphaCorners);

        //Draw.rectAlphaCorners(gl, kw, kh- KTH, kw+ KTH, bounds.h, knobColor, botAlphaCorners);

//        //gl.glColor4f(0.2f, 0.8f, 0f, 0.75f);
//        float knobSize = this.knobWidth;
//        Draw.rect(gl, w1-knobSize/2f, h1-knobSize/2f, knobSize, knobSize, 0); //knob
    }

    public XYSlider set(float x, float y) {
        knob.set(x, y);
        return this;
    }
}
