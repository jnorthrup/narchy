package spacegraph.space2d.widget.textedit.view;

import com.jogamp.opengl.GL2;
import spacegraph.space2d.widget.textedit.buffer.CursorPosition;
import spacegraph.video.Draw;

public class CursorView extends TextEditRenderable {

  private final CursorPosition cursor;

  public CursorView(CursorPosition cursor) {
    color.set(1f, 0.5f, 0, 0.5f);
    this.cursor = cursor;
  }

  public static float getWidth() {
    return 1;
  }

  public CursorPosition getCursor() {
    return cursor;
  }

  @Override
  public void innerDraw(GL2 gl) {
    Draw.rect(gl, -0.5f, -0.5f, 1f, 1f, 0);
//    Texture texture = textureProvider.getTexture(gl, "◆");
//    texture.enable(gl);
//    texture.bind(gl);
//    gl.glColor4d(0.4, 0.4, 1, 0.5);
//
//    gl.glRotated((System.currentTimeMillis() / 5f) % 360, 0, 1, 0);
//
//    gl.glBegin(GL2.GL_POLYGON);
//    gl.glTexCoord2f(0, 1);
//    gl.glVertex2d(-0.5, -0.5);
//    gl.glTexCoord2f(0, 0);
//    gl.glVertex2d(-0.5, 0.5);
//    gl.glTexCoord2f(1, 0);
//    gl.glVertex2d(0.5, 0.5);
//    gl.glTexCoord2f(1, 1);
//    gl.glVertex2d(0.5, -0.5);
//    gl.glEnd();
  }
}
