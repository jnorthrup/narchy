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
        Draw.bounds(bounds, gl, g-> ui.draw(this.p, g));
    }

    private SliderUI ui = SolidLeft;

    public interface SliderUI {
        void draw(float p, GL2 gl);

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
    public Surface tryTouch(Finger finger) {


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
        float x = hitPoint.x;
        if (x <= margin)
            return 0;
        else if (x >= (1f-margin))
            return 1f;
        else
            return x;
    }
    private static float pVertical(v2 hitPoint) {
        float y = hitPoint.y;
        if (y <= margin)
            return 0;
        else if (y >= (1f-margin))
            return 1f;
        else
            return y;
    }

    private static final SliderUI SolidLeft = new SliderUI() {
        @Override
        public void draw(float p, GL2 gl) {
            float W = 1; 
            float H = 1;
            float barSize = W * p;

            
            gl.glColor4f(0f, 0f, 0f, 0.5f);
            Draw.rect(gl, barSize, 0, W-barSize, H);

            
            gl.glColor4f(0.75f * 1f - p, 0.75f * p, 0f, 0.8f);
            Draw.rect(gl, 0, 0, barSize, H);
        }

        @Override
        public float p(v2 hitPoint) {
            return pHorizontal(hitPoint);
        }
    };


    public static final SliderUI KnobHoriz = new SliderUI() {
        @Override
        public void draw(float p, GL2 gl) {
            float knobWidth = 0.05f;
            float W = 1;
            float H = 1;
            float x = W * p;

            gl.glColor4f(0f, 0f, 0f, 0.5f);
            Draw.rect(gl, 0, 0, W, H);

            gl.glColor4f(1f - p, p, 0f, 0.75f);
            Draw.rect(gl, x-knobWidth/2f, 0, knobWidth, H);
        }

        @Override
        public float p(v2 hitPoint) {
            return pHorizontal(hitPoint);
        }
    };

    public static final SliderUI KnobVert = new SliderUI() {
        @Override
        public void draw(float p, GL2 gl) {
            float knobWidth = 0.05f;
            float W = 1;
            float H = 1;
            float x = W * p;

            gl.glColor4f(0f, 0f, 0f, 0.5f);
            Draw.rect(gl, 0, 0, W, H);

            gl.glColor4f(1f - p, p, 0f, 0.75f);
            Draw.rect(gl, 0, x-knobWidth/2f, W, knobWidth);
        }

        @Override
        public float p(v2 hitPoint) {
            return pVertical(hitPoint);
        }
    };















































































































































































































































}
