package spacegraph.space2d.widget.port;

import jcog.math.FloatRange;
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;
import spacegraph.space2d.ReSurface;
import spacegraph.space2d.widget.port.util.Wiring;
import spacegraph.space2d.widget.slider.FloatSlider;
import spacegraph.space2d.widget.text.LabeledPane;

public class FloatRangePort extends FloatPort {

    //private static final float EPSILON = 0.001f;

    public final FloatRange f;
    private final boolean autoUpdate = true; //TODO configurable rate
    public final FloatSlider slider;

    public FloatRangePort(float val, float min, float max) {
        this(new FloatRange(val, min, max));
    }

    public FloatRangePort(FloatRange f) {
        this(f, "");
    }
    public FloatRangePort(FloatRange f, String label) {
        super();
        this.f = f;

        FloatSlider s = (slider = new FloatSlider(f)).on((FloatProcedure) FloatRangePort.this::out);

        set(LabeledPane.the(label, s));

        on(this::SET);
    }


    @Override
    protected void renderContent(ReSurface r) {
        if (autoUpdate) {
            if (active())
                LOAD();
        }

        super.renderContent(r);
    }



    private void LOAD() {
        //synchronized (f) {
            out(f.floatValue());
        //}
    }

    public void SET(Float s) {
        //synchronized(f) {
            f.set(s);
        //}
    }

    @Override
    protected void onWired(Wiring w) {
        out();
    }
}
