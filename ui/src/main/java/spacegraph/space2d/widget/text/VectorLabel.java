package spacegraph.space2d.widget.text;

import com.jogamp.opengl.GL2;
import spacegraph.space2d.ReSurface;

/**
 * Created by me on 7/29/16.
 */
public class VectorLabel extends AbstractLabel {

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
    protected void doLayout(float dtS) {

        int len = text.length();
        if (len == 0) return;

        this.textScaleX = 1f / len;
//        this.textScaleY = charAspect;

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
    protected void renderContent(ReSurface r) {
        r.on(this::paintLabel);
        super.renderContent(r);
    }

    protected void paintLabel(GL2 gl, ReSurface r) {
        renderer.accept(this, gl);
    }

    @Override
    protected final boolean preRender(ReSurface r) {
        float p = r.visPMin(bounds);
        if (p < 7) {
            return false;
        }

        textThickness = Math.min(3, 0.5f + (p / 70f));

        return super.preRender(r);
    }


}
