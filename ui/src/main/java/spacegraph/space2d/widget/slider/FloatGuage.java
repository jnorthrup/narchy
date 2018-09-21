package spacegraph.space2d.widget.slider;

import jcog.math.FloatSupplier;
import spacegraph.space2d.SurfaceRender;

public class FloatGuage extends FloatSlider {
    private final FloatSupplier value;

    public FloatGuage(float min, float max, FloatSupplier value) {
        super(value.asFloat(), 0, 1);
        this.value = value;
    }

    @Override
    public boolean prePaint(SurfaceRender r) {
        if (super.prePaint(r)) {
            set(value.asFloat());
            return true;
        }
        return false;
    }

}
