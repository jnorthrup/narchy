package spacegraph.space2d.widget.text;

import com.jogamp.opengl.GL2;
import jcog.tree.rtree.rect.RectFloat2D;
import spacegraph.input.finger.Finger;
import spacegraph.space2d.Surface;
import spacegraph.space2d.container.AspectAlign;
import spacegraph.space2d.container.EmptySurface;
import spacegraph.util.math.Color4f;
import spacegraph.video.Draw;

/**
 * Created by me on 7/29/16.
 */
public class Label extends AspectAlign {

    private String text = "(null)";

    public float textScaleX = 1f, textScaleY = 1f;
    public final Color4f textColor = new Color4f(1f, 1f, 1f, 1f);
    public float textThickness = 3f;


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
        super(new EmptySurface());
        text(s);
    }

    @Override
    public Surface tryTouch(Finger finger) {
        return null;
    }

    @Override
    protected void doLayout(float tx, float ty, float tw, float th) {
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
            textScaleX = 1f / (len*textScaleY); //(charAspect * len);
            textScaleY = 1f;

        }

        textY = 0.5f - textScaleY / 2f;
    }

    @Override
    protected void paintIt(GL2 gl) {
        Draw.bounds(gl, innerBounds, this::paintUnit);
    }

    void paintUnit(GL2 gl) {


        textColor.apply(gl);
        gl.glLineWidth(textThickness);
        //float dz = 0.1f;
        Draw.text(gl, text(), textScaleX, textScaleY, 0, textY, 0, Draw.TextAlignment.Left);

    }


    public Label text(String newValue) {
        this.text = newValue;
        layout();
        return this;
    }

    public String text() {
        return text;
    }

    @Override
    public String toString() {
        return "Label[" + text() + ']';
    }


//    /**
//     * label which paints in XOR so it contrasts with what is underneath
//     */
//    public static class XorLabel extends Label {
//        @Override
//        public void paintIt(GL2 gl) {
//            //https://www.opengl.org/discussion_boards/showthread.php/179116-drawing-using-xor
//            gl.glEnable(GL_COLOR_LOGIC_OP);
//            gl.glLogicOp(GL_INVERT);
//
//            super.paint(gl);
//            gl.glDisable(GL_COLOR_LOGIC_OP);
//        }
//    }

}
