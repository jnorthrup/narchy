package spacegraph.space2d.widget.slider;

import com.jogamp.opengl.GL2;
import jcog.Util;
import org.eclipse.collections.api.block.procedure.primitive.ObjectFloatProcedure;
import org.jetbrains.annotations.Nullable;
import spacegraph.input.finger.Finger;
import spacegraph.input.finger.FingerDragging;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRender;
import spacegraph.util.math.v2;
import spacegraph.video.Draw;

/**
 * abstract 1D slider/scrollbar
 */
public class SliderModel extends Surface {


    /** dead-zone at the edges to latch min/max values */
    static private final float margin =
            0.0001f;

    

    @Nullable
    private ObjectFloatProcedure<SliderModel> change;

    private volatile float p;

    public SliderModel() {
    }
    public SliderModel(float v) {
        setValue(v);
    }


    public SliderModel on(ObjectFloatProcedure<SliderModel> c) {
        this.change = c;
        return this;
    }

    @Override
    protected void paint(GL2 gl, SurfaceRender surfaceRender) {
        Draw.bounds(bounds, gl, g -> ui.paint(this.p, g));
    }

    public SliderUI ui = SolidLeft;

    public interface SliderUI {
        void paint(float p, GL2 gl);

        /** resolves the 2d hit point to a 1d slider value */
        float p(v2 hitPoint);

    }

    public void update() {

    }

    public SliderModel type(SliderUI draw) {
        this.ui = draw;
        return this;
    }

    @Override
    public Surface finger(Finger finger) {


        if (finger!=null && finger.pressing(0)) {
            if (finger.tryFingering(new FingerDragging(0) {
                @Override protected boolean drag(Finger f) {
                    v2 hitPoint = finger.relativePos(SliderModel.this);
                    setPoint(ui.p(hitPoint));
                    return true;
                }
            }))
                return this;
        }
        return null; 
    }


    private void setPoint(float pNext) {
        Util.assertFinite(pNext);

        float pPrev = this.p;
        if (Util.equals(pPrev, pNext, Float.MIN_NORMAL))
            return;

        this.p = pNext;

        onChanged();
    }

    protected void onChanged() {
        if (change!=null) {
            change.value(this, value());
        }
    }


    protected final void setValue(float v) {
        if (v == v) {
            setPoint(p(v));
        } else {
            //? NaN to disable?
        }

    }

    public float value() {
        return v(p);
    }

    /** normalize: gets the output value given the proportion (0..1.0) */
    float v(float p) {
        return p;
    }

    /**
     * unnormalize: gets proportion from external value
     */
    float p(float v) {
        return v;
    }

    private static float pHorizontal(v2 hitPoint) {
        return pTarget(hitPoint.x);
    }
    private static float pVertical(v2 hitPoint) {
        return pTarget(1 - hitPoint.y);
    }

    private static float pTarget(float x) {
        if (x <= margin)
            return 0;
        else if (x >= (1f - margin))
            return 1f;
        else
            return x;
    }

    /** resulting value is lower aligned */
    private static float pTarget(float x, float knob) {
        return Util.clamp(pTarget(x) - knob/2, 0, 1-knob);
    }



    private static final SliderUI SolidLeft = new SliderUI() {
        @Override
        public void paint(float p, GL2 gl) {
            float W = 1; 
            float H = 1;
            float barSize = W * p;

            
            gl.glColor4f(0f, 0f, 0f, 0.5f);
            Draw.rect(barSize, 0, W-barSize, H, gl);

            
            gl.glColor4f(0.75f * 1f - p, 0.75f * p, 0f, 0.8f);
            Draw.rect(0, 0, barSize, H, gl);
        }

        @Override
        public float p(v2 hitPoint) {
            return pHorizontal(hitPoint);
        }
    };

    /** default impl, with point-center */
    public static final SliderUI KnobVert = new KnobVert() {
        @Override
        public float p(v2 hitPoint) {
            return 1-pTarget(hitPoint.y, 0);
        }
    };


    /** default impl, with point-center */
    public static final SliderUI KnobHoriz = new KnobHoriz() {
        @Override
        public float p(v2 hitPoint) {
            return pTarget(hitPoint.x, 0);
        }
    };

    public static class KnobVert extends Knob {

        @Override
        public void paint(float p, GL2 gl) {


            float y = H * p;

            gl.glColor4f(0f, 0f, 0f, 0.5f);
            Draw.rect(0, 0, W, H, gl);

            gl.glColor4f(1f - p, p, 0f, 0.75f);
            Draw.rect(0, H - y - knob, W, knob, gl);
        }

        @Override
        public float p(v2 hitPoint) {
            return pTarget(1 - hitPoint.y, knob);
        }
    }

    public static class KnobHoriz extends Knob {

        @Override
        public void paint(float p, GL2 gl) {
            float x = W * p;

            gl.glColor4f(0f, 0f, 0f, 0.5f);
            Draw.rect(0, 0, W, H, gl);

            gl.glColor4f(1f - p, p, 0f, 0.75f);
            Draw.rect(x , 0, knob, H, gl);
        }

        @Override
        public float p(v2 hitPoint) {
            return pTarget(hitPoint.x, knob);
        }
    }

    abstract public static class Knob implements SliderUI {

        /** proportion of the total range of which the knob is visible */
        public float knob = 0.05f;

        public float W = 1;
        public float H = 1;

    }
}
