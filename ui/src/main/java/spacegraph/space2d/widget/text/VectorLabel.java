package spacegraph.space2d.widget.text;

import jcog.Util;
import spacegraph.space2d.ReSurface;

/**
 * Created by me on 7/29/16.
 */
public class VectorLabel extends AbstractLabel {

    static final int MIN_PIXELS_TO_BE_VISIBLE = 3;
    static final float MIN_THICK = 0.5F, THICKNESSES_PER_PIXEL = 1f/100f, MAX_THICK = 5F;

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
        if (len == 0) {
            textScaleX = textScaleY = 0;
            return;
        }

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
        float p = r.visP(bounds.scale(textScaleX, textScaleY), MIN_PIXELS_TO_BE_VISIBLE);
        LabelRenderer lr;
        if (p <= 0)
            lr = LabelRenderer.LineBox;
        else {
            textThickness = Util.min(MAX_THICK, MIN_THICK + p * THICKNESSES_PER_PIXEL);
            lr = renderer;
        }

        lr.accept(this, r.gl);
        //super.renderContent(r);
    }

//    @Override
//    protected final boolean canRender(ReSurface r) {
//        float p = r.visP(bounds, 7);
//        if (p < 7)
//            return false;
//
//        textThickness = Math.min(3, 0.5f + (p / 70f));
//
//        //return super.preRender(r);
//        return true;
//    }


}
