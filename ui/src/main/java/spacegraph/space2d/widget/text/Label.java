package spacegraph.space2d.widget.text;

import com.jogamp.opengl.GL2;
import jcog.tree.rtree.rect.RectFloat2D;
import spacegraph.input.finger.Finger;
import spacegraph.space2d.Surface;
import spacegraph.space2d.SurfaceRender;
import spacegraph.space2d.container.EmptyContainer;
import spacegraph.util.math.Color4f;
import spacegraph.video.Draw;

/**
 * Created by me on 7/29/16.
 */
public class Label extends EmptyContainer {

    private String text = "(null)";

    private float textScaleX = 1f;
    private float textScaleY = 1f;
    private final Color4f textColor = new Color4f(1f, 1f, 1f, 1f);

    private transient float textThickness;


    /**
     * inner rectangle in which the text is actually rendered, after calculating
     * aspect ratio, scale and alignment
     */
    private RectFloat2D innerBounds = RectFloat2D.Zero;
    private float textY;

    public Label() {
        this("");
    }

    public Label(String s) {
        
        text(s);
    }

    @Override
    public Surface tryTouch(Finger finger) {
        return null;
    }

    @Override
    protected void doLayout(int dtMS) {
        float tx = x(), ty = y();
        float tw = w();
        float th = h();
        innerBounds = RectFloat2D.XYXY(tx, ty, tx + tw, ty + th);

        int len = text().length();
        if (len == 0) return;

        float charAspect = 1.4f;
        this.textScaleX = 1f / len;
        this.textScaleY = 1 * charAspect;

        float visAspect = th / tw;
        if (textScaleY / textScaleX <= visAspect) {
            this.textScaleX = 1f / (charAspect * len);
            this.textScaleY = textScaleX * charAspect;
        } else {
            this.textScaleY = textScaleX / visAspect;
        }

        if (textScaleY > 1f) {
            textScaleX = 1f / (len*textScaleY); 
            textScaleY = 1f;

        }

        textY = 0.5f - textScaleY / 2f;
    }

    @Override
    protected void paintIt(GL2 gl) {
        Draw.bounds(innerBounds, gl, this::paintUnit);
    }

    @Override
    protected boolean prePaint(SurfaceRender r) {
        float p = r.visP(bounds).minDimension();
        if (p < 7) {
            return false;
        }

        textThickness = Math.min(3, 0.5f + (p/70f));

        return super.prePaint(r);
    }

    private void paintUnit(GL2 gl) {


        textColor.apply(gl);
        gl.glLineWidth(textThickness);
        
        Draw.text(gl, text(), textScaleX, textScaleY, 0, textY, 0, Draw.TextAlignment.Left);

    }


    public Label text(String newValue) {
        this.text = newValue;
        layout();
        return this;
    }

    private String text() {
        return text;
    }

    @Override
    public String toString() {
        return "Label[" + text() + ']';
    }

















}
