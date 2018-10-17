package kashiki.view;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.texture.Texture;
import kashiki.buffer.CursorPosition;

public class CaretView extends Base {
  private final TextureProvider textureProvider;
  private final SmoothValue col = getPosition().getX();
  private final SmoothValue row = getPosition().getY();
  private final CursorPosition cursor;

  public CaretView(CursorPosition cursor) {
    this.cursor = cursor;
    this.textureProvider = TextureProvider.getInstance();
  }

  public void updatePosition(double x, double y) {
    this.row.setValue(-y);
    this.col.setValue(x);
  }

  public double getWidth() {
    return textureProvider.getWidth("◆");
  }

  public CursorPosition getCursor() {
    return cursor;
  }

  @Override
  public void innerDraw(GL2 gl) {
    Texture texture = textureProvider.getTexture(gl, "◆");
    texture.enable(gl);
    texture.bind(gl);
    gl.glColor4d(0.4, 0.4, 1, 0.5);

    gl.glRotated((System.currentTimeMillis() / 5f) % 360, 0, 1, 0);

    gl.glBegin(GL2.GL_POLYGON);
    gl.glTexCoord2f(0, 1);
    gl.glVertex2d(-0.5, -0.5);
    gl.glTexCoord2f(0, 0);
    gl.glVertex2d(-0.5, 0.5);
    gl.glTexCoord2f(1, 0);
    gl.glVertex2d(0.5, 0.5);
    gl.glTexCoord2f(1, 1);
    gl.glVertex2d(0.5, -0.5);
    gl.glEnd();
  }
}
