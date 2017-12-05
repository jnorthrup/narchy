package spacegraph.widget.text;

import com.jogamp.opengl.GL2;
import spacegraph.Surface;
import spacegraph.math.Color4f;
import spacegraph.render.Draw;

import static com.jogamp.opengl.GL.GL_COLOR_LOGIC_OP;
import static com.jogamp.opengl.GL.GL_INVERT;

/**
 * Created by me on 7/29/16.
 */
public class Label extends Surface {

    private String value = "(null)";

    public float fontScaleX = 1f, fontScaleY = 1f;
    public final Color4f color = new Color4f(1f, 1f, 1f, 1f);
    public float lineWidth = 3f;

    public Label() {
        this("");
    }

    public Label(String s) {
        super();
        set(s);
    }



    @Override
    public void paint(GL2 gl) {
        int len = value.length();
        if (len == 0) return;

        float charAspect = 1.4f;
        this.fontScaleX = 1f / len;
        this.fontScaleY = 1 * charAspect;

        float visAspect = h() / w();
        if (fontScaleY / fontScaleX <= visAspect) {
            this.fontScaleX = 1f / (charAspect * len);
            this.fontScaleY = fontScaleX * charAspect;
        } else {

            this.fontScaleY = fontScaleX / visAspect;
        }

        if (fontScaleY > 1f) {
            fontScaleY = 1f;
            fontScaleX = 1f / (charAspect * len);
        }

        Draw.bounds(gl, this, this::paintUnit);
    }

    void paintUnit(GL2 gl) {


        color.apply(gl);
        gl.glLineWidth(lineWidth);
        //float dz = 0.1f;
        Draw.text(gl, value(), fontScaleX, fontScaleY, 0, 0.5f - fontScaleY / 2f, 0, Draw.TextAlignment.Left);

    }


    public void set(String newValue) {

        this.value = newValue;

    }

    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return "Label[" + value + ']';
    }


    /**
     * label which paints in XOR so it contrasts with what is underneath
     */
    public static class XorLabel extends Label {
        @Override
        public void paint(GL2 gl) {
            //https://www.opengl.org/discussion_boards/showthread.php/179116-drawing-using-xor
            gl.glEnable(GL_COLOR_LOGIC_OP);
            gl.glLogicOp(GL_INVERT);

            super.paint(gl);
            gl.glDisable(GL_COLOR_LOGIC_OP);
        }
    }

}
