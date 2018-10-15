package spacegraph.space2d.widget.text;

import com.jogamp.opengl.GL2;
import spacegraph.input.finger.Finger;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRender;
import spacegraph.space2d.container.EmptyContainer;
import spacegraph.util.math.Color4f;

/**
 * Created by me on 7/29/16.
 */
public class VectorLabel extends EmptyContainer {

    protected volatile String text = "(null)";

    protected final Color4f textColor = new Color4f(1f, 1f, 1f, 1f);

    @Deprecated
    protected float textScaleX = 1f, textScaleY = 1f;
    protected transient float textThickness;
    protected float textY;
    final LabelRenderer renderer = LabelRenderer.
                                        Hershey;
                                        //AWTBitmap;
                                        //NewtGraph;


    public VectorLabel() {
        this("");
    }

    public VectorLabel(String s) {

        text(s);
    }

    @Override
    public Surface finger(Finger finger) {
        return null;
    }

    @Override
    protected void doLayout(int dtMS) {

        int len = text.length();
        if (len == 0) return;

        float charAspect = 1.4f;
        this.textScaleX = 1f / len;
        this.textScaleY = charAspect;

        float tw = w(), th = h();
        float visAspect = th / tw;
//        if (textScaleY / textScaleX <= visAspect) {
//            this.textScaleX = 1f / (charAspect * len);
//            this.textScaleY = textScaleX * charAspect;
//        } else {
            this.textScaleY = textScaleX / visAspect;
//        }

        if (textScaleY > 1f) {
            textScaleX = 1f / (len * textScaleY);
            textScaleY = 1f;
        }

        textY = 0.5f - textScaleY / 2f;
    }

    @Override
    protected void paintIt(GL2 gl) {
        renderer.accept(this, gl);
    }

    @Override
    protected boolean prePaint(SurfaceRender r) {
        float p = r.visPMin(bounds);
        if (p < 7) {
            return false;
        }

        textThickness = Math.min(3, 0.5f + (p / 70f));

        return super.prePaint(r);
    }


    public VectorLabel text(String newValue) {
        if (!newValue.equals(this.text)) {
            this.text = newValue;
            layout();
        }
        return this;
    }

    public final String text() { return text; }


    @Override
    public String toString() {
        return "Label[" + text + ']';
    }


}
