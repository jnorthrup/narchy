package spacegraph.space2d.widget.textedit.view;

import com.jogamp.opengl.GL2;
import spacegraph.util.math.Color4f;
import spacegraph.util.math.v3;


public abstract class TextEditRenderable {

    public final v3 position = new v3();
//    public final v3 angle = new v3();
    public final v3 scale = new v3(1,1,1);
    public final Color4f color = new Color4f(1,1,1,1);

    public void draw(GL2 gl) {
        gl.glPushMatrix();
        gl.glTranslatef(position.x, position.y, position.z);
        gl.glScalef(scale.x, scale.y, scale.z);
        gl.glColor4f(color.x, color.y, color.z, color.w);
        innerDraw(gl);
        gl.glPopMatrix();
    }



    protected abstract void innerDraw(GL2 gl);

}
