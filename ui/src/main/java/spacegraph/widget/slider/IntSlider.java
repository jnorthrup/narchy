package spacegraph.widget.slider;

import jcog.math.IntRange;

public class IntSlider extends FloatSlider {

    public IntSlider(int v, int min, int max) {
        super(v, min, max);
    }

    public IntSlider(IntRange x) {
        this(x.intValue(), x.min, x.max);
        input = x::intValue;
        slider.on((s, v) -> x.set(v));
    }

    @Override
    protected FloatSlider.XSlider slider(float v, float min, float max) {
        return new XSlider(v, min, max) {

            @Override
            protected float p(float v) {
                return super.p(Math.round(v));
            }

            @Override
            protected float v(float p) {
                return Math.round(super.v(p));
            }
        };
    }
}
