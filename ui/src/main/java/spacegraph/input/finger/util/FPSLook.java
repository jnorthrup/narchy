package spacegraph.input.finger.util;

import com.jogamp.newt.event.MouseEvent;
import jcog.math.v3;
import spacegraph.space3d.SpaceGraph3D;

import static com.jogamp.opengl.math.FloatUtil.sin;
import static java.lang.Math.cos;
import static jcog.math.v3.v;

/**
 * Created by me on 11/20/16.
 */
public class FPSLook extends SpaceMouse {

    private boolean dragging;
    private int prevX;
    private int prevY;
    private float h = (float) Math.PI;
    private float v;

    public FPSLook(SpaceGraph3D g) {
        super(g);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        dragging = false;
    }


    @Override
    public void mouseDragged(MouseEvent e) {
        var bd = e.getButtonsDown();
        if (bd.length > 0 && bd[0] == 3 /* RIGHT */) {
            if (!dragging) {
                prevX = e.getX();
                prevY = e.getY();
                dragging = true;
            }

            var x = e.getX();
            var y = e.getY();

            float dx = x - prevX;
            float dy = y - prevY;

            drag(dx, dy);

            prevX = x;
            prevY = y;

            
        }
    }

    public void drag(float dx, float dy) {
        var angleSpeed = 0.001f;
        h += -dx * angleSpeed;
        v += -dy * angleSpeed;

        var direction = v(
                (float) (cos(this.v) * sin(h)),
                sin(this.v),
                (float) (cos(this.v) * cos(h))
        );


        space.camFwd.set(direction);
    }

}
