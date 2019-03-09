package spacegraph.space2d.widget.port;

import jcog.math.FloatRange;
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;
import spacegraph.space2d.SurfaceRender;
import spacegraph.space2d.container.grid.Gridding;
import spacegraph.space2d.widget.port.util.Wiring;
import spacegraph.space2d.widget.slider.FloatSlider;

import javax.annotation.Nullable;

public class FloatRangePort extends FloatPort {

    //private static final float EPSILON = 0.001f;

    public final FloatRange f;

    public FloatRangePort(float val, float min, float max) {
        this(new FloatRange(val, min, max));
    }

    public FloatRangePort(FloatRange f) {
        this(f, "");
    }
    public FloatRangePort(FloatRange f, String label) {
        super();
        this.f = f;

        FloatSlider s = new FloatSlider(f, label).on((FloatProcedure) FloatRangePort.this::out);

        set(new Gridding(s).margin(0.1f));

    }

    @Override
    public Port on(@Nullable In i) {
        throw new UnsupportedOperationException();
    }

    /** HACK */
    @Override public boolean prePaint(SurfaceRender r) {
        if (super.prePaint(r)) {
            if (active())
                GET();
            return true;
        }
        return false;
    }

    public void GET() {
        synchronized(f) {
            out(f.floatValue());
        }
    }

    public void SET(Float s) {
        synchronized(f) {
            f.set(s);
        }
    }

    @Override
    public boolean recv(Wire from, Float s) {
        if (super.recv(from, s)) {
            SET(s);
            return true;
        }
        return false;
    }

    @Override
    protected void onWired(Wiring w) {
        out();
    }
}
