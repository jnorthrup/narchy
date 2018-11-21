package spacegraph.space2d.widget.slider;

import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.math.FloatRange;
import org.eclipse.collections.api.block.procedure.primitive.FloatFloatProcedure;
import spacegraph.input.finger.Finger;
import spacegraph.input.finger.FingerDragging;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRender;
import spacegraph.util.math.v2;
import spacegraph.video.Draw;

/**
 * Created by me on 6/26/16.
 */
public class XYSlider extends Surface {

    private final v2 knob = new v2(0.5f, 0.5f);

    private FloatFloatProcedure change = null;
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
        updated();
    }

    public XYSlider(FloatRange x, FloatRange y) {
        this();
        set(x.floatValue(), y.floatValue());
        on((xx,yy)->{
           x.setProportionally(xx); y.setProportionally(yy);
        });
    }

    public XYSlider on(FloatFloatProcedure change) {
        this.change = change;
        return this;
    }

    @Override
    public Surface finger(Finger finger) {

        if (finger!=null && finger.pressing(0)) {
            finger.tryFingering(new FingerDragging(0) {

                @Override
                protected boolean startDrag(Finger f) {
                    pressing = true;
                    return super.startDrag(f);
                }

                @Override
                public void stop(Finger finger) {
                    super.stop(finger);
                    pressing = false;
                }

                @Override protected boolean drag(Finger f) {
                    v2 hitPoint = finger.relativePos(XYSlider.this);
                    if (hitPoint.inUnit()) {
                        pressing = true;
                        if (!Util.equals(knob.x, hitPoint.x, Float.MIN_NORMAL) || !Util.equals(knob.y, hitPoint.y, Float.MIN_NORMAL)) {
                            knob.set(hitPoint);
                            updated();
                        }
                        return true;
                    }
                    return true;
                }
            });
        }
        return this;















    }




    private void updated() {
        FloatFloatProcedure c = change;
        if (c!=null) {
            c.value(knob.x, knob.y);
        }
    }


    @Override
    protected void paint(GL2 gl, SurfaceRender surfaceRender) {

        Draw.rectRGBA(bounds, 0f, 0f, 0f, 0.8f, gl);

        float px = knob.x;
        float py = knob.y;

        float knobThick = pressing ? 0.08f : 0.04f;


        float bw = bounds.w;
        float bh = bounds.h;
        float KT = Math.min(bw, bh) * knobThick;
        float kw = bounds.x+(px* bw);
        float kh = bounds.y+(py* bh);
        float KTH = KT / 2;
        Draw.rectAlphaCorners(bounds.x, kh - KTH, kw - KTH, kh + KTH, knobColor, lefAlphaCorners, gl
        );
        Draw.rectAlphaCorners(kw + KTH, kh - KTH, bounds.x + bw, kh + KTH, knobColor, rihAlphaCorners, gl
        );

        Draw.rectAlphaCorners(kw - KTH, bounds.y, kw + KTH, kh- KTH, knobColor, botAlphaCorners, gl
        );
        Draw.rectAlphaCorners(kw - KTH, kh + KTH, kw + KTH, bounds.y + bh, knobColor, topAlphaCorners, gl
        );

        




    }

    public XYSlider set(float x, float y) {
        knob.set(x, y);
        return this;
    }
}
