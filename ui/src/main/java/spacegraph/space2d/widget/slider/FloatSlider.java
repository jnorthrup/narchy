package spacegraph.space2d.widget.slider;

import com.jogamp.opengl.GL2;
import jcog.Texts;
import jcog.Util;
import jcog.math.FloatRange;
import jcog.math.FloatSupplier;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.eclipse.collections.api.block.procedure.primitive.FloatObjectProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ObjectFloatProcedure;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.SurfaceBase;
import spacegraph.space2d.container.AspectAlign;
import spacegraph.space2d.container.Scale;
import spacegraph.space2d.container.Stacking;
import spacegraph.space2d.widget.text.Label;
import spacegraph.space2d.widget.windo.Widget;

/**
 * Created by me on 11/18/16.
 */
public class FloatSlider extends Widget {

    final Label label = new Label();
    public FloatSupplier input;
    private String labelText = "";

    protected final BaseSlider slider;

    public FloatSlider(float v, float min, float max) {
        this(new FloatRange(v, min, max));
    }

    @Deprecated protected FloatSlider.XSlider slider(float v, float min, float max) {
        return new XSlider(v, min, max);
    }

    protected FloatSlider.XSlider slider(FloatRange f) {
        return slider(f.get(), f.min, f.max);
    }


    public FloatSlider(String label, float v, float min, float max) {
        this(v, min, max);
        this.labelText = label;
    }

    public FloatSlider(String label, FloatRange f) {
        this(f);
        this.labelText = label;
    }

    public FloatSlider(FloatRange f) {
        super();

        content(new Stacking(
                new Scale(slider = slider(f), 0.95f),
                label.scale(0.85f).align(AspectAlign.Align.Center)
        ));
        updateText();

        input = f;
    }


    public FloatSlider text(String label) {
        this.labelText = label;
        updateText();
        return this;
    }

    @Override
    public boolean start(@Nullable SurfaceBase parent) {
        if (super.start(parent)) {
            updateText();
            return true;
        }
        return false;
    }

    private void updateText() {
        this.label.text(text());
    }

    public String text() {
        return this.labelText + Texts.n2(slider.value());
    }

    public FloatSlider type(FloatObjectProcedure<GL2> t) {
        slider.type(t);
        return this;
    }

    public double value() {
        return slider.value();
    }
    public void value(float v) {
        slider.value(v);
    }

    public void valueRelative(float p) {
        this.slider.changed(p);
    }

    public FloatSlider on(ObjectFloatProcedure<BaseSlider> c) {
        slider.on(c);
        return this;
    }

    protected class XSlider extends BaseSlider {

        private final float min;
        private final float max;

        public XSlider(float v, float min, float max) {
            super((v - min) / (max - min));
            this.min = min;
            this.max = max;
        }


        @Override
        protected void paint(GL2 gl, int dtMS) {

            if (input != null)
                super.value(input.asFloat());

            super.paint(gl, dtMS);
        }

        @Override
        protected void changed(float p) {
            super.changed(p);
            updateText();

            //TODO other setter models, ex: AtomicDouble etc
            if (input instanceof MutableFloat) {
                ((MutableFloat) input).set(v(p));
            }

        }

        @Override
        protected float p(float v) {
            return (Util.clamp(v, min, max) - min) / (max - min);
        }

        @Override
        protected float v(float p) {
            return Util.clamp(p, 0, 1f) * (max - min) + min;
        }

    }

}
