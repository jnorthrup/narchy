package spacegraph.space2d.widget.button;

import com.jogamp.opengl.GL2;
import jcog.tree.rtree.rect.RectFloat;
import spacegraph.video.Draw;
import spacegraph.video.ImageTexture;

/** hexagonal (circular) PushButton with (optional):
 *     icon, caption, color, and tooltip */
public class HexButton extends PushButton {

    public HexButton(String icon /* TODO abstraction for semantically reference icon from various sources including user sketches */,
                     String tooltip) {
        super(ImageTexture.awesome(icon).view(1));
    }

    @Override
    protected void paintWidget(RectFloat bounds, GL2 gl) {
//        super.paintWidget(bounds, gl);
        gl.glColor3f(0, 0.75f, 0);
        float rad =
                Math.min(w(), h())/2;
                //bounds.radius()
                //bounds.radius() * 0.5f;

        gl.glPushMatrix();
        gl.glTranslatef(cx(), cy(), 0);
        Draw.poly(6, rad, true, gl);
        gl.glPopMatrix();
    }
}
