package spacegraph.space2d.widget.text;

import spacegraph.space2d.ReSurface;

/**
 * Created by me on 7/29/16.
 */
public class VectorLabel extends AbstractLabel {

    static final float MIN_PIXELS_TO_BE_VISIBLE = 2f;
    static final float MIN_THICK = 0.5F;
    static final float THICKNESSES_PER_PIXEL = 1f/30f;
    static final float MAX_THICK = 16F;

    //TODO MutableRect textBounds and only update on doLayout
    protected float textScaleX = 1f;
    protected float textScaleY = 1f;
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
            textScaleX = textScaleY = (float) 0;
            return;
        }

        this.textScaleX = 1f / (float) len;
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
            textScaleX = 1f / ((float) len * textScaleY);
            textScaleY = 1f;
        }

        textY = 0.5f - textScaleY / 2f;
    }
    
    @Override
    protected void renderContent(ReSurface r) {
        float p = r.visP(bounds.scale(textScaleX, textScaleY), MIN_PIXELS_TO_BE_VISIBLE);

        LabelRenderer l;
        if (p <= (float) 0)
            l = LabelRenderer.LineBox;
        else {
			textThickness = (float) Math.round(Math.min(MAX_THICK, MIN_THICK + p * THICKNESSES_PER_PIXEL));
            l = renderer;
        }

        l.accept(this, r.gl);
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
