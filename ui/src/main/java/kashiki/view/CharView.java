package kashiki.view;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2ES3;
import com.jogamp.opengl.util.texture.Texture;
import kashiki.buffer.BufferChar;

import java.util.function.Consumer;

public class CharView extends Base implements Consumer<BufferChar>, Comparable<CharView> {
  private final TextureProvider textureProvider;
  private final BufferChar bufferChar;

  public CharView(BufferChar bufferChar) {
    this.textureProvider = TextureProvider.getInstance();
    this.bufferChar = bufferChar;
    bufferChar.addListener(this);
    accept(bufferChar);
  }

  public double getWidth() {
    return textureProvider.getWidth(String.valueOf(bufferChar.getChar()));
  }

  @Override
  public void innerDraw(GL2 gl) {
    Texture texture = textureProvider.getTexture(gl, String.valueOf(bufferChar.getChar()));
    texture.enable(gl);
    texture.bind(gl);
    gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
    gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
    gl.glTexParameteri(GL.GL_TEXTURE_2D, GL2ES3.GL_TEXTURE_ALPHA_TYPE, GL.GL_LINEAR);
    gl.glTexParameteri(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);

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

  @Override
  public void accept(BufferChar bc) {
    // TODO
  }

  public BufferChar getBufferChar() {
    return bufferChar;
  }

  @Override
  public int compareTo(CharView o) {
    return bufferChar.compareTo(o.bufferChar);
  }

}
