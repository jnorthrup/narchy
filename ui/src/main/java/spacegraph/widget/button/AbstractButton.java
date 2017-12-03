package spacegraph.widget.button;

import com.jogamp.opengl.GL2;
import jcog.tree.rtree.rect.RectFloat2D;
import org.jetbrains.annotations.Nullable;
import spacegraph.input.Finger;
import spacegraph.render.Draw;
import spacegraph.widget.text.Label;
import spacegraph.widget.windo.Widget;

/**
 * Created by me on 11/12/16.
 */
public abstract class AbstractButton extends Widget {
    float pushed; //how depresssed the button is, from 0= not touched, to 1=push through the screen
    private boolean pressed;
    private final boolean enabled = true;

    public static void text(GL2 gl, Label text, float x, float y, float w, float h) {
        gl.glPushMatrix();
        gl.glTranslatef(x , y , 0);
        gl.glScalef(w, h, 1);
        text.paint(gl);
        gl.glPopMatrix();
    }


    @Override
    protected void paintComponent(GL2 gl) {
        RectFloat2D b = bounds;
        paintComponent(gl, b.min.x, b.min.y, b.max.x - b.min.x, b.max.y - b.min.y);
    }

    private void paintComponent(GL2 gl, float x, float y, float w, float h) {
        paintBack(gl, x, y, w, h);
        paintContent(gl, x, y, w, h);
    }

    /**
     * TODO make abstract
     */
    protected void paintContent(GL2 gl, float x, float y, float w, float h) {
    }

    protected void paintBackColor(GL2 gl) {
        float dim = 1f - (pushed /* + if disabled, dim further */) * 3f;
        gl.glColor3f(0.25f * dim, 0.25f * dim, 0.25f * dim);
    }

    protected void paintBack(GL2 gl, float x, float y, float w, float h) {
        paintBackColor(gl);
        float p = pushed / 2f;
        Draw.rect(gl, x + p, y + p, w - 2 * p, h - 2 * p);
    }

    @Override
    public void touch(@Nullable Finger finger) {
        super.touch(finger);

        boolean nowPressed = false;
        if (finger != null) {
            if (finger.clickReleased(0)) {
                pushed = 0;
                onClick();
            } else if (finger.pressed(0)) {
                pushed = 0.05f;
            } else {
                pushed = 0;
            }
        } else {
            pushed = 0;
        }

    }

    protected abstract void onClick();

//    static void label(GL2 gl, String text) {
//        gl.glColor3f(0.75f, 0.75f, 0.75f);
//        gl.glLineWidth(2);
//        Draw.text(gl, text, 1f/(1+text.length()), 0.5f, 0.5f,  0);
//    }

}
