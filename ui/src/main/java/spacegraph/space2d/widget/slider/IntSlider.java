package spacegraph.space2d.widget.slider;

import jcog.math.IntRange;

public class IntSlider extends FloatSlider {

    private IntSlider(int v, int min, int max) {
        super(v, min, max);
    }

    protected IntSlider(IntRange x) {
        this(x.intValue(), x.min, x.max);
        input = x::floatValue; //x::floatValue;
        slider.on((SliderModel s, float v) -> x.set(v));
    }

//    protected FloatSliderModel slider(float v, float min, float max) {
//        return new MyDefaultFloatSlider(v, min, max);
//    }
//
//    private static final class MyDefaultFloatSlider extends DefaultFloatSlider {
//
//        public MyDefaultFloatSlider(float v, float min, float max) {
//            super(v, min, max);
//        }
//
//        @Override
//        protected float p(float v) {
//            return super.p(Math.round(v));
//        }
//
//        @Override
//        protected float v(float p) {
//            return Math.round(super.v(p));
//        }
//    }
}
