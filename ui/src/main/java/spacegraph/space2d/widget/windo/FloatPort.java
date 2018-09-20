package spacegraph.space2d.widget.windo;

import jcog.Util;
import jcog.math.FloatRange;
import spacegraph.input.finger.Wiring;
import spacegraph.space2d.container.EmptySurface;
import spacegraph.space2d.container.Gridding;
import spacegraph.space2d.widget.slider.FloatSlider;

import javax.annotation.Nullable;

public class FloatPort extends /*Source*/Port {

    private static final float EPSILON = 0.001f;

    private float curValue = Float.NaN;
    public final FloatRange f;

    public FloatPort(float val, float min, float max) {
        this(new FloatRange(val, min, max));
    }

    private FloatPort(FloatRange f/*, Consumer<Runnable> updater*/) {
        this.f = f;

        FloatSlider s = new FloatSlider(f);
        content(new Gridding(0.25f, new EmptySurface(), s));
    }

    @Override
    public void prePaint(int dtMS) {
        

        float nextValue = f.get();
        if (!Util.equals(nextValue, curValue, EPSILON)) {
            curValue = nextValue;
            out();
        }

        super.prePaint(dtMS);
    }

    private void out() {
        out(curValue);
    }

    @Override
    public Port on(@Nullable In i) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean in(Wire from, Object s) {
        return false;
    }

    @Override
    public boolean onWireIn(@Nullable Wiring w, boolean preOrPost) {
        return false; 
    }

    @Override
    protected void onWired(Wiring w) {
        out();
    }
}
