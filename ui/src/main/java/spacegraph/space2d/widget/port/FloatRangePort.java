package spacegraph.space2d.widget.port;

import jcog.math.FloatRange;
import spacegraph.space2d.container.EmptySurface;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.port.util.Wire;
import spacegraph.space2d.widget.port.util.Wiring;
import spacegraph.space2d.widget.slider.FloatSlider;

import javax.annotation.Nullable;

public class FloatRangePort extends FloatPort {

    private static final float EPSILON = 0.001f;

    public final FloatRange f;

    public FloatRangePort(float val, float min, float max) {
        this(new FloatRange(val, min, max));
    }

    private FloatRangePort(FloatRange f/*, Consumer<Runnable> updater*/) {
        super();
        this.f = f;

        FloatSlider s = new FloatSlider(f).on((float ff)->FloatRangePort.this.out(ff));

        set(new Gridding(0.25f, new EmptySurface(), s));
    }




    @Override
    public Port on(@Nullable In i) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean recv(Wire from, Float s) {
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
