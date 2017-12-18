package spacegraph.widget.button;

import com.jogamp.opengl.GL2;
import org.jetbrains.annotations.Nullable;
import spacegraph.input.Finger;
import spacegraph.widget.text.Label;
import spacegraph.widget.windo.Widget;

/**
 * Created by me on 11/12/16.
 */
public abstract class AbstractButton extends Widget {

    private boolean pressed;

    @Override
    public void touch(@Nullable Finger finger) {
        super.touch(finger);

        boolean nowPressed = false;
        if (finger != null) {
            if (finger.clickReleased(0)) {
                dz = 0;
                onClick();
            } else if (finger.pressed(0)) {
                dz = 0.5f;
            } else {
                dz = 0;
            }
        } else {
            dz = 0;
        }

    }

    protected abstract void onClick();

//    static void label(GL2 gl, String text) {
//        gl.glColor3f(0.75f, 0.75f, 0.75f);
//        gl.glLineWidth(2);
//        Draw.text(gl, text, 1f/(1+text.length()), 0.5f, 0.5f,  0);
//    }

}
