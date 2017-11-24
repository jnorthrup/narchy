package spacegraph.widget.slider;

import com.jogamp.opengl.GL2;
import jcog.Texts;
import jcog.Util;
import jcog.math.FloatParam;
import jcog.math.FloatSupplier;
import org.jetbrains.annotations.Nullable;
import spacegraph.Surface;
import spacegraph.widget.text.Label;

/**
 * Created by me on 11/18/16.
 */
public class FloatSlider extends BaseSlider {

    private final float max;
    private final float min;
    final Label label = new Label();
    public FloatSupplier input;
    private String labelText = "";

    public FloatSlider(float v, float min, float max) {
        super((v - min) / (max - min));

        set(/*new AspectAlign*/(label));
        updateLabel();

        this.min = min;
        this.max = max;

    }

    public FloatSlider(String label, float v, float min, float max) {
        this(v, min, max);
        this.labelText = label;
        //this.label.set(label);
    }

    public FloatSlider(FloatParam f) {
        this(f.floatValue(), f.min, f.max);
        input = f;
        on((s, v) -> f.set(v));
    }

    public FloatSlider label(String label) {
        this.labelText = label;
        updateLabel();
        return this;
    }

    @Override
    public void start(@Nullable Surface parent) {
        super.start(parent);
        updateLabel();
    }

    @Override
    protected void changed(float p) {
        super.changed(p);
        updateLabel();
    }

    private void updateLabel() {
        this.label.set(labelText());
    }

    @Override
    protected void paintComponent(GL2 gl) {
        if (input != null)
            value(input.asFloat());

        super.paintComponent(gl);
    }

    public String labelText() {
        return this.labelText + Texts.n2(value());
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
