package spacegraph.space2d.widget.textedit.view;

import com.jogamp.opengl.GL2;


public abstract class TextEditView {

    public final Position position = new Position();
    public final Angle angle = new Angle();
    public final Scale scale = new Scale();
    public final Color color = new Color();

    public void draw(GL2 gl) {
        preDraw(gl);
        gl.glPushMatrix();
        position.updateTranslate(gl);
        angle.updateRotate(gl);
        scale.updateScale(gl);
        color.updateColor(gl);
        innerDraw(gl);
        gl.glPopMatrix();
    }

    protected void preDraw(GL2 gl) {

    }

    protected abstract void innerDraw(GL2 gl);

    boolean isAnimated() {
        return position.isAnimated() || angle.isAnimated() || scale.isAnimated() || color.isAnimated();
    }

}
