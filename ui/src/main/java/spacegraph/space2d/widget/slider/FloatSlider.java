package spacegraph.space2d.widget.slider;

import jcog.Texts;
import jcog.Util;
import jcog.data.NumberX;
import jcog.data.atomic.AtomicFloat;
import jcog.math.FloatRange;
import jcog.math.FloatSupplier;
import jcog.pri.ScalarValue;
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;
import org.eclipse.collections.api.block.procedure.primitive.ObjectFloatProcedure;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.container.Stacking;
import spacegraph.space2d.container.unit.Scale;
import spacegraph.space2d.widget.Widget;
import spacegraph.space2d.widget.text.AbstractLabel;
import spacegraph.space2d.widget.text.VectorLabel;

/**
 * Created by me on 11/18/16.
 * TODO extend Surface
 */
public class FloatSlider extends Widget {

    private final AbstractLabel label = new VectorLabel();
    FloatSupplier input;
    protected String labelText = "";

    protected final SliderModel slider;

    public FloatSlider(float v, float min, float max) {
        this(new FloatRange(v, min, max));
    }

    public FloatSlider(float v, float min, float max, String label) {
        this(v, min, max);
        this.labelText = label;
    }

    public FloatSlider(FloatRange f, String label) {
        this(f);
        this.labelText = label;
    }
    public FloatSlider(FloatSliderModel f, String label) {
        this(f);
        this.labelText = label;
    }

    public FloatSlider(FloatRange f) {
        this(new DefaultFloatSlider(f.get(), f.min, f.max));
        on(f::set);
        input = f;
    }
    public FloatSlider(AtomicFloat f, float min, float max) {
        this(new DefaultFloatSlider(f.get(), min, max));
        on(f::set);
        input = f;
    }


    private FloatSlider(FloatSliderModel m) {
        super();

        set(new Stacking(
            slider = m,
                new Scale(label, 0.85f)
        ));
    }

    public FloatSlider text(String label) {
        this.labelText = label;
        updateText();
        return this;
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

    @Deprecated volatile float lastValue = Float.NaN;

    @Override
    public boolean preRender(ReSurface r) {

        slider.update();

        float nextValue = get();
        if (lastValue != nextValue) {
            updateText();
            lastValue = nextValue;
        }


        return super.preRender(r);
    }


    public float get() {
        return slider.value();
    }

    public void set(float value) {
        slider.setValue(value);
    }

    public final FloatSlider on(ObjectFloatProcedure<SliderModel> c) {
        if (input instanceof NumberX) {
            ObjectFloatProcedure<SliderModel> c0 = c;
            c = (each,x) -> {
                //chain
                ((NumberX)input).set(x);
                c0.value(each,x);
            };
        }
        slider.on(c);
        return this;
    }


    public final FloatSlider on(FloatProcedure c) {
        return on((ObjectFloatProcedure<SliderModel>)(SliderModel x, float v)->c.value(v));
    }

    abstract public static class FloatSliderModel extends SliderModel {

        @Override
        protected void starting() {
            super.starting();
            update();
        }

        public void update() {
            FloatSlider p = parent(FloatSlider.class);
            if (p!=null) {
                FloatSupplier input = p.input; 
                if (input != null)
                    setValue(input.asFloat());
            }
        }

        public abstract float min();
        public abstract float max();

//
//        @Override
//        protected void _onChanged() {
//
//            FloatSlider parent = parent(FloatSlider.class);
//            if (parent!=null) {
//
//                FloatSupplier input = parent.input;
//                if (input instanceof NumberX) {
//                    ((NumberX) input).set(value());
//                }
//            }
//            super._onChanged();
//        }


        @Override
        protected float p(float v) {
            float min = min(), max = max();
//            min = Math.min(min, max); //HAcK
//            max = Math.max(min, max); //HAcK
            return Util.equals(min, max, ScalarValue.EPSILON) ? 0.5f : (Util.clamp(v, min, max) - min) / (max - min);
        }

        @Override
        protected float v(float p) {
            float min = min();
            float max = max();
            return Util.unitize(p) * (max - min) + min;
        }

    }

    /** with constant min/max limits */
    public static final class DefaultFloatSlider extends FloatSliderModel {

        private final float min;
        private final float max;

        DefaultFloatSlider(float v, float min, float max) {
            super();
            this.min = min;
            this.max = max;
            setValue(v);
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
