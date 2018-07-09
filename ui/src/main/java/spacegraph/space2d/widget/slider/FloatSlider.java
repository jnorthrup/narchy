package spacegraph.space2d.widget.slider;

import jcog.Texts;
import jcog.Util;
import jcog.math.FloatRange;
import jcog.math.FloatSupplier;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ObjectFloatProcedure;
import org.jetbrains.annotations.Nullable;
import spacegraph.space2d.SurfaceBase;
import spacegraph.space2d.container.Scale;
import spacegraph.space2d.container.Stacking;
import spacegraph.space2d.widget.text.Label;
import spacegraph.space2d.widget.windo.Widget;

/**
 * Created by me on 11/18/16.
 * TODO extend Surface
 */
public class FloatSlider extends Widget {

    private final Label label = new Label();
    FloatSupplier input;
    private String labelText = "";

    protected final SliderModel slider;

    public FloatSlider(float v, float min, float max) {
        this(new FloatRange(v, min, max));
    }




    public FloatSlider(String label, float v, float min, float max) {
        this(v, min, max);
        this.labelText = label;
    }

    public FloatSlider(String label, FloatRange f) {
        this(f);
        this.labelText = label;
    }
    public FloatSlider(String label, FloatSliderModel f) {
        this(f);
        this.labelText = label;
    }

    public FloatSlider(FloatRange f) {
        this(new DefaultFloatSlider(f.get(), f.min, f.max));
        input = f;
    }

    private FloatSlider(FloatSliderModel m) {
        super();

        content(new Stacking(
                new Scale(slider = m, 0.95f),
                new Scale(label, 0.85f)
        ));
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

    public FloatSlider type(SliderModel.SliderUI t) {
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

    public FloatSlider on(ObjectFloatProcedure<SliderModel> c) {
        slider.on(c);
        return this;
    }
    public FloatSlider on(FloatProcedure c) {
        return on((ObjectFloatProcedure<SliderModel>)(SliderModel x, float v)->c.value(v));
    }

    abstract public static class FloatSliderModel extends SliderModel {


        public FloatSliderModel(float v) {
            super(0);
            p(v);
        }

        @Override
        public boolean start(SurfaceBase parent) {
            if (super.start(parent)) {
                update();
                return true;
            }
            return false;
        }

        void update() {
            FloatSlider p = parent(FloatSlider.class);
            if (p!=null) {
                FloatSupplier input = p.input; 
                if (input != null)
                    super.value(input.asFloat());
            }
        }

        public abstract float min();
        public abstract float max();


        @Override
        protected void changed(float p) {
            super.changed(p);

            FloatSlider parent = parent(FloatSlider.class);
            if (parent!=null) {

                parent.updateText();

                
                FloatSupplier input = parent.input; 
                if (input instanceof MutableFloat) {
                    ((MutableFloat) input).set(v(p));
                }
            }

        }

        @Override
        protected float p(float v) {
            float min = min();
            float max = max();
            return (Util.clamp(v, min, max) - min) / (max - min);
        }

        @Override
        protected float v(float p) {
            float min = min();
            float max = max();
            return Util.clamp(p, 0, 1f) * (max - min) + min;
        }

    }

    /** with constant min/max limits */
    public static class DefaultFloatSlider extends FloatSliderModel {

        private final float min;
        private final float max;

        DefaultFloatSlider(float v, float min, float max) {
            super(v);
            this.min = min;
            this.max = max;
        }

        @Override
        public float min() {
            return min;
        }

        @Override
        public float max() {
            return max;
        }
    }
}
