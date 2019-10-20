package spacegraph.space2d.widget.button;

import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.tree.rtree.rect.RectFloat;
import spacegraph.video.Draw;
import spacegraph.video.ImageTexture;

/** hexagonal (circular) PushButton with (optional):
 *     icon, caption, color, and tooltip */
public class HexButton extends PushButton {

    public HexButton(String icon /* TODO abstraction for semantically reference icon from various sources including user sketches */,
                     String tooltip) {
        super(ImageTexture.awesome(icon).view(1.0F));
    }

    @Override
    protected void paintWidget(RectFloat bounds, GL2 gl) {
//        super.paintWidget(bounds, gl);

        //copied from: Widget.java paintWidget
        float dim = 1f - (dz /* + if disabled, dim further */) / 3f;
        float bri = 0.25f * dim;
        color.set( rgb-> Util.or(rgb,bri,pri/ 4.0F), gl);

        float rad =
                Math.min(w(), h())/ 2.0F;
                //bounds.radius()
                //bounds.radius() * 0.5f;

        gl.glPushMatrix();
        gl.glTranslatef(cx(), cy(), (float) 0);
        Draw.poly(6, rad, true, gl);
        gl.glPopMatrix();
    }
}
