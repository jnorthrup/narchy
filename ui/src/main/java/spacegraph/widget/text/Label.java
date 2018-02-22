package spacegraph.widget.text;

import com.jogamp.opengl.GL2;
import jcog.tree.rtree.rect.RectFloat2D;
import spacegraph.Surface;
import spacegraph.input.Finger;
import spacegraph.layout.AspectAlign;
import spacegraph.layout.EmptySurface;
import spacegraph.math.Color4f;
import spacegraph.render.Draw;

/**
 * Created by me on 7/29/16.
 */
public class Label extends AspectAlign {

    private String text = "(null)";

    public float textScaleX = 1f, textScaleY = 1f;
    public final Color4f textColor = new Color4f(1f, 1f, 1f, 1f);
    public float textThickness = 3f;


    /** inner rectangle in which the text is actually rendered, after calculating
     * aspect ratio, scale and alignment */
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
    public Surface onTouch(Finger finger, short[] buttons) {
        return null;
    }

    @Override
    protected void doLayout(float tx, float ty, float tw, float th) {
        innerBounds = new RectFloat2D(tx, ty, tx+tw, ty+th);

        int len = text().length();
        if (len == 0) return;

        float charAspect = 1.4f;
        this.textScaleX = len/charAspect;
        this.textScaleY = 1;
        float tr = textScaleY / textScaleX;
        if (tr <= th/tw) {
            this.textScaleX = 1;
            this.textScaleY = 1*tr;
            textScaleX/=len;
            assert(textScaleY < 1);
        } else {
            if (tr >= 1) {
                this.textScaleY = 1;
                this.textScaleX = 1 / tr;
            } else {
                this.textScaleY = 1;
                this.textScaleX = 1 * tr;
            }
            assert(textScaleX < 1);
            //this.textScaleY = textScaleX / visAspect;
//            textScaleY = 1;
//            textScaleX = textScaleY * (charAspect)/len;
        }

//        if (textScaleY > 1f) {
//            textScaleY = 1f;
//            textScaleX = 1f / (charAspect * len);
//        }

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
